package com.thordickinson.dumbcrawler.api;

public interface CrawlingComponent {

    void initialize(CrawlingSessionContext context);
    default void destroy() {};
}
