package com.thordickinson.dumbcrawler.services;

import java.time.Duration;

import com.thordickinson.dumbcrawler.api.*;
import com.thordickinson.dumbcrawler.util.ConfigurableCrawlingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.util.HumanReadable;
import com.thordickinson.dumbcrawler.util.Misc;

public class TaskKiller extends ConfigurableCrawlingComponent implements CrawlingResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(TaskKiller.class);

    private long timeout;
    private long lastValidPageTimestamp = -1;
    private int rejectedPageCount = 0;
    private int maxRejectedPageCount = -1;

    public TaskKiller() {
        super("taskKiller");
    }

    @Override
    public void initialize(CrawlingContext context) {
        timeout = context.getConfig("timeout").map(Any::toString).map(Misc::parsePeriod).orElse(-1L);
        maxRejectedPageCount = context.getConfig("maxRejectedPageCount").map(Any::toInt).orElse(-1);
        lastValidPageTimestamp = -1L;
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {
        if (evaluateUrlFilter(result.page().originalUrl())) {
            rejectedPageCount = 0;
            lastValidPageTimestamp = System.currentTimeMillis();
        } else {
            rejectedPageCount++;
        }

        if (maxRejectedPageCount > 0)
            setCounter("rejectedPages", rejectedPageCount);
        if (timeout > 0 && lastValidPageTimestamp > 0) {
            setCounter("lastValidPage",
                    HumanReadable
                            .formatDuration(Duration.ofMillis(System.currentTimeMillis() - lastValidPageTimestamp)));
        }
        checkConditions();
    }

    private void checkConditions() {
        var ctx = getContext();
        if (ctx.isStopRequested())
            return;
        if (maxRejectedPageCount > 0 && rejectedPageCount >= maxRejectedPageCount) {

            logger.info("Max rejected page count reached [{}] > [{}]. Stopping crawler.", rejectedPageCount,
                    maxRejectedPageCount);
            getContext().stopCrawling();
        }
        if (timeout > 0) {
            long expiresAt = lastValidPageTimestamp + timeout;
            long now = System.currentTimeMillis();
            if (expiresAt < now) {
                var timeSinceLast = now - lastValidPageTimestamp;
                logger.info("Last valid page was {} ago",
                        HumanReadable.formatDuration(Duration.ofMillis(timeSinceLast)));
                getContext().stopCrawling();
            }
        }
    }

}
