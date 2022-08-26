package com.thordickinson.webcrawler.rocketscrape;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.URLTransformer;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RocketScrapeURLTransformer implements URLTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RocketScrapeURLTransformer.class);
    private final ConfigurationSupport configuration = new ConfigurationSupport("rocketScrape");

    private Optional<String> apiKey;

    private Optional<String> getApiKey(CrawlingContext context){
        if(apiKey == null){
            apiKey = configuration.getConfig("apiKey", context).map(Any::toString);
        }
        return apiKey;
    }

    @Override
    public String transform(String webUrl, CrawlingContext context) {
        if (!configuration.shouldProceed(webUrl, context)) return webUrl;
        var apiKey = getApiKey(context);
        if(!apiKey.isPresent()){
            logger.warn("RocketScrape interceptor is not enabled because apiKey was not set. Use config.apiKey");
            return webUrl;
        }
        logger.debug("Using rocketscrappe with: {}", webUrl);
        context.increaseCounter("rocketScrapeUrls");
        return "https://api.rocketscrape.com/?apiKey=%s&url=%s".formatted(apiKey.get(), webUrl);
    }
}
