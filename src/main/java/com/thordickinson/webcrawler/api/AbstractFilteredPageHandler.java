package com.thordickinson.webcrawler.api;

import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFilteredPageHandler implements PageHandler {
    private final ConfigurationSupport config;
    private final Logger logger = LoggerFactory.getLogger(getClass() + ".filter");
    private boolean initialized = false;

    protected AbstractFilteredPageHandler(String handlerKey) {
        config = new ConfigurationSupport(handlerKey);
    }

    private void ensureInitialized(CrawlingContext context){
        if(initialized) return;
        config.ensureInitialized(context);
        initialize(context, config);
        initialized = true;
    }

    protected void initialize(CrawlingContext context, ConfigurationSupport config){
    }

    @Override
    public void handlePage(Page page, CrawlingContext context) throws Exception {
        ensureInitialized(context);
        var url = page.getWebURL().getURL();
        if (config.shouldProceed(page, context)) {
            logger.debug("ACCEPT: {}", url);
            handleFilteredPage(page, context);
        } else {
            logger.debug("REJECT: {}", url);
            onPageRejected(page, context);
        }
    }

    protected void onPageRejected(Page page, CrawlingContext ctx){
    }

    protected ConfigurationSupport getConfig() {
        return config;
    }

    protected abstract void handleFilteredPage(Page page, CrawlingContext context) throws Exception;
}
