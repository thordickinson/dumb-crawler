package com.thordickinson.dumbcrawler.api;

import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.Map;

public interface ContentValidator extends CrawlingComponent {

    ValidationResult VALID = new ValidationResult(true, Collections.emptyMap());
    ValidationResult validateContent(String url, Document document);

    record ValidationResult(boolean pageValid, Map<String, String> renderingHints){}

}
