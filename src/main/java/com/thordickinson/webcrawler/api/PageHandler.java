package com.thordickinson.webcrawler.api;

import edu.uci.ics.crawler4j.crawler.Page;

public interface PageHandler {
    void handlePage(Page page, CrawlingContext context) throws Exception;
}
