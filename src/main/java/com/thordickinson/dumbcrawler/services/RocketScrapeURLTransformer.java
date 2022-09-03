package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.URLTransformer;
import com.thordickinson.dumbcrawler.util.ConfigurationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RocketScrapeURLTransformer implements URLTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RocketScrapeURLTransformer.class);
    private ConfigurationSupport config;
    private Optional<String> apiKey;

    @Override
    public String transform(String url) {
        if(apiKey.isEmpty()){
            logger.warn("RocketScrape transformer is disabled because no api key was set");
            return url;
        }
        logger.debug("Transforming url: {}", url);
        return "https://api.rocketscrape.com/?apiKey=%s&url=%s".formatted(apiKey.get(), url);
    }

    @Override
    public void initialize(CrawlingContext context) {
        config = new ConfigurationSupport("rocketScrape", context);
        apiKey = config.getConfig("apiKey").map(Any::toString);
    }
}
