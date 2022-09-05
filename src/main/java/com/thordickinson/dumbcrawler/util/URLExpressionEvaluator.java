package com.thordickinson.dumbcrawler.util;

import com.creativewidgetworks.expressionparser.*;
import com.google.re2j.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

public class URLExpressionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(URLExpressionEvaluator.class);
    private final Parser parser = new Parser();
    private final FunctionToolbox toolbox = FunctionToolbox.register(parser);
    private final Value FALSE = new Value().setValue(false);
    private final Value TRUE = new Value().setValue(true);
    private final Set<String> COMMON_EXTENSIONS = Set.of("css", "js", "sass", "less", "ico", "jpeg", "jpg", "png",
            "webp",
            "pdf", "mpeg", "mpg", "mp3", "mp4", "avi", "ogg", "wav", "iso");
    private final Pattern RESOURCE_PATTERN = Pattern
            .compile(".*\\.(%s)$".formatted(String.join("|", COMMON_EXTENSIONS)));

    public URLExpressionEvaluator(){
        parser.addFunction(new Function("MATCHES", this, "_matches", 2, 2, ValueType.STRING, ValueType.STRING));
        parser.addFunction(new Function("ISRESOURCE", this, "_isResource", 1, 1, ValueType.STRING));
    }

    public Value _matches(Token function, Stack<Token> stack){
        String regex = ((Token)stack.pop()).asString();
        String value = ((Token)stack.pop()).asString();
        if(value == null || regex == null) return FALSE;
        return value.matches(regex)? TRUE : FALSE;
    }

    public Value _isResource(Token funciton, Stack<Token> stack) {
        String path = ((Token) stack.pop()).asString();
        if (path == null)
            return FALSE;
        var matcher = RESOURCE_PATTERN.matcher(path);
        return matcher.matches() ? TRUE : FALSE;
    }

    public boolean evaluate(String expression, String uri) {
        return evaluate(expression, uri, Optional.empty());
    }

    public boolean evaluate(String expression, String uri, Optional<String> contentType) {
        if(uri == null) return false;
        try {
            var parsed = URI.create(uri);
            return evaluate(expression, parsed, contentType);
        }catch (IllegalArgumentException ex){
            logger.warn("Error parsing url: " + uri, ex);
            return false;
        }
    }

    public boolean evaluate(String expression, URI uri, Optional<String> contentType) {
        if(StringUtils.isBlank(expression)) return true;
        parser.addVariable("protocol", new Value().setValue(uri.getScheme()));
        parser.addVariable("host", new Value().setValue(uri.getHost()));
        parser.addVariable("path", new Value().setValue(uri.getPath()));
        parser.addVariable("port", new Value().setValue(new BigDecimal(uri.getPort())));
        parser.addVariable("query", new Value().setValue(uri.getQuery()));
        parser.addVariable("fragment", new Value().setValue(uri.getFragment()));
        parser.addVariable("contentType", new Value().setValue(contentType.orElse(null)));

        var result = parser.eval(expression);
        var objectResult = result.asObject();
        if (objectResult instanceof Throwable) {
            throw new RuntimeException("Error evaluating expression: " + expression, (Throwable) objectResult);
        }
        if(result.getType() != ValueType.BOOLEAN){
            throw new RuntimeException("Expression result is not a boolean value: "+ result.getType().name());
        }
        return result.asBoolean();
    }

}
