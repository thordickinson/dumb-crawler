package com.thordickinson.dumbcrawler.services;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.util.JDBCUtil;
import com.thordickinson.dumbcrawler.util.SQLiteConnection;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public URLStore(CrawlingSessionContext context) {
        this.context = context;
        connection = new SQLiteConnection(context.getSessionDir());
        initialize();
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
        var orphans = connection.update("UPDATE links SET status = ?, taken_at = NULL WHERE status = ?", Status.QUEUED, Status.PROCESSING);
        if (orphans > 0)
            logger.warn("{} orphan urls were updated", orphans);
    }

    private void resetStatus() {
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
            resetStatus();
            return;
        }

        logger.info("Initializing schema");
        String table = "CREATE TABLE links (" +
                "hash TEXT PRIMARY KEY, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "url TEXT, " +
                "tags TEXT, " +
                "status INTEGER DEFAULT 0, " +
                "priority INTEGER DEFAULT 0, " +
                "taken_at DATETIME, " +
                "completed_at DATETIME, " +
                "error TEXT, " +
                "attempt_count INTEGER DEFAULT 0 " +
                ") WITHOUT ROWID";
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

    private String toSqlParams(CrawlingTask task){
        String tags = String.join(",", task.tags());
        return "( '%s', '%s', '%s', %d )".formatted(task.urlId(), task.url(), tags, task.priority());
    }

    private void addUrlsInternal(Collection<CrawlingTask> tasks) {
        if (tasks.isEmpty())
            return;

        logger.debug("Attempting to add {} new  urls", tasks.size());
        var filtered = tasks.stream().filter(this::shouldAddLink).toList();
        context.increaseCounter("ingnoredUrls", tasks.size() - filtered.size());

        if (filtered.isEmpty()) {
            logger.debug("No new urls to append");
            return;
        }

        logger.debug("{} new urls to append", tasks.size());
        String placeholders = JDBCUtil.generateParams(tasks.size());
        var params = tasks.stream().map(CrawlingTask::urlId).toList();
        var exists = "SELECT hash FROM links WHERE hash in %s".formatted(placeholders);
        var existent = connection.query(exists, params)
                .stream().map(List::getFirst).map(String::valueOf)
                .collect(Collectors.toSet());

        var toInsert = tasks.stream().collect(Collectors.toMap(CrawlingTask::urlId, Function.identity(), (a, b) -> b));
        for (var e : existent) {
            toInsert.remove(e);
        }
        if (toInsert.isEmpty())
            return;


        var objects = toInsert.values().stream().map(e -> List.<Object>of(e.urlId(), e.url(), String.join(",", e.tags()), e.priority())).toList();
        connection.insertMany("links", List.of("hash", "url", "tags", "priority"), objects);

        context.increaseCounter("discoveredUrls", toInsert.size());
        queued += toInsert.size();
        logger.debug("New urls added: {}", toInsert.size());
    }

    public Map<String, Integer> getStatus() {
        return Map.of("QUEUED", queued, "PROCESSED", processed, "FAILED", failed);
    }

    public void addTasks(Collection<CrawlingTask> tasks) {
        var deduplicated = new HashSet<>(tasks);
        addUrlsInternal(deduplicated);
    }

    public void markTaskAsProcessed(CrawlingTask task) {
       markProcessed(task, Status.PROCESSED, null);
    }

    public void markTasAsFailed(CrawlingTask task, Throwable ex){
        String detail = ex.toString();
        if(ex instanceof CrawlingException){
            detail = ((CrawlingException) ex).getErrorCode();
        }
        markProcessed(task, Status.FAILED, detail);
    }

    private void markProcessed(CrawlingTask task, int status, String error){
        var sql = "UPDATE links SET status = ?, completed_at = CURRENT_TIMESTAMP, error = ? WHERE hash = ?";
        var updated = connection.update(sql, status, error, task.urlId());
        if(updated != 1){
            logger.warn("Unexpected update count {}", updated);
        }
    }

    public List<CrawlingTask> getUnvisited(int count) {
        var sql = "SELECT url, hash, tags, priority FROM links WHERE status = ? ORDER BY priority DESC LIMIT ?";
        var tasks = connection.query(sql, List.of(Status.QUEUED, count));
        var results = tasks.stream().map( row -> {
            var taskId = UUID.randomUUID().toString();
            var hash = String.valueOf(row.get(1));
            var url = String.valueOf(row.get(0));
            var tags = String.valueOf(row.get(2)).split(",");
            var priority = (int) row.get(3);
            return new CrawlingTask(taskId, hash, url, tags, priority);
        }).toList();

        var params = new LinkedList<>();
        params.add(Status.PROCESSING);
        params.addAll(results.stream().map(CrawlingTask::urlId).toList());
        var update = "UPDATE links SET status = ?, taken_at = CURRENT_TIMESTAMP WHERE hash IN %s".formatted(JDBCUtil.generateParams(results.size()));
        var updated = connection.update(update, params);
        if(updated != results.size()){
            logger.warn("Cannot mark all the urls as taken");
        }
        logger.debug("Returned {} urls to process", updated);
        return results;
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


