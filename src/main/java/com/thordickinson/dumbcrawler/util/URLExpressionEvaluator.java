package com.thordickinson.dumbcrawler.util;

import com.creativewidgetworks.expressionparser.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
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

    public <T> Optional<T> evaluateAs(Class<T> expectedType, String expression, String url, Optional<String> contentType) {
        if(StringUtils.isBlank(expression) || StringUtils.isBlank(url)) return Optional.empty();
        try{
            var parsed = URI.create(url);
            var variables = new HashMap<String, Object>();
            variables.put("url", new Value().setValue(url));
            variables.put("protocol", new Value().setValue(parsed.getScheme()));
            variables.put("host", new Value().setValue(parsed.getHost()));
            variables.put("path", new Value().setValue(parsed.getPath()));
            variables.put("port", new Value().setValue(new BigDecimal(parsed.getPort())));
            variables.put("query", new Value().setValue(parsed.getQuery()));
            variables.put("fragment", new Value().setValue(parsed.getFragment()));
            variables.put("contentType", new Value().setValue(contentType.orElse(null)));
            var evaluator = ThreadLocalEvaluator.getThreadEvaluator();
            return evaluator.evaluateAs(expectedType, expression, variables);
        }catch(IllegalArgumentException ex){
            logger.warn("Error parsing url: " + url, ex);
            return Optional.empty();
        }
    }
}
