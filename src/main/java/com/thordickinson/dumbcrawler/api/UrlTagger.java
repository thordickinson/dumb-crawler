package com.thordickinson.dumbcrawler.api;

import com.thordickinson.dumbcrawler.expression.URLExpressionEvaluator;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * This component is responsible for tagging URLs based on a set of provided URL expressions.
 * @see URLExpressionEvaluator for supported URL expressions.
 */
@Service
public class UrlTagger extends AbstractCrawlingComponent {

    public static String OTHER_TAG = "other";

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
        if(tagExpressionMap.containsKey(OTHER_TAG)){
            logger.warn("{} is a reserved tag, tagging will be ignored for this tag", OTHER_TAG);
            tagExpressionMap.remove(OTHER_TAG);
        }
    }

    public String[] tagUrls(String url) {
        var tags = tagExpressionMap.entrySet().stream()
               .filter(e -> expressionEvaluator.evaluateBoolean(e.getValue(), url))
               .map(Map.Entry::getKey)
               .toArray(String[]::new);
        final var resolvedTags = tags.length != 0? tags : new String[]{ OTHER_TAG };
        logger.debug("Tagging url: {}, {}", url, resolvedTags);
        return resolvedTags;
    }

}
