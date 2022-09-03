package com.thordickinson.dumbcrawler.api;

public interface CrawlingResultHandler  extends  CrawlingComponent{
    void handleCrawlingResult(CrawlingResult result);
}
