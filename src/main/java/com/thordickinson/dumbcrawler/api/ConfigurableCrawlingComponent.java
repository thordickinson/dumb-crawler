package com.thordickinson.dumbcrawler.api;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.util.URLExpressionEvaluator;
import lombok.Getter;

import java.util.Optional;

import static com.thordickinson.dumbcrawler.util.JsonUtil.get;

public class ConfigurableCrawlingComponent extends AbstractCrawlingComponent {

    @Getter
    private URLExpressionEvaluator expressionEvaluator;
    @Getter
    private boolean enabled = false;
    private Optional<String> urlFilter;

    protected ConfigurableCrawlingComponent(String key){
        super(key);
    }

    @Override
    public void initialize(CrawlingContext context) {
        super.initialize(context);
        this.expressionEvaluator = new URLExpressionEvaluator();
        var job = context.getJobDescriptor();
        this.enabled = get(job, getComponentKey()).isPresent() &&
                !getConfiguration("disabled").map(Any::toBoolean).orElse(false);
        urlFilter = getConfiguration("urlFilter").map(Any::toString);
        if (enabled) {
            loadConfigurations(context);
        }
        logger.info("{} component is {}", getComponentKey(), (enabled ? "enabled" : "disabled"));
    }

    protected boolean isDisabled() {
        return !isEnabled();
    }

    protected boolean evaluateUrlFilter(String url) {
        return urlFilter.map(f -> expressionEvaluator.evaluateBoolean(f, url)).orElse(true);
    }

    protected boolean evaluateUrlFilter(String url, Optional<String> contentType) {
        return urlFilter.map(u -> expressionEvaluator.evaluateBoolean(u, url, contentType)).orElse(true);
    }

    protected <T> Optional<T> evaluateExpression(Class<T> type, String expression, String url){
        return expressionEvaluator.evaluateAs(type, expression, url);
    }
}
