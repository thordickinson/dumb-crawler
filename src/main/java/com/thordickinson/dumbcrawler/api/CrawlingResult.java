package com.thordickinson.dumbcrawler.api;

import java.util.Optional;
import java.util.Set;

public record CrawlingResult(CrawledPage page, Set<String> links,
                long startedAt, long endedAt,
                Optional<Exception> error) {
}
