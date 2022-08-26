package com.thordickinson.webcrawler.storage;

import com.thordickinson.webcrawler.api.AbstractFilteredPageHandler;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.PageHandler;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
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

@Service
public class WARCStorage extends AbstractFilteredPageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WARCStorage.class);
    private WarcWriter writer;

    public WARCStorage(){
        super("warcStorage");
    }

    private WarcWriter createWriter(CrawlingContext context) throws IOException {
        var path = context.getJobDir().resolve("pages").resolve(context.getExecutionId() + ".warc");
        createFile(path);
        var channel = Channels.newChannel(new FileOutputStream(path.toFile()));
        writer = new WarcWriter(channel, WarcCompression.GZIP);
        return writer;
    }

    private void createFile(Path path) throws IOException {
        logger.info("Creating log file to {}", path.toFile().getAbsolutePath());
        path.getParent().toFile().mkdirs();
        path.toFile().createNewFile();
    }
    private WarcWriter getWriter(CrawlingContext context) throws IOException {
        if(writer == null){
            writer = createWriter(context);
        }
        return writer;
    }
    @Override
    public void handleFilteredPage(Page page, CrawlingContext context) throws Exception {
        WarcWriter writer = getWriter(context);
        String url = page.getWebURL().getURL().toLowerCase();
        context.increaseCounter("storedPages");
        logger.debug("Saving page to warc file: {}", url);
        WarcResponse response =  new WarcResponse.Builder(url)
                .body(MediaType.parse(page.getContentType()), page.getContentData())
                .date(Instant.now())
                .build();
        synchronized (writer){
            writer.write(response);
        }
    }

    @PreDestroy
    void close(){
        if(writer != null) {
            try {
                logger.info("Closing warc file");
                writer.close();
            }catch (IOException ex){
                logger.error("Error closing stream", ex);
            }
        }
    }
}
