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

    private final String url;
    private static Optional<String> HTML = Optional.of("text/html");
    private static final Logger logger = LoggerFactory.getLogger(CrawlingTask.class);
    private int timeout = 30 * 1000;
    private int maxRetryCount = 3;

    public CrawlingTask(String url) {
        this.url = url;
    }

    @Override
    public CrawlingResult call() {
        long startedAt = System.currentTimeMillis();
        HashSet<String> links = new HashSet<>();
        CrawledPage page = null;
        Exception exception = null;
        try {
            page = crawl(url, links);
        } catch (IOException ex) {
            logger.debug("Error while fetching page", ex);
            page = createErrorPage(url);
            exception = ex;
        }

        long endedAt = System.currentTimeMillis();
        return new CrawlingResult(url, page, links, startedAt, endedAt, Optional.ofNullable(exception));
    }

    private CrawledPage crawl(String url, Collection<String> linkContainer) throws IOException {
        var attempt = 0;
        IOException lastException = null;
        do {
            attempt++;
            try {
                return doCrawl(url, linkContainer);
            } catch (IOException ex) {
                lastException = ex;
            }
        } while (attempt < maxRetryCount);
        throw lastException;
    }

    private CrawledPage createErrorPage(String url) {
        return new CrawledPage(url, -1, Optional.empty(), Optional.empty());
    }

    private CrawledPage doCrawl(String url, Collection<String> linkContainer) throws IOException {
        try {
            // TODO: create url transformer
            String transformedUrl = "https://api.rocketscrape.com/?apiKey=a8786555-59e6-4171-be58-804718a74a2a&url=%s"
                    .formatted(url);
            var document = Jsoup.connect(transformedUrl).timeout(timeout).get();
            var html = document.html();
            if (StringUtils.isBlank(html)) {
                logger.error("Parsed html is blank, {}", url);
            }
            var links = document.select("a")
                    .stream().map(l -> l.absUrl("href"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            if (links.size() > 300) {
                logger.warn("Page {} has more than 300 links", url);
            }
            linkContainer.addAll(links);
            return new CrawledPage(url, 200, HTML, Optional.of(html));
        } catch (HttpStatusException ex) {
            // TODO: Handle 500 errors
            logger.debug("Error getting url {}", url, ex);
            return new CrawledPage(url, ex.getStatusCode(), Optional.empty(), Optional.empty());
        } catch (UnsupportedMimeTypeException ex) {
            logger.debug("Error getting url {}", url, ex);
            return new CrawledPage(url, 200, Optional.of(ex.getMimeType()), Optional.empty());
        }
    }
}
