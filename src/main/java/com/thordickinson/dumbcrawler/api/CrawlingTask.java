package com.thordickinson.dumbcrawler.api;

public record CrawlingTask(String taskId, String urlId, String url, String[] tags, int attempt, int priority) {
}
