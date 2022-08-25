package com.thordickinson.webcrawler.rocketscrape;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.PrefetchInterceptor;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.url.WebURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RocketScrapePagePrefetchInterceptor implements PrefetchInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RocketScrapePagePrefetchInterceptor.class);
    private final ConfigurationSupport configuration = new ConfigurationSupport("rocketScrape");

    private Optional<String> apiKey;

    private Optional<String> getApiKey(CrawlingContext context){
        if(apiKey == null){
            apiKey = configuration.getConfig("apiKey", context).map(Any::toString);
        }
        return apiKey;
    }

    @Override
    public void intercept(WebURL webUrl, CrawlingContext context) {
        if (!configuration.shouldProceed(webUrl, context)) return;
        var apiKey = getApiKey(context);
        if(!apiKey.isPresent()){
            logger.warn("RocketScrape interceptor is not enabled because apiKey was not set. Use config.apiKey");
            return;
        }
        String url = webUrl.getURL();
        String fullUrl = "https://api.rocketscrape.com/?apiKey=%s&url=%s".formatted(apiKey.get(), url);
        logger.debug("Using rocketscrapper with: {}", url);
        webUrl.setURL(fullUrl);
    }
}
