package com.thordickinson.webcrawler;

import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.api.PageHandler;
import com.thordickinson.webcrawler.util.ConfigurationSupport;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class DefaultCrawler extends WebCrawler {

    private final CrawlingContext crawlingContext;

    private ConfigurationSupport configuration = new ConfigurationSupport("crawler");
    private final List<PageHandler> pageHandlers;
    private final Logger filterLogger = LoggerFactory.getLogger(DefaultCrawler.class.getName() + ".filter");

    public DefaultCrawler(CrawlingContext crawlingContext, List<PageHandler> pageHandlers) {
        this.crawlingContext = crawlingContext;
        this.pageHandlers = pageHandlers;
        configuration.ensureInitialized(crawlingContext);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        boolean accepted = configuration.shouldProceed(url, crawlingContext);
        filterLogger.debug("Fetcher: {} : {}", accepted? "ACCEPT": "REJECT", url.getURL());
        return accepted;
    }

    @Override
    public void visit(Page page) {
        for(var handler : pageHandlers){
            try {
                handler.handlePage(page, crawlingContext);
            }catch (Exception ex){
                logger.error("Error handling page {}", handler.getClass().getName(), ex);
            }
        }
    }

    private void debug(Page page){
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            System.out.println("Text length: " + text.length());
            System.out.println("Html length: " + html.length());
            System.out.println("Number of outgoing links: " + links.size());
        }
    }
}
