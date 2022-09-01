package com.thordickinson.dumbcrawler.services;

import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;

public class LogHandler implements CrawlingResultHandler {

    private CrawlingContext context;

    @Override
    public void initialize(CrawlingContext context) {
        this.context = context;
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {

    }

    @Override
    public void destroy() {
    }

}
