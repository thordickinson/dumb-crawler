package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.URLTransformer;
import com.thordickinson.dumbcrawler.util.ConfigurationSupport;
import com.thordickinson.dumbcrawler.util.Counters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class RocketScrapeURLTransformer extends AbstractCrawlingComponent implements URLTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RocketScrapeURLTransformer.class);
    private Optional<String> apiKey;

    public RocketScrapeURLTransformer(){
        super("rocketScrape");
    }

    @Override
    public String transform(String url, Map<String, String> renderingHints, Counters counters) {
        if(isDisabled()) return url;
        if(!evaluateUrlFilter(url)){
            logger.trace("Ignoring url: {}", url);
            return url;
        }
        if(apiKey.isEmpty()){
            logger.warn("RocketScrape transformer is disabled because no api key was set");
            return url;
        }
        var forceRender = "true".equalsIgnoreCase(renderingHints.get("rocketScrape.renderContent"));
        if(forceRender){
            counters.increase("rocketScrape.forceRender");
            logger.debug("Forcing render over {}", url);
        }
        var credits = forceRender? 10 : 1;
        counters.increase("rocketScrape.spentCredits", credits);
        var renderParam = String.valueOf(forceRender);
        logger.debug("Transforming url: {}", url);
        return "https://api.rocketscrape.com/?apiKey=%s&render=%s&url=%s".formatted(apiKey.get(), renderParam, url);
    }

    @Override
    protected void loadConfigurations(CrawlingContext context) {
        apiKey = getConfiguration("apiKey").map(Any::toString);
    }

}
