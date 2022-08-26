package com.thordickinson.webcrawler.storage;

import com.github.slugify.Slugify;
import com.thordickinson.webcrawler.api.AbstractFilteredPageHandler;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.PageHandler;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.util.IO;
import org.netpreserve.jwarc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WARCStorage extends AbstractFilteredPageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WARCStorage.class);
    private final Slugify slugify = Slugify.builder().lowerCase(true).underscoreSeparator(true).build();
    private final ConcurrentHashMap<Thread, WarcWriter> writers = new ConcurrentHashMap<>();

    public WARCStorage() {
        super("warcStorage");
    }

    private WarcWriter createWriter(Thread thread, CrawlingContext context) {
        String fileName = slugify.slugify(context.getExecutionId() + "_" + thread.getName());
        var path = context.getExecutionDir().resolve("warc").resolve(fileName + ".warc");
        try {
            createFile(path);
            var channel = Channels.newChannel(new FileOutputStream(path.toFile()));
            var writer = new WarcWriter(channel, WarcCompression.GZIP);
            return writer;
        } catch (IOException ex) {
            throw new RuntimeException("Error creating thread output file", ex);
        }
    }

    private void createFile(Path path) throws IOException {
        logger.info("Creating warc file: {}", path.toFile().getAbsolutePath());
        path.getParent().toFile().mkdirs();
        path.toFile().createNewFile();
    }

    private WarcWriter getWriter(CrawlingContext context) throws IOException {
        var local = writers.computeIfAbsent(Thread.currentThread(), (t) -> createWriter(t, context));
        return local;
    }

    @Override
    public void handleFilteredPage(Page page, CrawlingContext context) throws Exception {
        WarcWriter writer = getWriter(context);
        String url = page.getWebURL().getURL().toLowerCase();
        context.increaseCounter("storedPages");
        logger.debug("Saving page to warc file: {}", url);
        WarcResponse response = new WarcResponse.Builder(url)
                .body(MediaType.parse(page.getContentType()), page.getContentData())
                .date(Instant.now())
                .build();
        //Instead of syncing the same writer we should create a thread local writer
        writer.write(response);
    }

    @PreDestroy
    void close() {
        writers.values().forEach(w -> {
            try {
                w.close();
            } catch (IOException ex) {
                logger.warn("Error closing file", ex);
            }
        });
    }
}
