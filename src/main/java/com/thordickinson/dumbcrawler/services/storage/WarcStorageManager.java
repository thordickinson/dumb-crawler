package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.util.SQLiteConnection;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcResponse;
import org.netpreserve.jwarc.WarcWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;


@Service
public class WarcStorageManager extends AbstractStorageManager {


    public static final String LAST_NEW_PAGE_SAVED_AT_KEY = "LAST_NEW_PAGE_SAVED_AT";

    private Path currentFile;
    private long maxFileSize = 1024 * 1024 * 50; // 50 MB max file size
    private SQLiteConnection dbConnection;

    public WarcStorageManager() {
        super("warcStorage");
    }


    protected void doStoreResult(CrawlingResult result, CrawlingSessionContext sessionContext) {
        var fileToUpdate = getFileLocation(result.task());
        var html = result.content();
        if (fileToUpdate.isPresent()) {
            updateWarcFile(fileToUpdate.get(), result.task(), html, sessionContext);
        } else {
            saveToCurrentFile(result.task(), html, sessionContext);
        }
    }

    private void addToIndex(CrawlingTask task) {
        //Adds the page + the currentFile to our index [do not implement this]
        var path = getContext().getCrawlDir().relativize(currentFile).toString();
        dbConnection.update("INSERT INTO url_index (url_hash, file_path) VALUES (?, ?)", List.of(task.urlId(), path));
    }

    private Optional<Path> getFileLocation(CrawlingTask task) {
        return dbConnection.singleResult(String.class, "SELECT file_path FROM url_index WHERE url_hash = ?", task.urlId())
                .map(Path::of);
    }

    private void updateWarcFile(Path warcFilePath, CrawlingTask task, String content, CrawlingSessionContext sessionContext) {

        logger.info("Updating file with new crawled page {}", task.url());
        sessionContext.increaseCounter("UPDATED_PAGES");
        try (OutputStream outStream = Files.newOutputStream(warcFilePath, StandardOpenOption.APPEND)) {
            WarcWriter writer = new WarcWriter(outStream);

            // Create a new WARC response record
            WarcResponse response = createResponse(task, content);
            // Write the new record to the WARC file
            writer.write(response);

            logger.info("Updated WARC file with new content for URL: {}", task.url());
        } catch (IOException e) {
            logger.error("Error updating WARC file: {}", warcFilePath, e);
        }
    }

    private WarcResponse createResponse(CrawlingTask task, String content){
        return new WarcResponse.Builder(URI.create(task.url()))
                .body(MediaType.HTML_UTF8, content.getBytes(StandardCharsets.UTF_8))
                .date(Instant.now())
                .build();
    }

    /**
     * Saves new content to the current WARC file. If the file exceeds the max size,
     * a new WARC file is created.
     *
     * @param task     The URL of the page being archived.
     * @param content The content of the web page.
     */
    private void saveToCurrentFile(CrawlingTask task, String content, CrawlingSessionContext sessionContext) {
        sessionContext.increaseCounter("NEW_SAVED_PAGES");
        sessionContext.setVariable(LAST_NEW_PAGE_SAVED_AT_KEY, System.currentTimeMillis());
        logger.info("Saving page for the first time {}", task.url());
        try {
            // Check if the current file exists and its size
            if (currentFile == null || Files.notExists(currentFile) || Files.size(currentFile) >= maxFileSize) {
                // If the file does not exist or is too large, create a new WARC file
                currentFile = createNewWarcFile();
            }

            // Open the current WARC file in append mode
            try (OutputStream outStream = Files.newOutputStream(currentFile, StandardOpenOption.APPEND)) {
                WarcWriter writer = new WarcWriter(outStream);
                // Create a new WARC response record
                WarcResponse response = createResponse(task, content);
                // Write the new record to the WARC file
                writer.write(response);
                // Add the resource to the index (for future updates)
                addToIndex(task);
                logger.info("Saved new content to WARC file: {}", currentFile);
            }
        } catch (IOException e) {
            logger.error("Error saving content to WARC file", e);
        }
    }

    /**
     * Creates a new WARC file and returns its path.
     *
     * @return The path of the newly created WARC file.
     */
    private Path createNewWarcFile() throws IOException {
        // Generate a unique filename based on the timestamp
        String fileName = "crawl_" + Instant.now().toEpochMilli() + ".warc";
        Path newFilePath = getContext().getCrawlDir().resolve(fileName);

        // Ensure the directory exists
        Files.createDirectories(newFilePath.getParent());

        // Create the new file
        Files.createFile(newFilePath);
        logger.info("Created new WARC file: {}", newFilePath);
        return newFilePath;
    }

    private void initializeDB(CrawlingSessionContext context){
        dbConnection = new SQLiteConnection(context.getSessionDir(), "file_index");
        dbConnection.addTable("url_index", Map.of("url_hash", "TEXT NOT NULL PRIMARY KEY", "file_path", "TEXT NOT NULL"), false);
    }


    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        // Initialize the StorageManager if needed
        int maxFileSizeMb = context.getIntConf("storage.maxFileSize", 50);
        maxFileSize = 1024L * 1024 * maxFileSizeMb;
        initializeDB(context);
        super.loadConfigurations(context);
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
