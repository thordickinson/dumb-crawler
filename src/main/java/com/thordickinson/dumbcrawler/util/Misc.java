package com.thordickinson.dumbcrawler.util;

import java.net.URI;
import java.util.Optional;

public class Misc {

    public static Optional<URI> parseURI(String uri) {
        try {
            var parsed = URI.create(uri);
            return Optional.ofNullable(parsed);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
