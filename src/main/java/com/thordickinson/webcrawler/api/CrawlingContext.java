package com.thordickinson.webcrawler.api;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.filter.FilterPage;
import com.thordickinson.webcrawler.filter.Decider;
import com.thordickinson.webcrawler.filter.Decision;
import com.thordickinson.webcrawler.filter.FilterEvaluator;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;

import java.nio.file.Path;
import java.util.List;

public class CrawlingContext {
    private final String executionId;
    private final Any jobConfig;
    private final Path jobDir;
    private final CrawlConfig config;
    private final FilterEvaluator evaluator;

    public CrawlingContext(Any jobConfig, Path jobDir, CrawlConfig config, FilterEvaluator evaluator) {
        this.jobConfig = jobConfig;
        this.evaluator = evaluator;
        this.executionId = String.valueOf(System.currentTimeMillis());
        this.jobDir = jobDir;
        this.config = config;
    }

    public Decision evaluateFilter(List<Decider> deciders, Page page){
        return evaluator.evaluate(deciders, FilterPage.fromPage(page));
    }

    public Decision evaluateFilter(List<Decider> deciders, WebURL page){
        return evaluator.evaluate(deciders, FilterPage.fromUri(page));
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
}
