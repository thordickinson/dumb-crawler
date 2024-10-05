package com.thordickinson.dumbcrawler.services;

import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class ContentValidator extends AbstractCrawlingComponent {

    public ContentValidator(){
        super("contentValidator");
    }

    public boolean validatePageContent(Document parsedHtml){
        // throw new CrawlingException("CONTENT_VALIDATION_ERROR", "", true);
        return false;
    }

}
