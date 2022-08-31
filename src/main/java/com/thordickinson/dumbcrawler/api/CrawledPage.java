package com.thordickinson.dumbcrawler.api;

import java.util.Optional;

public record CrawledPage(String url, int resultCode, Optional<String> contentType, Optional<String> content) {
}
