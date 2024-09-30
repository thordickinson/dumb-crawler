package com.thordickinson.dumbcrawler.services.renderer;

import org.springframework.stereotype.Service;

@Service
public class RocketscrapeRenderer extends SimpleHttpRenderer {

    
    private String rocketScrapeEndpoint = "https://api.rocketscrape.com/";
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
        var transformedUrl = rocketScrapeEndpoint + "?apiKey=" + getApiKey() + "&url=" + url;
        return super.renderHtml(transformedUrl);
    }
}
