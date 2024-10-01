package com.thordickinson.dumbcrawler.api;
import java.util.Map;
import java.util.Optional;

//TODO: eliminar originalUrl, transformedUrl
public record CrawledPage(String originalUrl, Optional<String> transformedUrl, int resultCode,
Optional<String> contentType, Optional<String> content, Map<String, Integer> counters) {
}
