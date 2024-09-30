package com.thordickinson.dumbcrawler.services;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;

public class LogHandler implements CrawlingResultHandler {

    private CrawlingSessionContext context;

    @Override
    public void initialize(CrawlingSessionContext context) {
        this.context = context;
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {

    }

    @Override
    public void destroy() {
    }

}
