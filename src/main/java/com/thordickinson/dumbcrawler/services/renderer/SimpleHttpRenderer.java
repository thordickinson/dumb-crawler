package com.thordickinson.dumbcrawler.services.renderer;

import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.HtmlRenderer;

public class SimpleHttpRenderer extends AbstractCrawlingComponent implements HtmlRenderer {

    public SimpleHttpRenderer(){
        super("simpleHttpRenderer");
    }
    @Override
    public String renderHtml(String url) {
        return "";
    }
}
