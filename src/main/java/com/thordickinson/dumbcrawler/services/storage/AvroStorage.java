package com.thordickinson.dumbcrawler.services.storage;

import java.io.IOException;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;
import com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage;

@Service
public class AvroStorage implements CrawlingResultHandler {

    private static Logger logger = LoggerFactory.getLogger(AvroStorage.class);
    private DatumWriter<WebPage> writer = null;
    private DataFileWriter<WebPage> fileWriter;
    private CrawlingContext context;

    @Override
    public void initialize(CrawlingContext context) {
        this.context = context;
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {
        var record = createRecord(result);
        logger.info("Saving page in avro format {}", record.getUrl());
        try {
            write(record);
        } catch (IOException ex) {
            throw new RuntimeException("Error writing file", ex);
        }
    }

    private DataFileWriter<WebPage> initializeWriter() throws IOException {
        var file = context.getExecutionDir().resolve("avro")
                .resolve(context.getExecutionId() + ".avro");
        file.getParent().toFile().mkdirs();
        logger.info("Avro file created: {}", file.toString());
        writer = new SpecificDatumWriter<>(WebPage.class);
        fileWriter = new DataFileWriter<>(writer);
        if (file.toFile().isFile()) {
            fileWriter.appendTo(file.toFile());
        } else {
            fileWriter.create(WebPage.getClassSchema(), file.toFile());
        }
        return fileWriter;
    }

    private DataFileWriter<WebPage> getWriter() throws IOException {
        if (fileWriter == null) {
            initializeWriter();
        }
        return fileWriter;
    }

    private void write(WebPage record) throws IOException {
        var writer = getWriter();
        writer.append(record);
    }

    @Override
    public void destroy() {
        close();
    }

    private WebPage createRecord(CrawlingResult result) {
        var url = result.requestedUrl();
        var data = result.page().content().orElse("");
        logger.info("Writing data for [{}]: {} ", data.length(), url);
        return WebPage.newBuilder()
                .setContentType(result.page().contentType().orElse(""))
                .setContent(data)
                .setUrl(url)
                .setTimestamp(System.currentTimeMillis()).build();
    }

    private void close() {
        if (fileWriter != null) {
            try {
                logger.info("Closing avro file");
                fileWriter.close();
                fileWriter = null;
            } catch (IOException ex) {
                logger.error("Error closing output stream", ex);
            }
        }
    }

}
