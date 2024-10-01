package com.thordickinson.dumbcrawler.services;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.thordickinson.dumbcrawler.util.CollectionUtils;
import com.thordickinson.dumbcrawler.util.SQLiteConnection;
import jakarta.annotation.PreDestroy;

import com.thordickinson.dumbcrawler.util.JDBCUtil;

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
    private Map<String,Integer> priorities = Collections.emptyMap();
    private final URLExpressionEvaluator expressionEvaluator = new URLExpressionEvaluator();

    public URLStore(CrawlingSessionContext context) {
        this.context = context;
        connection = new SQLiteConnection(context.getSessionDir());
        configure();
        initialize();
    }

    private void configure(){
        var priorities = context.getConfig("priorities");
        this.priorities = priorities.map(p -> p.asMap()).map(m -> CollectionUtils.mapValues(m, x -> x.toInt())).
                orElse(Collections.emptyMap());
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
        if (!refetch) {
            return;
        }
        logger.warn("Marking all links for refetch");
        connection.update("UPDATE links SET status = 0"); // This will force to revisit all the pages
        logger.warn("fetch update completed");
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


    private boolean shouldAddLink(CrawlingTask task) {
        var uri = task.url();
        if (!uri.startsWith("http")) {
            logger.trace("Url ignored, protocol is not http or https {}", uri);
            return false;
        }
        var result = true;
        logger.trace("URL filtered [{}]: {}", result, uri);
        return result;
    }

    private void addUrlsInternal(Collection<CrawlingTask> urls) {
        if (urls.isEmpty())
            return;

        logger.debug("Attempting to add {} new  urls", urls.size());
        var filtered = urls.stream().filter(this::shouldAddLink).toList();
        context.increaseCounter("ingnoredUrls", urls.size() - filtered.size());

        if (filtered.isEmpty()) {
            logger.debug("No new urls to append");
            return;
        }

        logger.debug("{} new urls to append", urls.size());
        var hashList = urls.stream().map(url -> url.id()).toList();
        var placeholders = JDBCUtil.generateParams(hashList.size());
        var exists = "SELECT hash FROM links WHERE hash in %s".formatted(placeholders);
        var existent = connection.query(exists, hashList)
                .stream().map(r -> r.get(0)).map(String::valueOf)
                .collect(Collectors.toSet());

        var toInsert = urls.stream().collect(Collectors.toMap(CrawlingTask::id, Function.identity(), (a, b) -> b));
        for (var e : existent) {
            toInsert.remove(e);
        }
        if (toInsert.isEmpty())
            return;

        var prioritized = toInsert.values().stream().map(task -> new NewUrl(task.id(), task.url(), getPriority(task))).toList();

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
        addUrlsInternal(deduplicated);
    }

    private int getPriority(CrawlingTask task) {
        var tags = task.tags();
        return Arrays.stream(tags).map(tag -> priorities.getOrDefault(tag, 0)).min(Comparator.naturalOrder()).orElse(0);
    }

    private static String joinIds(Set<CrawlingTask> urls) {
        return urls.stream().map(CrawlingTask::id).map("'%s'"::formatted).collect(Collectors.joining(", "));
    }

    public void setVisited(Set<CrawlingTask> tasks) {
        updateStatus(tasks, Status.PROCESSED);
        var size = tasks.size();
        processed += size;
        queued -= size;
    }

    public void setFailed(Set<CrawlingTask> tasks) {
        updateStatus(tasks, Status.FAILED);
        var size = tasks.size();
        failed += size;
        queued -= size;
    }

    public void updateStatus(Set<CrawlingTask> tasks, int status) {
        String ids = joinIds(tasks);
        String sql = "UPDATE links SET status = %d WHERE hash IN (%s)".formatted(status, ids);
        var updated = connection.update(sql);
        context.increaseCounter("visitedUrls", tasks.size());
        logger.debug("Marking {} urls as visited", updated);
    }

    public Set<String> getUnvisited(int count) {
        var sql = "SELECT url, hash FROM links WHERE status = 0 ORDER BY priority DESC LIMIT ?";
        var result = connection.query(sql, List.of(count));

        var urls = result.stream().map(r -> r.get(0)).map(String::valueOf).collect(Collectors.toSet());
        var hashes = result.stream().map(row -> row.get(1)).map("'%s'"::formatted).collect(Collectors.joining(", "));

        var update = "UPDATE links SET status = 1 WHERE hash IN (%s)".formatted(hashes);
        var updated = connection.update(update);
        logger.debug("Returned {} urls to process", updated);
        return urls;
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


record NewUrl(String hash, String url, int priority) {
}

