package com.thordickinson.dumbcrawler.services;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.thordickinson.dumbcrawler.api.*;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.services.renderer.ContentRenderer;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlingTaskCallable implements Callable<CrawlingResult> {


    @Getter
    private final CrawlingTask task;
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTaskCallable.class);
    private final ContentRenderer htmlRenderer;
    private final ContentValidator contentValidator;

    public CrawlingTaskCallable(CrawlingTask task, ContentRenderer renderer, ContentValidator contentValidator) {
        this.task = task;
        this.contentValidator = contentValidator;
        this.htmlRenderer = renderer;
    }

    @Override
    public CrawlingResult call() {
        long startedAt = System.currentTimeMillis();
        var html = htmlRenderer.renderPage(task);
        if (StringUtils.isBlank(html)) {
            throw new CrawlingException(task, "EMPTY_RESPONSE_BODY", true);
        }
        try {
            var document = parseHtml(html);
            contentValidator.validatePageContent(document);
            var links = getLinks(document);
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", task.url());
            }
            long endedAt = System.currentTimeMillis();
            return new CrawlingResult(task, html, links, startedAt, endedAt);
        }catch(CrawlingException ex){
            writeDebugFile(html);
            throw ex;
        }
    }

    private Document parseHtml(String html){
        try {
            return Jsoup.parse(html);
        } catch (Exception e) {
            throw new CrawlingException(task, "ERROR_PARSING_JSON", e.getMessage(), true, e);
        }
    }

    private void writeDebugFile(String content){
    }

    private Set<String> getLinks(Document document) { 
        return document.select("a[href]")
               .stream()
               .filter(l -> !"nofollow".equals(l.attr("rel")))
               .map(l -> l.absUrl("href"))
               .filter(StringUtils::isNotBlank)
               .collect(Collectors.toSet());
    }

}
