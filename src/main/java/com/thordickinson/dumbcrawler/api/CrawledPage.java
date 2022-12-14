package com.thordickinson.dumbcrawler.api;
import java.util.Map;
import java.util.Optional;

public record CrawledPage(String originalUrl, Optional<String> transformedUrl, int resultCode,
Optional<String> contentType, Optional<String> content, Map<String, Integer> counters) {
}
