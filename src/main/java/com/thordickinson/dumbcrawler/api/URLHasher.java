package com.thordickinson.dumbcrawler.api;

import java.util.Optional;

public interface URLHasher extends CrawlingComponent {
    
    Optional<String> hashUrl(String url);
}
