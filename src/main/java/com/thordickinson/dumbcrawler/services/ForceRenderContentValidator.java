package com.thordickinson.dumbcrawler.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.thordickinson.dumbcrawler.util.ConfigurableCrawlingComponent;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.ContentValidator;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.expression.URLExpressionEvaluator;

/**
 *
 * "contentValidator": {
 *   "validIf": "NOT matches(path, '^\/(MCO|mco)-[0-9]+.*') OR contains(html, 'technical_specifications')"
 * }
 */
@Deprecated
@Service
public class ForceRenderContentValidator extends ConfigurableCrawlingComponent implements ContentValidator  {

    private Optional<String>  expression = Optional.empty();


    public ForceRenderContentValidator(){
        super("forceRenderContentValidator");
    }

    
    @Override
    public void loadConfigurations(CrawlingSessionContext context) {
        expression = getConfiguration("validIf").map(Any::toString);
    }

    private Map<String,Object> getVariables(String url, Document document){
        Map<String,Object> map = new HashMap<String, Object>(URLExpressionEvaluator.getVariablesfromUrl(url).orElse(Collections.emptyMap()));
        map.put("document", document);
        map.put("html", document.html());
        return map;
    }

    @Override
    public ValidationResult validateContent(String url, Document document) {
        if(!evaluateUrlFilter(url)) return VALID;
        var validPage = expression.flatMap(e ->
                evaluate(Boolean.class, e, getVariables(url, document))
        ).orElse(true);
        var renderingHints = new HashMap<String, String>();
        if(!validPage){
            renderingHints.put("rocketScrape.renderContent", "true");
        }
        return new ValidationResult(validPage, renderingHints);
    }

}
