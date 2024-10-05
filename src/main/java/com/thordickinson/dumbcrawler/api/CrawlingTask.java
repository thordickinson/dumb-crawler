package com.thordickinson.dumbcrawler.api;

public record CrawlingTask(String taskId, String urlId, String url, String[] tags, int priority) {
    public CrawlingTask withUrl(String url){
        return new CrawlingTask(taskId, urlId, url, tags, priority);
    }
}
