package com.thordickinson.dumbcrawler.services.renderer;


import com.thordickinson.dumbcrawler.api.CrawlingTask;

public interface HtmlRenderer {

    String renderHtml(CrawlingTask url);
}
