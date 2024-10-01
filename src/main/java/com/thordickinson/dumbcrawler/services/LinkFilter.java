package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class LinkFilter extends AbstractCrawlingComponent {

    private Set<String> whitelist = Collections.emptySet();
    private Set<String> blacklist = Collections.emptySet();
    private boolean allowByDefault = false;

    public LinkFilter() {
        super("linkFilter");
    }

    public boolean isURLAllowed(CrawlingTask task) {
        var url = task.url();
        if(!url.startsWith("http")){
            return  false;
        }
        for(var tag : task.tags()){
            if(blacklist.contains(tag)){
                logger.debug("Url in blacklist: {}", url);
                return false;
            }
            if(whitelist.contains(tag)){
                logger.debug("Url is in whitelist: {}", url);
                return true;
            }
        }
        logger.debug("Allowing by default {}: {}", allowByDefault, url);
        return allowByDefault;
    }

    private Set<String> getList(CrawlingSessionContext context, String color) {
        return new HashSet<String>(context.getConfig("linkFilter.%s".formatted(color))
                .map(Any::asList)
                .map(l -> l.stream().map(Object::toString).toList())
                .orElseGet(Collections::emptyList));
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        whitelist = getList(context, "whitelist");
        blacklist = getList(context, "blacklist");
        allowByDefault = context.getBoolConf("allowByDefault", false);
    }
}
