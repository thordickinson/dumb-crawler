package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.services.storage.WarcStorageManager;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.util.Misc;
import org.springframework.stereotype.Service;

@Service
public class TaskKiller extends AbstractCrawlingComponent {

    private long timeout;
    public TaskKiller(){
        super("taskKiller");
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        var timeout = context.getConfig("taskKiller.timeout").map(Any::toString).orElse("10m");
        var parsed = Misc.parsePeriod(timeout);
        this.timeout = parsed == null? 60 * 60 * 10 : parsed;
    }

    public boolean shouldStop(CrawlingSessionContext sessionContext){
        if(sessionContext.isStopRequested()){
            return true;
        }
        var lastNewPageSavedAt = sessionContext.getVariable(WarcStorageManager.LAST_NEW_PAGE_SAVED_AT_KEY, System.currentTimeMillis());
        if(System.currentTimeMillis() - lastNewPageSavedAt > timeout) {
            logger.warn("Stopping crawler after no new pages were saved: {}ms", timeout);
            return true;
        }
        return false;
    }
}
