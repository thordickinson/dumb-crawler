package com.thordickinson.dumbcrawler.services;

import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.util.Misc;

import static com.thordickinson.dumbcrawler.util.JDBCUtil.*;

public class URLStore {

    private static final Logger logger = LoggerFactory.getLogger(URLStore.class);
    private Connection cachedConnection;
    private final CrawlingContext context;

    public URLStore(CrawlingContext context) {
        this.context = context;
        cachedConnection = createConnection();
    }

    private void initialize(Connection connection) {
        String check = "SELECT name FROM sqlite_master WHERE type='table' AND name='links'";
        var checkResult = singleResult(String.class, connection, check);
        if (checkResult.isPresent()) {
            logger.info("Schema is initialized");
            return;
        }

        logger.info("Initializing schema");
        String table = "CREATE TABLE links (created_at DATETIME DEFAULT CURRENT_TIMESTAMP, url TEXT PRIMARY KEY, status INTEGER DEFAULT 0) WITHOUT ROWID";
        executeUpdate(connection, table);
        String index = "CREATE INDEX status_index ON links(status)";
        executeUpdate(connection, index);
        String cleanup = "UPDATE links SET status = 0 WHERE status = 1";
        var orphans = executeUpdate(connection, cleanup);
        if (orphans > 0)
            logger.warn("{} orphan urls were updated", orphans);
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

    private boolean shouldAddLink(String link) {
        if (link.length() > 1024) {
            logger.warn("Link is too long: [{}]", link);
            return false;
        }
        var uri = Misc.parseURI(link);
        if (uri.map(u -> u.getHost() != null
                && u.getHost().matches("^(carro|carros|vehiculo|vehiculos)\\.mercadolibre\\.com\\.co$"))
                .orElse(false)) {
            return true;
        }
        return false;
    }


    private boolean addUrlsInternal(Collection<String> urls) {
        if (urls.isEmpty())
            return false;

        var filtered = urls.stream().filter(this::shouldAddLink).collect(Collectors.toList());
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
        var sqlFormat = String.join(", ",
                toInsert.stream().map(s -> "('%s')".formatted(s)).collect(Collectors.toSet()));

        context.increaseCounter("discoveredUrls", toInsert.size());
        var insert = "INSERT INTO links (url) VALUES %s".formatted(sqlFormat);
        executeUpdate(connection, insert);
        logger.debug("New urls added: {}", toInsert.size());
        return true;
    }

    public void getStatus() {
        // singleResult(Integer.class, getConnection(), "SELECT count(*) FROM links
        // GROUP BY status");
    }

    public boolean addURLs(Set<String> urls) {
        if (urls.size() > 50) {
            logger.warn("Adding {} urls, spliting in batches", urls.size());
        }
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
        String strUrls = formatUrls(urls);
        String sql = "UPDATE links SET status = 2 WHERE url IN (%s)".formatted(strUrls);
        var updated = executeUpdate(getConnection(), sql);
        context.increaseCounter("visitedUrls", urls.size());
        logger.debug("Marking {} urls as visited", updated);
    }

    public Set<String> getUnvisited(int count) {
        var sql = "SELECT url FROM links WHERE status = 0 ORDER BY created_at ASC LIMIT ?";
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
