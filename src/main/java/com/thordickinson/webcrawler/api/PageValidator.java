package com.thordickinson.webcrawler.api;

public interface PageValidator {
    PageValidationResult validatePage(PageFetch result, CrawlingContext context);
}
