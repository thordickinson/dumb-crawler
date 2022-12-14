package com.thordickinson.dumbcrawler.services;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.thordickinson.dumbcrawler.api.URLTransformer;
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
    private final List<URLTransformer> urlTransformers;
    private static Optional<String> HTML = Optional.of("text/html");
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTask.class);
    private int timeout = 30 * 1000;
    private int maxRetryCount = 3;
    private int maxValidationCount = 3;
    private final List<ContentValidator> contentValidators;

    public CrawlingTask(String originalUrl, List<ContentValidator> contentValidators, List<URLTransformer> urlTransformers) {
        this.originalUrl = originalUrl;
        this.contentValidators = contentValidators;
        this.urlTransformers = urlTransformers;
    }

    private String transformUrl(Map<String, String> renderingHints, Counters counters) {
        renderingHints = renderingHints == null ? Collections.emptyMap() : renderingHints;
        String result = originalUrl;
        for (var t : urlTransformers) result = t.transform(result, renderingHints, counters);
        return result;
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
        } catch (InvalidDocumentException ex) {
            logger.debug("Invalid document detected");
            page = createErrorPage(originalUrl);
            exception = ex;
        }

        long endedAt = System.currentTimeMillis();
        return new CrawlingResult(page, links, startedAt, endedAt, Optional.ofNullable(exception));
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
        return new CrawledPage(originalUrl, Optional.empty(), -1, Optional.empty(), Optional.empty(), Collections.emptyMap());
    }


    private DownloadedDocument getDocument(Counters counters) throws IOException {
        int attempt = 0;
        boolean invalid = true;
        Document document = null;
        var renderingHints = Map.<String, String>of();
        var transformedUrl = originalUrl;
        do {
            transformedUrl = transformUrl(renderingHints, counters);
            counters.increase("GetRequests");
            document = Jsoup.connect(transformedUrl).timeout(timeout).get();
            invalid = false;
            for (ContentValidator validator : contentValidators) {
                var validationResult = validator.validateContent(originalUrl, document);
                if (!validationResult.pageValid()) {
                    renderingHints = validationResult.renderingHints();
                    counters.increase("invalidContentDetection");
                    logger.debug("Invalid content detected on url: {}", originalUrl);
                    invalid = true;
                }
            }
            attempt++;
        } while (invalid && attempt < maxValidationCount);
        if (attempt >= maxValidationCount) {
            counters.increase("invalidContentDiscarded");
            throw new InvalidDocumentException("Document is invalid");
        }
        return new DownloadedDocument(transformedUrl, document);
    }

    private CrawledPage doCrawl(Collection<String> linkContainer) throws IOException {
        var counters = new Counters();
        try {
            var downloadedDocument = getDocument(counters);
            var document = downloadedDocument.document;
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
            var transformedUrl = Objects.equals(originalUrl, downloadedDocument.fetchedUrl) ? Optional.<String>empty() : Optional.of(downloadedDocument.fetchedUrl);
            return new CrawledPage(originalUrl, transformedUrl, 200, HTML, Optional.of(html), counters.toMap());
        } catch (HttpStatusException ex) {
            // TODO: Handle 500 errors
            logger.debug("Error getting url {}", originalUrl, ex);
            counters.increase("InternalServerErrorReceived");
            return new CrawledPage(originalUrl, Optional.empty(), ex.getStatusCode(), Optional.empty(), Optional.empty(), counters.toMap());
        } catch (UnsupportedMimeTypeException ex) {
            logger.debug("Error getting url {}", originalUrl, ex);
            counters.increase("UnsupportedMimeTimeErrorReceived");
            return new CrawledPage(originalUrl, Optional.empty(), 200, Optional.of(ex.getMimeType()), Optional.empty(), counters.toMap());
        }
    }

    private record DownloadedDocument(String fetchedUrl, Document document) {
    }
}
