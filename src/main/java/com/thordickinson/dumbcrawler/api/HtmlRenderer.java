package com.thordickinson.dumbcrawler.api;

public interface HtmlRenderer extends CrawlingComponent {

    String renderHtml(String url);
}
