package com.thordickinson.dumbcrawler.api;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;

public class DefaultURLHasher implements URLHasher {

    @Override
    public void initialize(CrawlingContext context) {
    }

    @Override
    public Optional<String> hashUrl(String url) {
        return Optional.of(DigestUtils.md5Hex(url));
    }
    
}
