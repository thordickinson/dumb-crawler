package com.thordickinson.dumbcrawler.api;

import java.util.Set;


public record CrawlingResult(CrawlingTask task,
                             String content,
                             Set<String> links,
                             long startedAt,
                             long endedAt) {
}