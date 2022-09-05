package com.thordickinson.dumbcrawler.services;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thordickinson.dumbcrawler.api.CrawledPage;
import com.thordickinson.dumbcrawler.api.CrawlingResult;

public class CrawlingTask implements Callable<CrawlingResult> {

    private final String originalUrl;
    private final String transformedUrl;
    private static Optional<String> HTML = Optional.of("text/html");
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTask.class);
    private int timeout = 30 * 1000;
    private int maxRetryCount = 3;

    public CrawlingTask(String originalUrl, String transformedUrl) {
        this.originalUrl = originalUrl;
        this.transformedUrl = transformedUrl;
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
        return new CrawledPage(originalUrl, -1, Optional.empty(), Optional.empty());
    }

    private CrawledPage doCrawl(Collection<String> linkContainer) throws IOException {
        try {
            var document = Jsoup.connect(transformedUrl).timeout(timeout).get();
            var html = document.html();
            if (StringUtils.isBlank(html)) {
                logger.error("Parsed html is blank, {}", originalUrl);
            }
            var links = document.select("a")
                    .stream().map(l -> l.absUrl("href"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", originalUrl);
            }
            linkContainer.addAll(links);
            return new CrawledPage(originalUrl, 200, HTML, Optional.of(html));
        } catch (HttpStatusException ex) {
            // TODO: Handle 500 errors
            logger.debug("Error getting url {}", originalUrl, ex);
            return new CrawledPage(originalUrl, ex.getStatusCode(), Optional.empty(), Optional.empty());
        } catch (UnsupportedMimeTypeException ex) {
            logger.debug("Error getting url {}", originalUrl, ex);
            return new CrawledPage(originalUrl, 200, Optional.of(ex.getMimeType()), Optional.empty());
        }
    }
}
