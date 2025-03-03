package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import org.apache.hadoop.shaded.org.codehaus.jackson.map.ObjectMapper;
import org.apache.hadoop.shaded.org.codehaus.jackson.map.SerializationConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@Service
public class GZipStorageManager extends AbstractStorageManager {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationConfig.Feature.INDENT_OUTPUT);
    public GZipStorageManager() {
        super("storage");
    }

    private Map<String,String> getMetadata(CrawlingResult result){
        return Map.of("id", result.task().taskId(),
                "url", result.task().url(),
                "time", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
    }

    @Override
    protected void doStoreResult(CrawlingResult result, CrawlingSessionContext sessionContext) throws IOException {
        final var folder = sessionContext.getCrawlDir().resolve(result.task().urlId());
        Files.createDirectories(folder);

        final var metadata = getMetadata(result);
        final var metadataFilePath = folder.resolve("metadata.json");
        saveJSONFile(metadataFilePath, metadata);

        final String content = result.content();
        final var filePath = folder.resolve("content.zip");
        saveGzippedContent(filePath, content);
    }

    private void saveJSONFile(Path filePath, Object content) throws IOException {
        objectMapper.writeValue(filePath.toFile(), content);
    }

    private void saveGzippedContent(Path gzipFilePath, String content) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(gzipFilePath, StandardOpenOption.CREATE);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
            gzipOutputStream.write(content.getBytes());
        }
    }
}
