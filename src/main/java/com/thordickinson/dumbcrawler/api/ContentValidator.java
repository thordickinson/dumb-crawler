package com.thordickinson.dumbcrawler.api;

import java.util.Optional;

import org.jsoup.nodes.Document;

public interface ContentValidator extends CrawlingComponent {
    boolean validateContent(Document document);
}
