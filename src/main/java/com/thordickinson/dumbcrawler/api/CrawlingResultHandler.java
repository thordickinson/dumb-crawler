package com.thordickinson.dumbcrawler.api;

public interface CrawlingResultHandler {
    void initialize(CrawlingContext context);

    void handleCrawlingResult(CrawlingResult result);

    void destroy();
}
