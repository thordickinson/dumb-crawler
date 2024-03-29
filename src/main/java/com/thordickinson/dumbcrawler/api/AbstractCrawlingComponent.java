package com.thordickinson.dumbcrawler.api;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.util.ThreadLocalEvaluator;
import com.thordickinson.dumbcrawler.util.URLExpressionEvaluator;

import static com.thordickinson.dumbcrawler.util.JsonUtil.*;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class AbstractCrawlingComponent implements CrawlingComponent {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private CrawlingContext context;
    @Getter
    private URLExpressionEvaluator expressionEvaluator;
    @Getter
    private boolean enabled = false;
    private final String componentKey;
    private Optional<String> urlFilter;

    public AbstractCrawlingComponent(String componentKey) {
        this.componentKey = componentKey;
    }

    @Override
    public void initialize(CrawlingContext context) {
        this.context = context;
        this.expressionEvaluator = new URLExpressionEvaluator();
        var job = context.getJobDescriptor();
        this.enabled = get(job, componentKey).isPresent() &&
                !getConfiguration("disabled").map(Any::toBoolean).orElse(false);
        urlFilter = getConfiguration("urlFilter").map(Any::toString);
        if (enabled) {
            loadConfigurations(context);
        }
        logger.info("{} component is {}", componentKey, (enabled ? "enabled" : "disabled"));
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

    protected <T> Optional<T> evaluate(Class<T> targetType, String expression, Map<String, Object> variables){
        var evaluator = ThreadLocalEvaluator.getThreadEvaluator();
        return evaluator.evaluateAs(targetType, expression, variables);
    }

    protected boolean isDisabled() {
        return !isEnabled();
    }


    protected void setCounter(String key, Serializable value){
        if(this.context != null) return;
        this.context.setCounter(componentKey + "." + key, value);
    }

    protected void increaseCounter(String counter) {
        increaseCounter(counter, 1);
    }

    protected void increaseCounter(String counter, int amount) {
        if (this.context != null)
            this.context.increaseCounter(componentKey + "." + counter, amount);
    }

    protected Optional<Any> getConfiguration(String key) {
        return get(context.getJobDescriptor(), componentKey + "." + key);
    }

    protected void loadConfigurations(CrawlingContext context) {
    }
}
