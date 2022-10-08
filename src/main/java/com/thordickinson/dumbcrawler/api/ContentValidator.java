package com.thordickinson.dumbcrawler.api;

import org.jsoup.nodes.Document;

public interface ContentValidator extends CrawlingComponent {
    boolean validateContent(String url, Document document);
}
