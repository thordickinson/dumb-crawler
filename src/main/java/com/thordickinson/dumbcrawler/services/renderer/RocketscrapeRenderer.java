package com.thordickinson.dumbcrawler.services.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketscrapeRenderer extends SimpleHttpRenderer {

    private static final Logger logger = LoggerFactory.getLogger(RocketscrapeRenderer.class);
    private String apiKey = null;

    private String getApiKey(){
        if(apiKey == null){
            apiKey = System.getenv("ROCKETSCRAPE_API_KEY");
        }
        if(apiKey.isEmpty() || apiKey.length() < 10){
            throw new IllegalStateException("Please provide a valid ROCKETSCRAPE_API_KEY environment variable!");
        }
        return apiKey;
    }
    @Override
    public HtmlRenderResponse renderHtml(String url) {
        logger.debug("Rendering using rocketscrapper: {}", url);
        String rocketScrapeEndpoint = "https://api.rocketscrape.com/";
        var transformedUrl = rocketScrapeEndpoint + "?apiKey=" + getApiKey() + "&url=" + url;
        return super.renderHtml(transformedUrl);
    }
}
