package com.thordickinson.webcrawler.api;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;

public interface PageFilter {
    void shouldVisit(Page referringPage, WebURL url);
}
