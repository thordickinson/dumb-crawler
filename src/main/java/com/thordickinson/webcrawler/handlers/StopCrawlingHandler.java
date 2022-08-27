package com.thordickinson.webcrawler.handlers;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.CrawlerService;
import com.thordickinson.webcrawler.api.AbstractFilteredPageHandler;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detiene la ejecuci
 */
@Service
public class StopCrawlingHandler extends AbstractFilteredPageHandler {

    private static final Logger logger = LoggerFactory.getLogger(StopCrawlingHandler.class);
    private long lastValidPageDetected = System.currentTimeMillis();
    private Long timeout = -1L;
    private Integer maxRejectedPageCount;
    private AtomicInteger rejectedPageCounter = new AtomicInteger(0);
    @Autowired
    private ApplicationContext applicationContext;

    public StopCrawlingHandler(){
        super("stopper");
    }

    @Override
    protected void initialize(CrawlingContext context, ConfigurationSupport config) {
        timeout = config.getConfig("timeout", context).map(Any::toLong).map(t -> t * 1000).orElseGet(() -> -1L);
        maxRejectedPageCount = config.getConfig("maxRejectedPageCount", context).map(Any::toInt).orElseGet(() -> -1);
    }

    @Override
    protected void handleFilteredPage(Page page, CrawlingContext context) throws Exception {
        lastValidPageDetected = System.currentTimeMillis();
    }

    private void stop(){
        CrawlerService service = applicationContext.getBean(CrawlerService.class);
        service.stopCrawler();
    }

    @Override
    protected void onPageRejected(Page page, CrawlingContext ctx) {
        var now = System.currentTimeMillis();
        var rejectedPageCount = rejectedPageCounter.incrementAndGet();
        if(timeout > 0 && lastValidPageDetected +  timeout < now){
            logger.info("Stopping crawler after timeout: {} ms", timeout);
            stop();
        }
        if(maxRejectedPageCount > 0 && rejectedPageCount > maxRejectedPageCount){
            logger.info("Stopping crawler after {} rejected pages", rejectedPageCount);
            stop();
        }
    }
}
