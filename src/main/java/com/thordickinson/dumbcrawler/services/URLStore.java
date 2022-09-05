package com.thordickinson.dumbcrawler.services;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import com.jsoniter.any.Any;
import static com.thordickinson.webcrawler.util.JsonUtil.*;

import com.thordickinson.dumbcrawler.util.URLExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.util.ParsedURI;

import static com.thordickinson.dumbcrawler.util.JDBCUtil.*;

public class URLStore {

    public static class Status {
        public static int QUEUED = 0;
        public static int PROCESSING = 1;
        public static int PROCESSED = 2;
        public static int FAILED = 3;
    }

    private static final Logger logger = LoggerFactory.getLogger(URLStore.class);
    private Connection cachedConnection;
    private final CrawlingContext context;
    private int queued = 0;
    private int processed = 0;
    private int failed = 0;
    private List<PriorityFilter> priorityFilters;
    private Optional<String> urlFilter = Optional.empty();
    private final URLExpressionEvaluator expressionEvaluator =  new URLExpressionEvaluator();

    public URLStore(CrawlingContext context) {
        this.context = context;
        cachedConnection = createConnection();
        priorityFilters = context.getConfig("crawler.priorities").map(Any::asList)
                .map(l -> l.stream().map(this::makeFilter).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
        urlFilter = context.getConfig("crawler.urlFilter").map(Any::toString);
    }

    private PriorityFilter makeFilter(Any value){
        var filter = get(value, "urlFilter").map(Any::toString).orElse(null);
        var priority = get(value, "priority").map(Any::toInt).orElse(0);
        return new PriorityFilter(filter, priority);
    }


    private void loadCounters(Connection connection) {
        var sql = "select status, count(*) as count from links group by status";
        var counters = query(connection, sql);
        for (var counter : counters) {
            var status = (int) counter.get(0);
            switch (status) {
                case 0 -> queued = (int) counter.get(1);
                case 2 -> processed = (int) counter.get(1);
                case 3 -> failed = (int) counter.get(1);
            }
        }
    }

    private void updateOrphans(Connection connection) {
        var orphans = executeUpdate(connection, "UPDATE links SET status = 0 WHERE status = 1");
        if (orphans > 0)
            logger.warn("{} orphan urls were updated", orphans);
    }

    private void markForRefetch(Connection connection) {
        boolean refetch = false; // TODO: Make this configurable with a argument
        if (refetch) {
            logger.warn("Marking all links for refetch");
            executeUpdate(connection, "UPDATE links SET status = 0"); // This will force to revisit all the pages
            logger.warn("Refetch update completed");
        }
    }

    private void initialize(Connection connection) {
        String check = "SELECT name FROM sqlite_master WHERE type='table' AND name='links'";
        var checkResult = singleResult(String.class, connection, check);
        if (checkResult.isPresent()) {
            logger.info("Schema is initialized");
            updateOrphans(connection);
            loadCounters(connection);
            markForRefetch(connection);
            return;
        }

        logger.info("Initializing schema");
        String table = "CREATE TABLE links (created_at DATETIME DEFAULT CURRENT_TIMESTAMP, url TEXT PRIMARY KEY, " +
                "status INTEGER DEFAULT 0, priority INTEGER DEFAULT 0) WITHOUT ROWID";
        executeUpdate(connection, table);
        executeUpdate(connection, "CREATE INDEX status_index ON links(status)");
        executeUpdate(connection, "CREATE INDEX priority_index ON links(priority)");
        logger.info("Schema creation complete");
    }

    private Connection createConnection() {
        Path url = context.getExecutionDir().resolve("db.sqlite");
        url.getParent().toFile().mkdirs();
        try {
            var conn = DriverManager.getConnection("jdbc:sqlite:%s".formatted(url.toAbsolutePath()));
            logger.info("Connected to database at: %s".formatted(url));
            initialize(conn);
            return conn;
        } catch (SQLException ex) {
            throw new RuntimeException("Error connecting to: %s".formatted(url), ex);
        }
    }

    private Connection getConnection() {
        return cachedConnection;
    }

    private boolean shouldAddLink(String uri) {
        var result = urlFilter.map(r -> expressionEvaluator.evaluate(r, uri)).orElse(true);
        logger.trace("URL filtered [{}]: {}", result, uri);
        return result;
    }

    private int getPriority(String url) {
        var priority = priorityFilters.stream()
                .filter(f -> expressionEvaluator.evaluate(f.filter(), url))
                .findFirst().map(f -> f.value()).orElse(0);
        logger.trace("URL priority: [{}]: {}", priority, url);
        return priority;
    }

    private boolean addUrlsInternal(Collection<String> urls) {
        if (urls.isEmpty())
            return false;

        var filtered = urls.stream().filter(this::shouldAddLink)
                .collect(Collectors.toList());

        context.increaseCounter("ingnoredUrls", urls.size() - filtered.size());

        if (filtered.isEmpty())
            return false;

        var connection = getConnection();
        var placeholders = String.join(", ", filtered.stream().map(f -> "?").collect(Collectors.toList()));
        var exists = "SELECT url FROM links WHERE url in (%s)".formatted(placeholders);
        var existent = query(connection, exists, filtered).stream().map(r -> r.get(0)).map(String::valueOf)
                .collect(Collectors.toSet());

        var toInsert = new HashSet<>(filtered);
        toInsert.removeAll(existent);
        if (toInsert.isEmpty())
            return false;
        var prioritized = toInsert.stream()
                .collect(Collectors.toMap(u -> u, u -> getPriority(u)));

        context.increaseCounter("discoveredUrls", toInsert.size());

        var sqlFormat = String.join(", ",
                toInsert.stream().map(s -> "(?, ?)").collect(Collectors.toList()));
        List<Object> params = prioritized.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        queued += toInsert.size();
        var insert = "INSERT INTO links (url, priority) VALUES %s".formatted(sqlFormat);
        executeUpdate(connection, insert, params);
        logger.debug("New urls added: {}", toInsert.size());
        return true;
    }

    public Map<String, Integer> getStatus() {
        return Map.of("QUEUED", queued, "PROCESSED", processed, "FAILED", failed);
    }

    public boolean addURLs(Set<String> urls) {
        logger.debug("Receiving {} urls to process", urls.size());
        var chunks = Lists.partition(new ArrayList<>(urls), 50);
        return chunks.stream().map(this::addUrlsInternal).reduce((a, b) -> a || b).orElse(false);
    }

    public void addSeeds(Set<String> seeds) {
        this.addURLs(seeds);
    }

    private static String formatUrls(Set<String> urls) {
        return String.join(", ", urls.stream().map(s -> "'%s'".formatted(s)).collect(Collectors.toList()));
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
        var updated = executeUpdate(getConnection(), sql);
        context.increaseCounter("visitedUrls", urls.size());
        logger.debug("Marking {} urls as visited", updated);
    }

    public Set<String> getUnvisited(int count) {
        var sql = "SELECT url FROM links WHERE status = 0 ORDER BY priority DESC LIMIT ?";
        var result = query(getConnection(), sql, List.of(count));
        var unvisited = result.stream().map(r -> r.get(0)).map(String::valueOf).collect(Collectors.toSet());
        var urlList = String.join(", ", unvisited.stream().map(s -> "'%s'".formatted(s)).collect(Collectors.toList()));
        var update = "UPDATE links SET status = 1 WHERE url IN (%s)".formatted(urlList);
        var updated = executeUpdate(getConnection(), update);
        logger.debug("Returned {} urls to process", updated);
        return unvisited;
    }

    @PreDestroy
    void destroy() {
        if (cachedConnection != null) {
            try {
                logger.debug("Closing connection");
                cachedConnection.close();
            } catch (Exception ex) {
                logger.warn("Error closing connection", ex);
            }
        }
    }
}

record PriorityFilter(String filter, Integer value){
}

