package com.thordickinson.dumbcrawler.services;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.thordickinson.dumbcrawler.util.SQLiteConnection;
import jakarta.annotation.PreDestroy;

import com.jsoniter.any.Any;

import static com.thordickinson.dumbcrawler.util.JsonUtil.*;

import com.thordickinson.dumbcrawler.util.JDBCUtil;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.expression.URLExpressionEvaluator;

public class URLStore {

    public static class Status {
        public static int QUEUED = 0;
        public static int PROCESSING = 1;
        public static int PROCESSED = 2;
        public static int FAILED = 3;
    }

    private static final Logger logger = LoggerFactory.getLogger(URLStore.class);
    private final SQLiteConnection connection;
    private final CrawlingSessionContext context;
    private int queued = 0;
    private int processed = 0;
    private int failed = 0;
    private final List<PriorityFilter> priorityFilters;
    private Optional<String> urlFilter = Optional.empty();
    private final URLExpressionEvaluator expressionEvaluator = new URLExpressionEvaluator();

    public URLStore(CrawlingSessionContext context) {
        this.context = context;
        connection = new SQLiteConnection(context.getExecutionDir());
        priorityFilters = context.getConfig("crawler.priorities").map(Any::asList)
                .map(l -> l.stream().map(this::makeFilter).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
        urlFilter = context.getConfig("crawler.urlFilter").map(Any::toString);
    }

    private PriorityFilter makeFilter(Any value) {
        var filter = get(value, "urlFilter").map(Any::toString).orElse(null);
        var priority = get(value, "priority").map(Any::toInt).orElse(0);
        return new PriorityFilter(filter, priority);
    }


    private void loadCounters() {
        var sql = "select status, count(*) as count from links group by status";
        var counters = connection.query(sql);
        for (var counter : counters) {
            var status = (int) counter.get(0);
            switch (status) {
                case 0 -> queued = (int) counter.get(1);
                case 2 -> processed = (int) counter.get(1);
                case 3 -> failed = (int) counter.get(1);
            }
        }
    }

    private void updateOrphans() {
        var orphans = connection.update("UPDATE links SET status = 0 WHERE status = 1");
        if (orphans > 0)
            logger.warn("{} orphan urls were updated", orphans);
    }

    private void markForRefetch() {
        boolean refetch = false; // TODO: Make this configurable with a argument
        if (refetch) {
            logger.warn("Marking all links for refetch");
            connection.update("UPDATE links SET status = 0"); // This will force to revisit all the pages
            logger.warn("fetch update completed");
        }
    }

    private void initialize() {
        String check = "SELECT name FROM sqlite_master WHERE type='table' AND name='links'";
        var checkResult = connection.singleResult(String.class, check);
        if (checkResult.isPresent()) {
            logger.info("Schema is initialized");
            updateOrphans();
            loadCounters();
            markForRefetch();
            return;
        }

        logger.info("Initializing schema");
        String table = "CREATE TABLE links (hash TEXT PRIMARY KEY, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, url TEXT, " +
                "status INTEGER DEFAULT 0, priority INTEGER DEFAULT 0) WITHOUT ROWID";
        connection.update(table);
        connection.update("CREATE INDEX url_index ON links(url)");
        connection.update("CREATE INDEX status_index ON links(status)");
        connection.update("CREATE INDEX priority_index ON links(priority)");
        logger.info("Schema creation complete");
    }


    private boolean shouldAddLink(String uri) {
        if (!uri.startsWith("http")) {
            logger.trace("Url ignored, protocol is not http or https {}", uri);
            return false;
        }
        var result = urlFilter.map(r -> expressionEvaluator.evaluateBoolean(r, uri)).orElse(true);
        logger.trace("URL filtered [{}]: {}", result, uri);
        return result;
    }

    private int getPriority(String url) {
        var priority = priorityFilters.stream()
                .filter(f -> expressionEvaluator.evaluateBoolean(f.filter(), url))
                .findFirst().map(PriorityFilter::value).orElse(0);
        logger.trace("URL priority: [{}]: {}", priority, url);
        return priority;
    }

    private String hashUrl(String url) {
        return DigestUtils.md5Hex(url);
    }

    private void addUrlsInternal(Collection<String> urls, boolean filter) {
        if (urls.isEmpty())
            return;

        var filtered = filter ? urls.stream().filter(this::shouldAddLink)
                .toList() : new ArrayList<>(urls);

        context.increaseCounter("ingnoredUrls", urls.size() - filtered.size());

        if (filtered.isEmpty())
            return;

        var hashedUrls = new HashMap<String, String>();
        for (var url : filtered) {
            hashedUrls.put(hashUrl(url), url);
        }

        var hashList = new ArrayList<String>(hashedUrls.keySet());
        var placeholders = JDBCUtil.generateParams(hashList.size());
        var exists = "SELECT hash FROM links WHERE hash in %s".formatted(placeholders);
        var existent = connection.query(exists, hashList)
                .stream().map(r -> r.get(0)).map(String::valueOf)
                .collect(Collectors.toSet());

        var toInsert = new HashMap<>(hashedUrls);
        for (var e : existent) {
            toInsert.remove(e);
        }
        if (toInsert.isEmpty())
            return;

        var prioritized = toInsert.entrySet().stream().map(e -> new NewUrl(e.getValue(), e.getKey(), getPriority(e.getValue()))).toList();

        var sqlFormat = JDBCUtil.generateParams(3, prioritized.size());
        List<Object> params = prioritized.stream().flatMap(e -> Stream.of(e.hash(), e.url(), e.priority()))
                .collect(Collectors.toList());
        var insert = "INSERT INTO links (hash, url, priority) VALUES %s".formatted(sqlFormat);
        connection.update(insert, params);

        context.increaseCounter("discoveredUrls", prioritized.size());
        queued += prioritized.size();
        logger.debug("New urls added: {}", prioritized.size());
    }

    public Map<String, Integer> getStatus() {
        return Map.of("QUEUED", queued, "PROCESSED", processed, "FAILED", failed);
    }

    public void addUrls(Collection<CrawlingTask> tasks) {
        var deduplicated = new HashSet<>(tasks);
        deduplicated.forEach(System.out::println);
    }

    private static String formatUrls(Set<String> urls) {
        return urls.stream().map("'%s'"::formatted).collect(Collectors.joining(", "));
    }

    public void setVisited(Set<String> urls) {
        updateStatus(urls, Status.PROCESSED);
        var size = urls.size();
        processed += size;
        queued -= size;
    }

    public void setFailed(Set<String> urls) {
        updateStatus(urls, Status.FAILED);
        var size = urls.size();
        failed += size;
        queued -= size;
    }

    public void updateStatus(Set<String> urls, int status) {
        String strUrls = formatUrls(urls);
        String sql = "UPDATE links SET status = %d WHERE url IN (%s)".formatted(status, strUrls);
        var updated = connection.update(sql);
        context.increaseCounter("visitedUrls", urls.size());
        logger.debug("Marking {} urls as visited", updated);
    }

    public Set<String> getUnvisited(int count) {
        var sql = "SELECT url FROM links WHERE status = 0 ORDER BY priority DESC LIMIT ?";
        var result = connection.query(sql, List.of(count));
        var unvisited = result.stream().map(r -> r.get(0)).map(String::valueOf).collect(Collectors.toSet());
        var urlList = unvisited.stream().map("'%s'"::formatted).collect(Collectors.joining(", "));
        var update = "UPDATE links SET status = 1 WHERE url IN (%s)".formatted(urlList);
        var updated = connection.update(update);
        logger.debug("Returned {} urls to process", updated);
        return unvisited;
    }

    @PreDestroy
    void destroy() {
        if (connection != null) {
            try {
                logger.debug("Closing connection");
                connection.close();
            } catch (Exception ex) {
                logger.warn("Error closing connection", ex);
            }
        }
    }
}

record PriorityFilter(String filter, Integer value) {
}

record NewUrl(String url, String hash, int priority) {
}

