package com.thordickinson.dumbcrawler.api;

public record CrawlingTask(String taskId, String urlId, String url, String[] tags, int attempt, int priority) {
    public CrawlingTask withTags(String[] tags){
        return new CrawlingTask(taskId,  urlId, url, tags, attempt, priority);
    }
}
