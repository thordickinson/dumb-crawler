package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TimedTaskKiller extends AbstractCrawlingComponent implements CrawlingResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(TimedTaskKiller.class);
    private Optional<Long> timeoutMillis = Optional.empty();
    private Optional<String> timeout = Optional.empty();

    public TimedTaskKiller() {
        super("timedTaskKiller");
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {
        checkConditions();
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        timeout = getConfiguration("timeout").map(Any::toString);
        timeoutMillis = timeout.map(Misc::parsePeriod);
    }

    private void checkConditions() {
        var ctx = getContext();
        if (ctx.isStopRequested() || !timeoutMillis.isPresent())
            return;
        var stopAfter = ctx.getStartedAt() + timeoutMillis.get();
        if(System.currentTimeMillis() > stopAfter){
            logger.warn("Stopping crawling task after {}", timeout.get());
            ctx.stopCrawling();
        }
    }
    
}
