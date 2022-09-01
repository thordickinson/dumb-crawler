package com.thordickinson.dumbcrawler.util;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Misc {

    private static final Logger logger = LoggerFactory.getLogger(Misc.class);

    public static Optional<URI> parseURI(String uri) {
        try {
            var parsed = URI.create(uri);
            if (parsed.getHost() == null) {
                logger.warn("URL does not have host info {}", uri);
            }
            return Optional.ofNullable(parsed);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
