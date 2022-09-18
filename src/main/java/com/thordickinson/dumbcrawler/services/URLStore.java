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
import static com.thordickinson.dumbcrawler.util.JsonUtil.*;

import com.thordickinson.dumbcrawler.util.JDBCUtil;
import com.thordickinson.dumbcrawler.util.URLExpressionEvaluator;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.DefaultURLHasher;
import com.thordickinson.dumbcrawler.api.URLHasher;

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
    @Setter @Getter
    private List<URLHasher> urlHashers = Collections.emptyList();
    private final URLHasher defaultHasher =  new DefaultURLHasher();

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
        String table = "CREATE TABLE links (hash TEXT PRIMARY KEY, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, url TEXT, " +
                "status INTEGER DEFAULT 0, priority INTEGER DEFAULT 0) WITHOUT ROWID";
        executeUpdate(connection, table);
        executeUpdate(connection, "CREATE INDEX url_index ON links(url)");
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
        if(!uri.startsWith("http")){
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
                .findFirst().map(f -> f.value()).orElse(0);
        logger.trace("URL priority: [{}]: {}", priority, url);
        return priority;
    }

    private String hashUrl(String url){
        for(var hasher : urlHashers){
            var result = hasher.hashUrl(url);
            if(result.isPresent()) return result.get();
        }
        return defaultHasher.hashUrl(url).get();
    }

    private boolean addUrlsInternal(Collection<String> urls, boolean filter) {
        if (urls.isEmpty())
            return false;

        var filtered = filter? urls.stream().filter(this::shouldAddLink)
                .collect(Collectors.toList()) : new ArrayList<>(urls);

        context.increaseCounter("ingnoredUrls", urls.size() - filtered.size());

        if (filtered.isEmpty())
            return false;

        var hashedUrls =  new HashMap<String,String>();
        for(var url : filtered){
            hashedUrls.put(hashUrl(url), url);
        }
        
        var connection = getConnection();
        var hashList = new ArrayList<String>(hashedUrls.keySet());
        var placeholders = JDBCUtil.generateParams(hashList.size());
        var exists = "SELECT hash FROM links WHERE hash in %s".formatted(placeholders);
        var existent = query(connection, exists,  hashList)
        .stream().map(r -> r.get(0)).map(String::valueOf)
                .collect(Collectors.toSet());

        var toInsert = new HashMap<>(hashedUrls);
        for(var e : existent){
            toInsert.remove(e);
        }
        if (toInsert.isEmpty())
            return false;
        
        var prioritized = toInsert.entrySet().stream().map(e -> new NewUrl(e.getValue(), e.getKey(), getPriority(e.getValue()))).collect(Collectors.toList());

        var sqlFormat = JDBCUtil.generateParams(3, prioritized.size());
        List<Object> params = prioritized.stream().flatMap(e -> Stream.of(e.hash(), e.url(), e.priority()))
        .collect(Collectors.toList());
        var insert = "INSERT INTO links (hash, url, priority) VALUES %s".formatted(sqlFormat);
        executeUpdate(connection, insert, params);

        context.increaseCounter("discoveredUrls", prioritized.size());
        queued += prioritized.size();
        logger.debug("New urls added: {}", prioritized.size());
        return true;
    }

    public Map<String, Integer> getStatus() {
        return Map.of("QUEUED", queued, "PROCESSED", processed, "FAILED", failed);
    }

    public boolean addURLs(Set<String> urls) {
        logger.debug("Receiving {} urls to process", urls.size());
        var chunks = Lists.partition(new ArrayList<>(urls), 50);
        return chunks.stream().map(s -> addUrlsInternal(s, true)).reduce((a, b) -> a || b).orElse(false);
    }

    public void addSeeds(Set<String> seeds) {
        this.addUrlsInternal(seeds, false);
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

record NewUrl(String url, String hash, int priority){ 
}

