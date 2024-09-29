package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;

public class WarcStorage extends AbstractCrawlingComponent implements CrawlingResultHandler {

    public WarcStorage(String componentKey) {
        super(componentKey);
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {

    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
