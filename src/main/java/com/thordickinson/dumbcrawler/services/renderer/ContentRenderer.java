package com.thordickinson.dumbcrawler.services.renderer;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.util.CollectionUtils;
import org.springframework.stereotype.Service;

import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

import java.util.*;

@Service
public class ContentRenderer extends AbstractCrawlingComponent {

    private final RocketscrapeRenderer rocketscrapeRenderer = new RocketscrapeRenderer();
    private final SimpleHttpRenderer defaultRenderer = new SimpleHttpRenderer();
    private Map<String, Set<String>> renderTagConfig = Collections.emptyMap();


    public ContentRenderer() {
        super("contentRenderer");
    }

    public String renderPage(CrawlingTask task){
       return tryRender(task);
    }

    private String tryRender(CrawlingTask task){
        if(requiresProxy(task)){
            logger.debug("Proxifying ulr {}", task.url());
            return rocketscrapeRenderer.renderHtml(task);
        }
        logger.debug("Using plain connection to get ulr {}", task.url());
        return defaultRenderer.renderHtml(task);
    }


    private boolean requiresProxy(CrawlingTask task){
        var set = renderTagConfig.getOrDefault("proxify", Collections.emptySet());
        if(set.contains("all")){
            return true;
        }
        for(String tag : task.tags()){
            if(set.contains(tag)){
                return true;
            }
        }
        return false;
    }


    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        var config = context.getConfig("renderer");
        renderTagConfig =
        config.map(Any::asMap)
                .map( m -> CollectionUtils.mapValues(m, k -> (Set<String>) new HashSet<>(k.asList().stream().map(Any::toString).toList())))
                .orElse(Collections.emptyMap());
        rocketscrapeRenderer.initialize(context);
        defaultRenderer.initialize(context);
    }
}
