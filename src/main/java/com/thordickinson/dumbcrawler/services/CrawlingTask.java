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

import com.thordickinson.dumbcrawler.api.CrawledPage;
import com.thordickinson.dumbcrawler.api.CrawlingResult;

public class CrawlingTask implements Callable<CrawlingResult> {

    private final String url;
    private static Optional<String> HTML = Optional.of("text/html");

    public CrawlingTask(String url) {
        this.url = url;
    }

    @Override
    public CrawlingResult call() {
        long startedAt = System.currentTimeMillis();
        HashSet<String> links = new HashSet<>();
        CrawledPage page = crawl(url, links);
        long endedAt = System.currentTimeMillis();
        return new CrawlingResult(url, page, links, startedAt, endedAt);
    }

    private CrawledPage crawl(String url, Collection<String> linkContainer) {
        try {
            // TODO: create url transformer
            String transformedUrl = "https://api.rocketscrape.com/?apiKey=a8786555-59e6-4171-be58-804718a74a2a&url=%s"
                    .formatted(url);
            var document = Jsoup.connect(transformedUrl).get();
            var links = document.select("a")
                    .stream().map(l -> l.absUrl("href"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            linkContainer.addAll(links);
            return new CrawledPage(url, 200, HTML, Optional.of(document.outerHtml()));
        } catch (HttpStatusException ex) {
            return new CrawledPage(url, ex.getStatusCode(), Optional.empty(), Optional.empty());
        } catch (UnsupportedMimeTypeException ex) {
            return new CrawledPage(url, 200, Optional.of(ex.getMimeType()), Optional.empty());
        } catch (IOException ex) {
            return new CrawledPage(url, -1, Optional.empty(), Optional.empty());
        }
    }
}
