package com.thordickinson.dumbcrawler.services;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.thordickinson.dumbcrawler.api.*;
import com.thordickinson.dumbcrawler.services.renderer.SimpleHttpRenderer;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thordickinson.dumbcrawler.util.Counters;
import com.thordickinson.dumbcrawler.util.InvalidDocumentException;

public class CrawlingTask implements Callable<CrawlingResult> {

    private final String url;
    private static Optional<String> HTML = Optional.of("text/html");
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTask.class);
    private final List<ContentValidator> contentValidators;
    private final HtmlRenderer htmlRenderer;

    public CrawlingTask(String url, List<ContentValidator> contentValidators) {
        this.url = url;
        this.contentValidators = contentValidators;
        this.htmlRenderer = new SimpleHttpRenderer();
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
            page = createErrorPage(url);
            exception = ex;
        } catch (InvalidDocumentException ex) {
            logger.debug("Invalid document detected");
            page = createErrorPage(url);
            exception = ex;
        }

        long endedAt = System.currentTimeMillis();
        return new CrawlingResult(page, links, startedAt, endedAt, Optional.ofNullable(exception));
    }

    private CrawledPage crawl(Collection<String> linkContainer) throws IOException {
        var attempt = 0;
        IOException lastException = null;
        int maxRetryCount = 3;
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
        return new CrawledPage(originalUrl, Optional.empty(), -1, Optional.empty(), Optional.empty(), Collections.emptyMap());
    }

    private String renderHtml(String url){
        return "";
    }

    private DownloadedDocument getDocument(Counters counters) throws IOException {
        int attempt = 0;
        boolean invalid = true;
        Document document = null;
        int maxValidationCount = 3;
        do {
            counters.increase("GetRequests");
            String html = renderHtml(url);
            document = Jsoup.parse(html);
            invalid = false;
            for (ContentValidator validator : contentValidators) {
                var validationResult = validator.validateContent(url, document);
                if (!validationResult.pageValid()) {
                    counters.increase("invalidContentDetection");
                    logger.debug("Invalid content detected on url: {}", url);
                    invalid = true;
                }
            }
            attempt++;
        } while (invalid && attempt < maxValidationCount);
        if (attempt >= maxValidationCount) {
            counters.increase("invalidContentDiscarded");
            throw new InvalidDocumentException("Document is invalid");
        }
        return new DownloadedDocument(url, document);
    }

    private CrawledPage doCrawl(Collection<String> linkContainer) throws IOException {
        var counters = new Counters();
        try {
            var downloadedDocument = getDocument(counters);
            var document = downloadedDocument.document;
            var html = document.html();
            if (StringUtils.isBlank(html)) {
                logger.error("Parsed html is blank, {}", url);
            }
            var links = document.select("a")
                    .stream()
                    .filter(l -> !"nofollow".equals(l.attr("rel")))
                    .map(l -> l.absUrl("href"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", url);
            }
            linkContainer.addAll(links);
            var transformedUrl = Objects.equals(url, downloadedDocument.fetchedUrl) ? Optional.<String>empty() : Optional.of(downloadedDocument.fetchedUrl);
            return new CrawledPage(url, transformedUrl, 200, HTML, Optional.of(html), counters.toMap());
        } catch (HttpStatusException ex) {
            // TODO: Handle 500 errors
            logger.debug("Error getting url {}", url, ex);
            counters.increase("InternalServerErrorReceived");
            return new CrawledPage(url, Optional.empty(), ex.getStatusCode(), Optional.empty(), Optional.empty(), counters.toMap());
        } catch (UnsupportedMimeTypeException ex) {
            logger.debug("Error getting url {}", url, ex);
            counters.increase("UnsupportedMimeTimeErrorReceived");
            return new CrawledPage(url, Optional.empty(), 200, Optional.of(ex.getMimeType()), Optional.empty(), counters.toMap());
        }
    }

    private record DownloadedDocument(String fetchedUrl, Document document) {
    }
}
