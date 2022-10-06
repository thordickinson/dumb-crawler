package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;
import com.thordickinson.dumbcrawler.util.Misc;

public class TimedTaskKiller extends AbstractCrawlingComponent implements CrawlingResultHandler {

    private long timeout;

    public TimedTaskKiller() {
        super("timedTaskKiller");
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {
        checkConditions();
    }

    @Override
    public void initialize(CrawlingContext context) {
        timeout = context.getConfig("timeout").map(Any::toString).map(Misc::parsePeriod).orElse(-1L);
    }

    private void checkConditions() {
        var ctx = getContext();
        if (ctx.isStopRequested())
            return;
        var stopAfter = ctx.getStartedAt() + timeout;
        if(System.currentTimeMillis() > stopAfter){
            ctx.stopCrawling();
        }
    }
    
}
