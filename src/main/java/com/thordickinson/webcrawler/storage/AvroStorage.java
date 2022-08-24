package com.thordickinson.webcrawler.storage;

import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.PageHandler;
import com.thordickinson.webcrawler.storage.avro.schema.WebPage;
import edu.uci.ics.crawler4j.crawler.Page;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class AvroStorage implements PageHandler {
    private static Logger logger = LoggerFactory.getLogger(AvroStorage.class);
    private DatumWriter<WebPage> writer = null;
    private DataFileWriter<WebPage> fileWriter;

    @Override
    public void handlePage(Page page, CrawlingContext context) throws Exception {
        var record = createRecord(page, context);
        logger.info("Saving page in avro format {}", record.getUrl());
        write(record, context);
    }

    private DataFileWriter<WebPage> initializeWriter(CrawlingContext ctx) throws IOException {
        var file  = ctx.getJobDir().resolve("pages").resolve(String.valueOf(System.currentTimeMillis()) + ".avro");
        // createFile(file);
        logger.info("Avro file created: {}", file.toString());
        writer = new SpecificDatumWriter<>(WebPage.class);
        fileWriter = new DataFileWriter<>(writer);
        fileWriter.create(WebPage.getClassSchema(), file.toFile());
        return fileWriter;
    }
    private DataFileWriter<WebPage> getWriter(CrawlingContext ctx) throws IOException {
        if (fileWriter == null) {
            initializeWriter(ctx);
        }
        return fileWriter;
    }

    private void write(WebPage record, CrawlingContext ctx) throws IOException {
        var writer = getWriter(ctx);
        writer.append(record);
    }

    private void close() {
        if (fileWriter != null) {
            try {
                logger.info("Closing avro file");
                fileWriter.close();
            } catch (IOException ex) {
                logger.error("Error closing output stream", ex);
            }
        }

    }

    private WebPage createRecord(Page page, CrawlingContext ctx) {
        var url = page.getWebURL().getURL().toLowerCase();
        var data = new String(page.getContentData(), StandardCharsets.UTF_8);
        var language = Optional.ofNullable(page.getLanguage()).orElseGet(() -> "en");
        logger.info("Writing data for [{}]: {} ", data.length(), url);
        return WebPage.newBuilder()
                .setContentType(page.getContentType())
                .setLanguage(language)
                .setLength(data.length())
                .setContent(data)
                .setUrl(url)
                .setStatus(page.getStatusCode()).setTimestamp(System.currentTimeMillis()).build();
    }

    @PreDestroy
    public void destroy() {
        close();
    }
}
