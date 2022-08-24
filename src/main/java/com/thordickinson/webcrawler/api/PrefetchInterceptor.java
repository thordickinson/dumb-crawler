package com.thordickinson.webcrawler.api;

import edu.uci.ics.crawler4j.url.WebURL;

public interface PrefetchInterceptor {
    void intercept(WebURL url, CrawlingContext context);
}
