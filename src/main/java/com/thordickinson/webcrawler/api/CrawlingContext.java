package com.thordickinson.webcrawler.api;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.filter.FilterPage;
import com.thordickinson.webcrawler.filter.Decider;
import com.thordickinson.webcrawler.filter.Decision;
import com.thordickinson.webcrawler.filter.FilterEvaluator;
import com.thordickinson.webcrawler.util.MutableInteger;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CrawlingContext {
    private final String executionId;
    private final Any jobConfig;
    private final Path jobDir;
    private final CrawlConfig config;
    private final Path executionDir;
    private final FilterEvaluator evaluator;
    private final Map<String, MutableInteger> counters = new ConcurrentHashMap<>();
    private final Long createdAt = System.currentTimeMillis();

    private static final Long PRINT_TIMERS_TIMEOUT = 60_000L;
    private static final Logger counterLogger = LoggerFactory.getLogger(CrawlingContext.class.getName() + ".counters");
    private Long nextDisplay = System.currentTimeMillis();

    public CrawlingContext(String executionId, Any jobConfig, Path jobDir, CrawlConfig config,
            FilterEvaluator evaluator) {
        this.jobConfig = jobConfig;
        this.evaluator = evaluator;
        this.executionId = executionId;
        this.jobDir = jobDir;
        this.config = config;
        executionDir = getJobDir().resolve("executions").resolve(executionId);
    }

    public Decision evaluateFilter(List<Decider> deciders, Page page) {
        return evaluator.evaluate(deciders, FilterPage.fromPage(page));
    }

    public Decision evaluateFilter(List<Decider> deciders, WebURL page) {
        return evaluator.evaluate(deciders, FilterPage.fromUri(page));
    }

    public Decision evaluateFilter(List<Decider> deciders, String url) {
        return evaluator.evaluate(deciders, FilterPage.fromUri(url));
    }

    public int increaseCounter(String key) {
        MutableInteger initValue = new MutableInteger(1);
        MutableInteger oldValue = counters.put(key, initValue);

        if (oldValue != null) {
            initValue.set(oldValue.get() + 1);
        }
        printCounters();
        return initValue.get();
    }

    public int getCounter(String key) {
        return counters.getOrDefault(key, new MutableInteger(0)).get();
    }

    public void printCounters() {

        var now = System.currentTimeMillis();
        if (nextDisplay > now)
            return;
        nextDisplay = now + PRINT_TIMERS_TIMEOUT;

        StringBuilder message = new StringBuilder("Time since start: ").append(Math.floor((now - createdAt) / 1000))
                .append(" secs.\n");
        if (counters.isEmpty()) {
            message.append("No counters to display");
        }
        for (var entry : counters.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue().get()).append("\n");
        }
        counterLogger.info(message.toString());
    }

    public Set<String> getCounterKeys() {
        return counters.keySet();
    }

    public String getExecutionId() {
        return executionId;
    }

    public CrawlConfig getConfig() {
        return config;
    }

    public Any getJobConfig() {
        return jobConfig;
    }

    public Path getJobDir() {
        return jobDir;
    }

    public Path getExecutionDir() {
        return executionDir;
    }
}
