package com.thordickinson.webcrawler.api;

import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFilteredPageHandler implements PageHandler {
    private final ConfigurationSupport config;
    private final Logger logger = LoggerFactory.getLogger(getClass() + ".filter");

    protected AbstractFilteredPageHandler(String handlerKey) {
        config = new ConfigurationSupport(handlerKey);
    }

    @Override
    public void handlePage(Page page, CrawlingContext context) throws Exception {
        var url = page.getWebURL().getURL();
        if (config.shouldProceed(page, context)) {
            logger.debug("ACCEPT: {}", url);
            handleFilteredPage(page, context);
        } else {
            logger.debug("REJECT: {}", url);
        }
    }

    protected abstract void handleFilteredPage(Page page, CrawlingContext context) throws Exception;
}
