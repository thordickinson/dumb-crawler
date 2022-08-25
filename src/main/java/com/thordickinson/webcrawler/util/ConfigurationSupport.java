package com.thordickinson.webcrawler.util;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.filter.Decider;
import com.thordickinson.webcrawler.filter.Decision;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.thordickinson.webcrawler.util.JsonUtil.get;
import static com.thordickinson.webcrawler.util.JsonUtil.getList;

public class ConfigurationSupport {

    private final String handlerKey;
    private List<Decider> filters = Collections.emptyList();
    private Map<String, Any> configuration = Collections.emptyMap();
    private boolean enabled = false;
    private boolean initialized = false;


    public ConfigurationSupport(String handlerKey) {
        this.handlerKey = handlerKey;
    }

    private void ensureInitialized(CrawlingContext context){
        if(initialized) return;
        var jobConfig = context.getJobConfig();
        enabled = get(jobConfig, handlerKey).isPresent();
        filters = getList(context.getJobConfig(), handlerKey + ".filters").stream().map(Decider::fromJSON).collect(Collectors.toList());
        configuration = get(jobConfig, handlerKey + ".config").map(Any::asMap).orElseGet(Collections::emptyMap);
        initialized = true;
    }

    public boolean shouldProceed(Page page, CrawlingContext ctx){
        ensureInitialized(ctx);
        if (!isEnabled()) return false;
        var result = ctx.evaluateFilter(getFilters(), page);
        if(result != Decision.ACCEPT) return false;
        return true;
    }

    public boolean shouldProceed(WebURL url, CrawlingContext ctx){
        ensureInitialized(ctx);
        if (!isEnabled()) return false;
        var result = ctx.evaluateFilter(getFilters(), url);
        if(result != Decision.ACCEPT) return false;
        return true;
    }

    public Optional<Any> getConfig(String key, CrawlingContext ctx) {
        ensureInitialized(ctx);
        return Optional.ofNullable(configuration.get(key));
    }

    public List<Decider> getFilters() {
        return filters;
    }

    public String getHandlerKey() {
        return handlerKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
