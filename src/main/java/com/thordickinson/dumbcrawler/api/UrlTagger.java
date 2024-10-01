package com.thordickinson.dumbcrawler.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.thordickinson.dumbcrawler.expression.URLExpressionEvaluator;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

/**
 * This component is responsible for tagging URLs based on a set of provided URL expressions.
 * @see UrlExpressionEvaluator for supported URL expressions.
 */
@Service
public class UrlTagger extends AbstractCrawlingComponent {

    private Map<String, String> tagExpressionMap = new HashMap<>();
    private final URLExpressionEvaluator expressionEvaluator = new URLExpressionEvaluator() ;

    public UrlTagger() {
        super("urlTagger");
    }
    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        var taggerConfig = context.getConfig("tagger");
        if(taggerConfig.isEmpty()){
            logger.warn("No URL tagger configuration found");
            return;
        }
        var config = taggerConfig.get();
        config.asMap().forEach((key, value) -> tagExpressionMap.put(key, value.toString()));
    }

    public Collection<String> tagUrls(String url) {
        return tagExpressionMap.entrySet().stream()
               .filter(e -> expressionEvaluator.evaluateBoolean(e.getValue(), url))
               .map(Map.Entry::getKey)
               .toList();
    }

}
