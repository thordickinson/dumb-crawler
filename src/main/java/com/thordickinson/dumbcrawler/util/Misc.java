package com.thordickinson.dumbcrawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Misc {

    private static final Logger logger = LoggerFactory.getLogger(Misc.class);
    private static final Pattern periodPattern = Pattern.compile("([0-9]+)([smhdw])");

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

    public static Long parsePeriod(String period) {
        if (period == null)
            return null;
        period = period.toLowerCase(Locale.ENGLISH);
        Matcher matcher = periodPattern.matcher(period);
        Instant instant = Instant.EPOCH;
        while (matcher.find()) {
            int num = Integer.parseInt(matcher.group(1));
            String typ = matcher.group(2);
            switch (typ) {
                case "s":
                    instant = instant.plus(Duration.ofSeconds(num));
                    break;
                case "m":
                    instant = instant.plus(Duration.ofMinutes(num));
                    break;
                case "h":
                    instant = instant.plus(Duration.ofHours(num));
                    break;
                case "d":
                    instant = instant.plus(Duration.ofDays(num));
                    break;
                case "w":
                    instant = instant.plus(Period.ofWeeks(num));
                    break;
            }
        }
        return instant.toEpochMilli();
    }


}
