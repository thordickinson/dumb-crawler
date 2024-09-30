package com.thordickinson.dumbcrawler.api;

import java.util.Optional;
import java.util.Set;

public record CrawlingResult(CrawlingTask task, CrawledPage page, Set<String> links,
                long startedAt, long endedAt,
                Optional<Exception> error) {
}
