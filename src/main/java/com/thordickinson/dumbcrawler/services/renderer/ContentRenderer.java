package com.thordickinson.dumbcrawler.services.renderer;

import org.springframework.stereotype.Service;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

@Service
public class ContentRenderer extends AbstractCrawlingComponent {

    public ContentRenderer() {
        super("contentRenderer");
    }
    public HtmlRenderResponse renderPage(CrawlingTask url){
        return HtmlRenderResponse.success("<html><body>Page content</body></html>");
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        
    }
}
