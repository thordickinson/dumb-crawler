package com.thordickinson.dumbcrawler.api;

public interface CrawlingComponent {

    void initialize(CrawlingContext context);
    default void destroy() {};
}
