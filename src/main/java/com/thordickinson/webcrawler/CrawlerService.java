package com.thordickinson.webcrawler;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import static com.thordickinson.webcrawler.util.JsonUtil.*;

import com.thordickinson.webcrawler.api.*;
import com.thordickinson.webcrawler.filter.FilterEvaluator;
import com.thordickinson.webcrawler.util.ConfigurationException;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);
    @Autowired(required = false)
    private List<PrefetchInterceptor> prefetchInterceptors = Collections.emptyList();
    @Autowired(required = false)
    private List<PageValidator> pageValidators = Collections.emptyList();
    @Autowired(required = false)
    private List<PageHandler> pageHandlers = Collections.emptyList();
    @Autowired(required = false)
    private List<URLTransformer> urlTransformers = Collections.emptyList();
    @Autowired
    private FilterEvaluator evaluator;

    private CrawlingContext crawlingContext;

    public void crawl(String job) throws Exception {
        var dataDir = "./data/jobs/%s".formatted(job);
        var jobConfig = loadJob(dataDir);
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(dataDir + "/db");
        crawlingContext = new CrawlingContext(jobConfig, Path.of(dataDir), config, evaluator);

        int numberOfCrawlers = get(jobConfig, "threadCount").map(Any::toInt).orElseGet(() -> 3);

        // Instantiate the controller for this crawl.
        PageFetcher pageFetcher = new GenericFetcher(crawlingContext, prefetchInterceptors, pageValidators, urlTransformers);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        var seeds = getSeeds(jobConfig);
        if (seeds.isEmpty()) throw new IllegalArgumentException("Crawl job must have at least one seed");
        getSeeds(jobConfig).forEach(s -> {
            logger.info("Adding url seed: {}", s);
            controller.addSeed(s);
        });

        // The factory which creates instances of crawlers.
        CrawlController.WebCrawlerFactory<WebCrawler> factory = () -> new DefaultCrawler(crawlingContext, pageHandlers);

        // Start the crawl. This is a blocking operation, meaning that your code
        // will reach the line after this only when crawling is finished.
        controller.start(factory, numberOfCrawlers);
    }

    private Set<String> getSeeds(Any jobConfig) {
        return get(jobConfig,"seeds").map(Any::asList).map(l -> l.stream().map(Any::toString).collect(Collectors.toSet()))
                .orElseThrow(() -> new ConfigurationException("Seeds are required"));
    }

    private Any loadJob(String dataDir){
        var configFile = dataDir + "/job.json";
        try {
            return JsonIterator.deserialize(Files.readAllBytes(Path.of(configFile)));
        } catch (IOException ex) {
            throw new ConfigurationException("Error reading config file: " + configFile, ex);
        }
    }

    @PreDestroy
    void preDestroy(){
        logger.info("Counters");
        if(crawlingContext != null){
            Set<String> counters = crawlingContext.getCounterKeys();
            for(var counter : counters){
                logger.info("{} : {}", counter, crawlingContext.getCounter(counter));
            }
        }
    }
}
