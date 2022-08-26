package com.thordickinson.webcrawler.api;

public interface URLTransformer {

    String transform(String url, CrawlingContext context);

}
