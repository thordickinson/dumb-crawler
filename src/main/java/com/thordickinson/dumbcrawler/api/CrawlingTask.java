package com.thordickinson.dumbcrawler.api;

public record CrawlingTask(String id, String url, String[] tags) {
}
