package com.thordickinson.webcrawler.api;

import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;

public abstract class AbstractFilteredPageHandler implements PageHandler {
    private final ConfigurationSupport config;
    protected AbstractFilteredPageHandler(String handlerKey) {
        config  = new ConfigurationSupport(handlerKey);
    }
    @Override
    public void handlePage(Page page, CrawlingContext context) throws Exception {
        if(config.shouldProceed(page, context))
        handleFilteredPage(page, context);
    }

    protected abstract void handleFilteredPage(Page page, CrawlingContext context) throws Exception;
}
