package com.thordickinson.webcrawler;

import com.thordickinson.webcrawler.api.*;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.IOException;
import java.util.List;

public class GenericFetcher extends PageFetcher {

    private final CrawlingContext context;
    private final List<PrefetchInterceptor> prefetchInterceptors;
    private final List<PageValidator> pageValidators;

    public GenericFetcher(CrawlingContext context, List<PrefetchInterceptor> interceptors, List<PageValidator> pageValidators) {
        super(context.getConfig());
        this.context = context;
        this.prefetchInterceptors = interceptors;
        this.pageValidators = pageValidators;
    }

    @Override
    public PageFetchResult fetchPage(WebURL webUrl) throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        for (var interceptor : prefetchInterceptors) {
            interceptor.intercept(webUrl, context);
        }
        PageFetchResult result = null;
        int attempt = 0;
        PageValidationResult lastAction = PageValidationResult.EMPTY;
        loop:
        do {
            attempt++;
            result = super.fetchPage(webUrl);
            var currentFetch = new PageFetch(result, attempt);
            for (var validator : pageValidators) {
                lastAction = validator.validatePage(currentFetch, context);
                if (lastAction.action() == PageValidationAction.FAIL) {
                    break loop;
                }
            }
        } while (lastAction.action() == PageValidationAction.RETRY);
        if(lastAction.action() == PageValidationAction.FAIL){
            throw new IOException("Error fetching page: " + lastAction.message().orElseGet(()->"<no message>"));
        }
        return result;
    }


}
