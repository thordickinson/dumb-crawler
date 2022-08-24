package com.thordickinson.webcrawler.api;

import edu.uci.ics.crawler4j.fetcher.PageFetchResult;

public record PageFetch(PageFetchResult result, int attemptCount) {
}
