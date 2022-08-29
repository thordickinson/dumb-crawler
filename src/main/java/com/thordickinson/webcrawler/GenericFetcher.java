package com.thordickinson.webcrawler;

import com.thordickinson.webcrawler.api.*;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GenericFetcher extends PageFetcher {

    private static final PageFetchResult ERROR = new PageFetchResult();
    static {
        ERROR.setStatusCode(500);
    }
    private final CrawlingContext context;
    private final List<PrefetchInterceptor> prefetchInterceptors;
    private final List<PageValidator> pageValidators;

    private final List<URLTransformer> urlTransformers;
    private AtomicBoolean isStopped = new AtomicBoolean(false);

    public GenericFetcher(CrawlingContext context, List<PrefetchInterceptor> interceptors,
            List<PageValidator> pageValidators, List<URLTransformer> urlTransformers) {
        super(context.getConfig());
        this.context = context;
        this.prefetchInterceptors = interceptors;
        this.pageValidators = pageValidators;
        this.urlTransformers = urlTransformers;
    }

    public PageFetchResult intercept(WebURL webUrl)
            throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        var originalUrl = webUrl.getURL();
        for (var interceptor : prefetchInterceptors) {
            interceptor.intercept(webUrl, context);
        }
        PageFetchResult result = null;
        int attempt = 0;
        PageValidationResult lastAction = PageValidationResult.EMPTY;
        loop: do {
            attempt++;
            result = super.fetchPage(webUrl);
            result.setFetchedUrl(originalUrl);

            var currentFetch = new PageFetch(result, attempt);
            for (var validator : pageValidators) {
                lastAction = validator.validatePage(currentFetch, context);
                if (lastAction.action() == PageValidationAction.FAIL) {
                    break loop;
                }
            }
        } while (lastAction.action() == PageValidationAction.RETRY);
        if (lastAction.action() == PageValidationAction.FAIL) {
            throw new IOException("Error fetching page: " + lastAction.message().orElseGet(() -> "<no message>"));
        }
        return result;
    }

    private String getUrlToFetch(WebURL url) {
        String result = url.getURL();
        for (URLTransformer transformer : urlTransformers) {
            result = transformer.transform(result, context);
        }
        return result;
    }

    @Override
    public synchronized void shutDown() {
        this.isStopped.set(true);
        super.shutDown();
    }

    public PageFetchResult fetchPage(WebURL webUrl)
            throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        // Getting URL, setting headers & content
        PageFetchResult fetchResult = new PageFetchResult();
        String toFetchURL = webUrl.getURL();
        String transformedUrl = getUrlToFetch(webUrl);
        HttpUriRequest request = null;
        try {
            request = newHttpUriRequest(transformedUrl);
            if (config.getPolitenessDelay() > 0) {
                // Applying Politeness delay
                synchronized (mutex) {
                    long now = (new Date()).getTime();
                    if ((now - lastFetchTime) < config.getPolitenessDelay()) {
                        Thread.sleep(config.getPolitenessDelay() - (now - lastFetchTime));
                    }
                    lastFetchTime = (new Date()).getTime();
                }
            }

            if (isStopped.get())
                return ERROR;
            CloseableHttpResponse response = httpClient.execute(request);
            fetchResult.setEntity(response.getEntity());
            fetchResult.setResponseHeaders(response.getAllHeaders());

            // Setting HttpStatus
            int statusCode = response.getStatusLine().getStatusCode();

            // If Redirect ( 3xx )
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                    statusCode == HttpStatus.SC_SEE_OTHER ||
                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
                    statusCode == 308) { // todo follow
                // https://issues.apache.org/jira/browse/HTTPCORE-389

                Header header = response.getFirstHeader("Location");
                if (header != null) {
                    String movedToUrl = URLCanonicalizer.getCanonicalURL(header.getValue(), toFetchURL);
                    fetchResult.setMovedToUrl(movedToUrl);
                }
            } else if (statusCode >= 200 && statusCode <= 299) { // is 2XX, everything looks ok
                fetchResult.setFetchedUrl(toFetchURL);
                String uri = request.getURI().toString();
                if (toFetchURL.equals(transformedUrl) && !uri.equals(toFetchURL)) {
                    if (!URLCanonicalizer.getCanonicalURL(uri).equals(toFetchURL)) {
                        fetchResult.setFetchedUrl(uri);
                    }
                }

                // Checking maximum size
                if (fetchResult.getEntity() != null) {
                    long size = fetchResult.getEntity().getContentLength();
                    if (size == -1) {
                        Header length = response.getLastHeader("Content-Length");
                        if (length == null) {
                            length = response.getLastHeader("Content-length");
                        }
                        if (length != null) {
                            size = Integer.parseInt(length.getValue());
                        }
                    }
                    if (size > config.getMaxDownloadSize()) {
                        // fix issue #52 - consume entity
                        response.close();
                        throw new PageBiggerThanMaxSizeException(size);
                    }
                }
                context.increaseCounter("fetchedPages");
            }

            context.increaseCounter("fetchAttempts");
            fetchResult.setStatusCode(statusCode);
            return fetchResult;

        } finally { // occurs also with thrown exceptions
            if ((fetchResult.getEntity() == null) && (request != null)) {
                request.abort();
            }
        }
    }

}
