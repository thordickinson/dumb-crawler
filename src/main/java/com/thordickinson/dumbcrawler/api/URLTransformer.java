package com.thordickinson.dumbcrawler.api;

public interface URLTransformer extends CrawlingComponent{
    String transform(String url);
}
