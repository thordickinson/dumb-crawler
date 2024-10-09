package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.util.CollectionUtils;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class ContentValidator extends AbstractCrawlingComponent {

    private Map<String,String> tagValidators = Collections.emptyMap();
    public ContentValidator(){
        super("contentValidator");
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        tagValidators = CollectionUtils.mapValues(context.getConfig("validationSelectors").map(Any::asMap)
                .orElse(Collections.emptyMap()), Object::toString);
    }

    private void validateTag(String tag, CrawlingTask task, Document parsedHtml){
        if(!tagValidators.containsKey(tag)){
            return;
        }
        var validator = tagValidators.get(tag);
        var elements = parsedHtml.select(validator);
        if(elements.isEmpty()){
            throw new CrawlingException(task, "CONTENT_VALIDATION_ERROR_" + tag, true);
        }
    }

    public void validatePageContent(CrawlingTask task, Document parsedHtml){
        if(tagValidators.isEmpty()){
            return;
        }
        for(var tag : task.tags()){
            validateTag(tag, task, parsedHtml);
        }
    }

}
