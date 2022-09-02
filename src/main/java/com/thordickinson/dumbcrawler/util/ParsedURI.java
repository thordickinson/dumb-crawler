package com.thordickinson.dumbcrawler.util;

import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public record ParsedURI(String originalUri, Optional<String> host, int port, Optional<String> path,
        Optional<String> query) {
    public static Optional<ParsedURI> fromString(String uri) {
        try {
            var parsed = URI.create(uri);
            var host = Optional.ofNullable(StringUtils.trimToNull(parsed.getHost()));
            var path = Optional.ofNullable(StringUtils.trimToNull(parsed.getPath()));
            var query = Optional.ofNullable(StringUtils.trimToNull(parsed.getQuery()));
            return Optional.of(new ParsedURI(uri, host, parsed.getPort(), path, query));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
