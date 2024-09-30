package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

import java.util.Collection;

import org.springframework.stereotype.Service;


@Service
public class StorageManager extends AbstractCrawlingComponent {

    public StorageManager() {
        super("storage");
    }


    public void storeResults(Collection<CrawlingResult> results){ 
    }


    @Override
    public void initialize(CrawlingSessionContext context) {
    }
}
