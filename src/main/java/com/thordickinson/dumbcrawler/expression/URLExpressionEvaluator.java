package com.thordickinson.dumbcrawler.expression;

import com.thordickinson.dumbcrawler.util.ThreadLocalEvaluator;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class URLExpressionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(URLExpressionEvaluator.class);

    public boolean evaluateBoolean(String expression, String uri) {
        return evaluateBoolean(expression, uri, Optional.empty());
    }

    public boolean evaluateBoolean(String expression, String uri, Optional<String> contentType) {
        return evaluateAs(Boolean.class, expression, uri, contentType).orElse(Boolean.FALSE);
    }

    public <T> Optional<T> evaluateAs(Class<T> expectedType, String expression, String url) {
        return evaluateAs(expectedType, expression, url, Optional.empty());
    }

    public static Optional<Map<String, Object>> getVariablesfromUrl(String url) {
        return getVariablesfromUrl(url, Optional.empty());
    }

    public static Optional<Map<String, Object>> getVariablesfromUrl(String url, Optional<String> contentType) {
        var parsed = HttpUrl.parse(url);
        if (parsed == null) {
            logger.warn("Error parsing url: {}", url);
            return Optional.empty();
        }
        var variables = new HashMap<String, Object>();
        variables.put("url", url);
        variables.put("protocol", parsed.scheme());
        variables.put("host", parsed.host());
        variables.put("path", parsed.encodedPath());
        variables.put("port", new BigDecimal(parsed.port()));
        variables.put("query", parsed.query());
        variables.put("fragment", parsed.fragment());
        variables.put("contentType", contentType.orElse(null));
        return Optional.of(variables);
    }


    public <T> Optional<T> evaluateAs(Class<T> expectedType, String expression, String url, Optional<String> contentType) {
        if (StringUtils.isBlank(expression) || StringUtils.isBlank(url)) return Optional.empty();
        try {
            return getVariablesfromUrl(url, contentType).flatMap(variables ->
                    ThreadLocalEvaluator.getThreadEvaluator().evaluateAs(expectedType, expression, variables)
            );
        } catch (IllegalArgumentException ex) {
            logger.warn("Error parsing url: " + url, ex);
            return Optional.empty();
        }
    }
}
