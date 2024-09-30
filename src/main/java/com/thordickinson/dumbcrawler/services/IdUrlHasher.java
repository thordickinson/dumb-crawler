package com.thordickinson.dumbcrawler.services;

import java.util.Optional;

import com.thordickinson.dumbcrawler.util.ConfigurableCrawlingComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;

@Service
public class IdUrlHasher extends ConfigurableCrawlingComponent {
    
    private Optional<String> idExtractor;
    public IdUrlHasher(){
        super("idUrlHasher");
    }

    @Override
    public void loadConfigurations(CrawlingSessionContext context) {
        idExtractor = getConfiguration("idExtractor").map(Any::toString);
    }
    public Optional<String> hashUrl(String url) {
        if(isDisabled()) return Optional.empty();
        if(idExtractor.isEmpty()) return Optional.empty();
        if(!evaluateUrlFilter(url)) return Optional.empty();
        var externalId = evaluateExpression(String.class, idExtractor.get(), url);
        return externalId.map(DigestUtils::md5Hex);
    }
    
}
