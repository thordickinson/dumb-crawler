package com.thordickinson.webcrawler.rocketscrape;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.url.WebURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

public class RocketScrapePageFetcher extends PageFetcher {


    private static final Logger logger = LoggerFactory.getLogger(RocketScrapePageFetcher.class);
    private String apiKey;
    private Optional<String> whitelist;
    private Optional<String> blacklist;

    public RocketScrapePageFetcher(CrawlConfig config, String apiKey, Optional<String> whitelist, Optional<String> blacklist) {
        super(config);
        this.apiKey = apiKey;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    private boolean useRocketScrape(String url){
        var whitelisted = whitelist.map( f -> url.matches(f)).orElseGet(() -> true);
        var blacklisted = blacklist.map(f -> url.matches(f)).orElseGet(() -> false);
        if(!whitelisted || blacklisted) return false;
        return true;
    }

    @Override
    public PageFetchResult fetchPage(WebURL webUrl) throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        var url = webUrl.getURL();
        if(useRocketScrape(url)){
            String fullUrl = "https://api.rocketscrape.com/?apiKey=%s&url=%s".formatted(apiKey, url);
            logger.debug("Using rocketscrapper with: {}", url);
            webUrl.setURL(fullUrl);
        }
        return super.fetchPage(webUrl);
    }
}
