package com.thordickinson.webcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Optional;

@SpringBootApplication
public class WebCrawlerApplication {

	private static final Logger logger = LoggerFactory.getLogger(WebCrawlerApplication.class);
	@Autowired
	private CrawlerService crawler;
	@Value("${crawler.job:#{null}}")
	private Optional<String> job;

	public static void main(String[] args) {
		SpringApplication.run(WebCrawlerApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startCrawling() {
		if(job.isEmpty()) {
			logger.warn("No job to execute");
			return;
		}
		try{
			crawler.crawl(job.get());
		} catch (Exception ex){
			logger.error("Error crawling data", ex);
		}
	}
}
