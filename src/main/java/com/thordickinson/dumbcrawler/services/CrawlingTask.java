package com.thordickinson.dumbcrawler.services;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thordickinson.dumbcrawler.api.ContentValidator;
import com.thordickinson.dumbcrawler.api.CrawledPage;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.util.Counters;
import com.thordickinson.dumbcrawler.util.InvalidDocumentException;

public class CrawlingTask implements Callable<CrawlingResult> {

    private final String originalUrl;
    private final String transformedUrl;
    private static Optional<String> HTML = Optional.of("text/html");
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTask.class);
    private int timeout = 30 * 1000;
    private int maxRetryCount = 3;
    private int maxValidationCount = 5;
    private final List<ContentValidator> contentValidators;

    public CrawlingTask(String originalUrl, String transformedUrl, List<ContentValidator> contentValidators) {
        this.originalUrl = originalUrl;
        this.transformedUrl = transformedUrl;
        this.contentValidators = contentValidators;
    }

    @Override
    public CrawlingResult call() {
        long startedAt = System.currentTimeMillis();
        HashSet<String> links = new HashSet<>();
        CrawledPage page = null;
        Exception exception = null;
        try {
            page = crawl(links);
        } catch (IOException ex) {
            logger.debug("Error while fetching page", ex);
            page = createErrorPage(originalUrl);
            exception = ex;
        } catch(InvalidDocumentException ex){
            logger.debug("Invalid document detected");
            page = createErrorPage(originalUrl);
            exception = ex;
        }

        long endedAt = System.currentTimeMillis();
        return new CrawlingResult(transformedUrl, page, links, startedAt, endedAt, Optional.ofNullable(exception));
    }

    private CrawledPage crawl(Collection<String> linkContainer) throws IOException {
        var attempt = 0;
        IOException lastException = null;
        do {
            attempt++;
            try {
                return doCrawl(linkContainer);
            } catch (IOException ex) {
                lastException = ex;
            }
        } while (attempt < maxRetryCount);
        throw lastException;
    }

    private CrawledPage createErrorPage(String originalUrl) {
        return new CrawledPage(originalUrl, -1, Optional.empty(), Optional.empty(), Collections.emptyMap());
    }


    private Document getDocument(Counters counters) throws IOException{
        int attempt = 0;
        boolean invalid = true;
        Document document = null;
        do{
            counters.increase("GetRequests");
            document = Jsoup.connect(transformedUrl).timeout(timeout).get();
            invalid = false;
            for(ContentValidator validator : contentValidators){
                if(!validator.validateContent(originalUrl, document)){
                    counters.increase("invalidContentDetection");
                    logger.debug("Invalid content detected on url: {}", originalUrl);
                    invalid = true;
                }
            }
            attempt++;
        }while(invalid && attempt < maxValidationCount);
        if(attempt >= maxValidationCount){
            counters.increase("invalidContentDiscarded");
            throw new InvalidDocumentException("Document is invalid");
        }
        return document;
    }

    private CrawledPage doCrawl(Collection<String> linkContainer) throws IOException {
        var counters = new Counters();
        try {
            var document = getDocument(counters);
            var html = document.html();
            if (StringUtils.isBlank(html)) {
                logger.error("Parsed html is blank, {}", originalUrl);
            }
            var links = document.select("a")
                    .stream()
                    .filter(l -> !"nofollow".equals(l.attr("rel")))
                    .map(l -> l.absUrl("href"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", originalUrl);
            }
            linkContainer.addAll(links);
            return new CrawledPage(originalUrl, 200, HTML, Optional.of(html), counters.toMap());
        } catch (HttpStatusException ex) {
            // TODO: Handle 500 errors
            logger.debug("Error getting url {}", originalUrl, ex);
            counters.increase("InternalServerErrorReceived");
            return new CrawledPage(originalUrl, ex.getStatusCode(), Optional.empty(), Optional.empty(), counters.toMap());
        } catch (UnsupportedMimeTypeException ex) {
            logger.debug("Error getting url {}", originalUrl, ex);
            counters.increase("UnsupportedMimeTimeErrorReceived");
            return new CrawledPage(originalUrl, 200, Optional.of(ex.getMimeType()), Optional.empty(), counters.toMap());
        }
    }
}
