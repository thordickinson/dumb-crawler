package com.thordickinson.dumbcrawler.util;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.thordickinson.dumbcrawler.util.JsonUtil.get;

public class ConfigurationSupport {
    @Getter
    private final String componentKey;
    private Map<String, Any> configuration = Collections.emptyMap();
    private boolean enabled = false;
    private final Optional<String> filter;
    private final CrawlingContext crawlingContext;
    private final URLExpressionEvaluator evaluator = new URLExpressionEvaluator();


    public ConfigurationSupport(String componentKey, CrawlingContext crawlingContext) {
        this.componentKey = componentKey.intern();
        this.crawlingContext = crawlingContext;
        var jobConfig = crawlingContext.getJobDescriptor();

        enabled = get(jobConfig, componentKey).isPresent() && !get(jobConfig, componentKey + ".disabled").map(Any::toBoolean).orElse(false);
        configuration = get(jobConfig, componentKey + ".config").map(Any::asMap).orElseGet(Collections::emptyMap);
        filter = get(jobConfig, "filter").map(Any::toString);
    }


    public boolean evaluateFilter(String url, Optional<String> contentType){
        if(!enabled) return false;
        return filter.map(f -> evaluator.evaluateBoolean(f, url, contentType)).orElse(true);
    }

    public boolean evaluateFilter(CrawlingResult result){
        return evaluateFilter(result.page().originalUrl(), result.page().contentType());
    }

    public Optional<Any> getConfig(String key) {
        return Optional.ofNullable(configuration.get(key));
    }

    public boolean isEnabled() {
        return enabled;
    }

}
