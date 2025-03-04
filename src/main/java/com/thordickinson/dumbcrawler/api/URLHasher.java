package com.thordickinson.dumbcrawler.api;

import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class URLHasher extends AbstractCrawlingComponent {

    private Pattern pattern = null;

    public URLHasher() {
        super("urlHasher");
    }

    @Override
    public void initialize(CrawlingSessionContext context) {
        var configPattern = context.getStringConf("idExtractorPattern", null);
        if(configPattern == null) {
            logger.warn("No idExtractorPattern configured, will use default hasher.");
            return;
        }
        if(!configPattern.contains("?<id>")){
            throw new IllegalArgumentException("Invalid idExtractorPattern, should contain '?<id>'");
        }
        pattern = Pattern.compile(configPattern);
    }

    public String hashUrl(String url) {
        if(pattern != null){
            final var matcher = pattern.matcher(url);
            if(matcher.matches()){
                final var hash = matcher.group("id");
                logger.debug("Getting url has from {} -> {}", hash, url);
                return hash;
            }
        }
        logger.debug("Not able to detect hash for url, returning md5: {}", url);
        return DigestUtils.md5Hex(url);
    }
    
}
