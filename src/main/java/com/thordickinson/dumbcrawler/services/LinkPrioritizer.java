package com.thordickinson.dumbcrawler.services;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.util.CollectionUtils;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LinkPrioritizer extends AbstractCrawlingComponent {

    private Map<String,Integer> priorities = Collections.emptyMap();
    public LinkPrioritizer(){
        super("linkPrioritizer");
    }

    public int getPriorityForTag(String[] tags){
        return Streams.of(tags).map(tag -> priorities.getOrDefault(tag, 0))
                .max(Comparator.naturalOrder()).orElse(0);
    }

    @Override
    protected void loadConfigurations(CrawlingSessionContext context) {
        var priorities = context.getConfig("priorities");
        this.priorities = priorities.map(Any::asMap).map(m -> CollectionUtils.mapValues(m, Any::toInt)).
                orElse(Collections.emptyMap());
    }
}
