package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;

public class StorageManager extends AbstractCrawlingComponent implements CrawlingResultHandler {

    public StorageManager() {
        super("storage");
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {

    }
}
