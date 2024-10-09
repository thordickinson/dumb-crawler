package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
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
public class StorageManager extends AbstractCrawlingComponent {


    public static final String LAST_NEW_PAGE_SAVED_AT_KEY = "LAST_NEW_PAGE_SAVED_AT";

    private Path currentFile;
    private long maxFileSize = 1024 * 1024 * 50; // 50 MB max file size
    private SQLiteConnection dbConnection;
    private Set<String> allowedTags = Collections.emptySet();

    public StorageManager() {
        super("storage");
    }


    private boolean shouldStore(CrawlingResult result, CrawlingSessionContext sessionContext){
        for(var tag : result.task().tags()){
            if(allowedTags.contains(tag)){
                return true;
            }
        }
        return false;
    }

    public void storeResult(CrawlingResult result, CrawlingSessionContext sessionContext) {
        if(!shouldStore(result, sessionContext)){
            logger.debug("Ignoring url: {}", result.task().url());
            sessionContext.increaseCounter("UNSAVED_PAGES");
            return;
        }

        var taskId = result.task().urlId();
        var fileToUpdate = getFileLocation(taskId);
        sessionContext.increaseCounter("SavedPages");
        var url = result.task().url();
        var html = result.content();
        if (fileToUpdate.isPresent()) {
            updateWarcFile(fileToUpdate.get(), url, html, sessionContext);
        } else {
            saveToCurrentFile(url, html, sessionContext);
        }
    }

    /**
     * Appends the page to the index that relates pages to files.
     *
     * @param resourceId the page to store
     */
    private void addToIndex(String resourceId) {
        //Adds the page + the currentFile to our index [do not implement this]
        var path = getContext().getDataPath().relativize(currentFile).toString();
        dbConnection.update("INSERT INTO url_index (url_hash, file_path) VALUES (?, ?)", List.of(resourceId, path));
    }

    /**
     * Will return the file location where this page is currently located, so the file
     * can be updated including the new version.
     *
     * @param resourceId the id or hash of the url of the resource
     * @return the path of the file containing the current version of the page or empty if there is
     * no file containing the document.
     */
    private Optional<Path> getFileLocation(String resourceId) {
        return dbConnection.singleResult(String.class, "SELECT file_path FROM url_index WHERE url_hash = ?", resourceId)
                .map(Path::of);
    }

    /**
     * Updates an existing WARC file with new content.
     *
     * @param warcFilePath The path to the WARC file.
     * @param url          The URL of the page being archived.
     * @param content      The content of the web page.
     */
    private void updateWarcFile(Path warcFilePath, String url, String content, CrawlingSessionContext sessionContext) {

        logger.info("Updating file with new crawled page {}", url);
        sessionContext.increaseCounter("UPDATED_PAGES");
        try (OutputStream outStream = Files.newOutputStream(warcFilePath, StandardOpenOption.APPEND)) {
            WarcWriter writer = new WarcWriter(outStream);

            // Create a new WARC response record
            WarcResponse response = createResponse(url, content);
            // Write the new record to the WARC file
            writer.write(response);

            logger.info("Updated WARC file with new content for URL: {}", url);
        } catch (IOException e) {
            logger.error("Error updating WARC file: {}", warcFilePath, e);
        }
    }

    private WarcResponse createResponse(String url, String content){
        return new WarcResponse.Builder(URI.create(url))
                .body(MediaType.HTML_UTF8, content.getBytes(StandardCharsets.UTF_8))
                .date(Instant.now())
                .build();
    }

    /**
     * Saves new content to the current WARC file. If the file exceeds the max size,
     * a new WARC file is created.
     *
     * @param url     The URL of the page being archived.
     * @param content The content of the web page.
     */
    private void saveToCurrentFile(String url, String content, CrawlingSessionContext sessionContext) {
        sessionContext.increaseCounter("NEW_SAVED_PAGES");
        sessionContext.setVariable(LAST_NEW_PAGE_SAVED_AT_KEY, System.currentTimeMillis());
        logger.info("Saving page for the first time {}", url);
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
                WarcResponse response = createResponse(url, content);
                // Write the new record to the WARC file
                writer.write(response);
                // Add the resource to the index (for future updates)
                addToIndex(url);
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
        Path newFilePath = getContext().getDataPath().resolve(fileName);

        // Ensure the directory exists
        Files.createDirectories(newFilePath.getParent());

        // Create the new file
        Files.createFile(newFilePath);
        logger.info("Created new WARC file: {}", newFilePath);
        return newFilePath;
    }

    private void initializeDB(CrawlingSessionContext context){
        dbConnection = new SQLiteConnection(context.getDataPath(), "file_index");
        dbConnection.addTable("url_index", Map.of("url_hash", "TEXT NOT NULL PRIMARY KEY", "file_path", "TEXT NOT NULL"), false);
    }


    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        // Initialize the StorageManager if needed
        int maxFileSizeMb = context.getIntConf("storage.maxFileSize", 50);
        maxFileSize = 1024L * 1024 * maxFileSizeMb;
        initializeDB(context);
        allowedTags = context.getConfig("storage.includedTags")
                .map(Any::asList).map(l -> (Set<String>) new HashSet<String>(l.stream().map(Object::toString).toList()))
                .orElseGet(Collections::emptySet);
    }
}
