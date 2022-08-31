package com.thordickinson.dumbcrawler;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.thordickinson.dumbcrawler.services.DumbCrawler;

@SpringBootApplication
public class DumbCrawlerApplication {

    @Autowired
    private DumbCrawler crawler;
    @Value("${crawler.jobId:#{null}}")
    private Optional<String> job;
    @Value("${crawler.executionId:#{null}}")
    private Optional<String> executionId;
    private static final Logger logger = LoggerFactory.getLogger(DumbCrawlerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DumbCrawlerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        if (job.isEmpty()) {
            logger.warn("No job to execute");
            return;
        }
        crawler.start(job.get(), executionId);
    }
}
