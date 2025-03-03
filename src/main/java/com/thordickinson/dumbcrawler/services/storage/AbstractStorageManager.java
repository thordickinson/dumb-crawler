package com.thordickinson.dumbcrawler.services.storage;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractStorageManager  extends AbstractCrawlingComponent {

    private Set<String> allowedTags = Collections.emptySet();

    public AbstractStorageManager(String key) {
        super(key);
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        allowedTags = context.getConfig("storage.includedTags")
                .map(Any::asList).map(l -> (Set<String>) new HashSet<String>(l.stream().map(Object::toString).toList()))
                .orElseGet(Collections::emptySet);
    }

    private boolean shouldStore(CrawlingResult result, CrawlingSessionContext sessionContext){
        for(var tag : result.task().tags()){
            if(allowedTags.contains(tag)){
                return true;
            }
        }
        return false;
    }

    public void storeResult(CrawlingResult result, CrawlingSessionContext sessionContext) {
        if(!shouldStore(result, sessionContext)){
            logger.debug("Ignoring url: {}", result.task().url());
            sessionContext.increaseCounter("UNSAVED_PAGES");
            return;
        }
        try {
            doStoreResult(result, sessionContext);
            sessionContext.increaseCounter("SAVED_PAGES");
        }catch (IOException ex){
            logger.error("Error storing data for {} - Ignoring", result.task().url(), ex);
            sessionContext.increaseCounter("STORE_ERROR");
        }
    }

    protected abstract void doStoreResult(CrawlingResult result, CrawlingSessionContext sessionContext) throws IOException;

}
