package com.thordickinson.dumbcrawler.services;

import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.services.renderer.ContentRenderer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class CrawlingTaskCallable implements Callable<CrawlingResult> {


    @Getter
    private final CrawlingTask task;
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTaskCallable.class);
    private final ContentRenderer htmlRenderer;
    private final ContentValidator contentValidator;
    private final Path sessionFolder;

    public CrawlingTaskCallable(CrawlingTask task, ContentRenderer renderer,
                                ContentValidator contentValidator, Path sessionFolder) {
        this.task = task;
        this.contentValidator = contentValidator;
        this.htmlRenderer = renderer;
        this.sessionFolder = sessionFolder;
    }

    @Override
    public CrawlingResult call() {
        long startedAt = System.currentTimeMillis();
        logger.info("Processing url: {}", task.url());
        var html = htmlRenderer.renderPage(task);
        if (StringUtils.isBlank(html)) {
            throw new CrawlingException(task, "EMPTY_RESPONSE_BODY", true);
        }
        try {
            var document = parseHtml(html);
            contentValidator.validatePageContent(task, document);
            var links = getLinks(document);
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", task.url());
            }
            long endedAt = System.currentTimeMillis();
            return new CrawlingResult(task, html, links, startedAt, endedAt);
        }catch(CrawlingException ex){
            writeDebugFile(ex, html);
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

    private void writeDebugFile(CrawlingException ex, String content){
        String errorCode = ex.getErrorCode();
        String fileName = ex.getTask().taskId() + ".html";
        var path = sessionFolder.resolve("debug").resolve(errorCode).resolve(fileName);
        try {
            path.getParent().toFile().mkdirs();
            Files.writeString(path, content);
            logger.warn("Debug file stored at {}", path);
        }catch (IOException ioex){
            logger.error("Error writing file", ioex);
        }
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
