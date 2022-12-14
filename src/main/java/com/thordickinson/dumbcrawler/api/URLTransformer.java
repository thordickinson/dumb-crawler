package com.thordickinson.dumbcrawler.api;

import com.thordickinson.dumbcrawler.util.Counters;

import java.util.Map;

public interface URLTransformer extends CrawlingComponent{
    String transform(String url, Map<String, String> hints, Counters counters);
}
