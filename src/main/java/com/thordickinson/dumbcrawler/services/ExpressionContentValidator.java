package com.thordickinson.dumbcrawler.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.ContentValidator;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.util.URLExpressionEvaluator;

/**
 *
 * "contentValidator": {
 *   "validIf": "NOT matches(path, '^\/(MCO|mco)-[0-9]+.*') OR contains(html, 'technical_specifications')"
 * }
 */
@Service
public class ExpressionContentValidator extends AbstractCrawlingComponent implements ContentValidator  {

    private Optional<String>  expression = Optional.empty();

    public ExpressionContentValidator(){
        super("contentValidator");
    }

    
    @Override
    public void loadConfigurations(CrawlingContext context) {
        expression = getConfiguration("validIf").map(Any::toString);
    }

    private Map<String,Object> getVariables(String url, Document document){
        Map<String,Object> map = new HashMap<String, Object>(URLExpressionEvaluator.getVariablesfromUrl(url).orElse(Collections.emptyMap()));
        map.put("document", document);
        map.put("html", document.html());
        return map;
    }

    @Override
    public boolean validateContent(String url, Document document) {
        return expression.flatMap(e -> 
            evaluate(Boolean.class, e, getVariables(url, document))
        ).orElse(true);
    }

}
