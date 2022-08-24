package com.thordickinson.webcrawler.api;

import com.jsoniter.any.Any;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;

import java.nio.file.Path;

public class CrawlingContext {
    private final Any jobConfig;
    private final Path jobDir;
    private final CrawlConfig config;

    public CrawlingContext(Any jobConfig, Path jobDir, CrawlConfig config) {
        this.jobConfig = jobConfig;
        this.jobDir = jobDir;
        this.config = config;
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
