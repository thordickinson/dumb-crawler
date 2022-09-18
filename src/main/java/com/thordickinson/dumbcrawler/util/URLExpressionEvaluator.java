package com.thordickinson.dumbcrawler.util;

import com.creativewidgetworks.expressionparser.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

public class URLExpressionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(URLExpressionEvaluator.class);
    private final Parser parser = new Parser();
    private final FunctionToolbox toolbox = FunctionToolbox.register(parser);
    private final Value FALSE = new Value().setValue(false);
    private final Value TRUE = new Value().setValue(true);
    private final Value NULL = new Value();
    private final Object UNPARSED = new Object();
    private final Set<String> COMMON_EXTENSIONS = Set.of("css", "js", "sass", "less", "ico", "jpeg", "jpg", "png",
            "webp",
            "pdf", "mpeg", "mpg", "mp3", "mp4", "avi", "ogg", "wav", "iso");
    private static final Map<ValueType, Set<Class<?>>> COMPATIBLE_TYPES_MAP = Map.of(ValueType.BOOLEAN, Set.of(Boolean.class),
            ValueType.DATE, Set.of(Date.class),
            ValueType.ARRAY, Set.of(List.class),
            ValueType.NUMBER, Set.of(BigDecimal.class), 
            ValueType.STRING, Set.of(String.class),
            ValueType.UNDEFINED, Set.of(Void.class)
            );
    private final Pattern RESOURCE_PATTERN = Pattern
            .compile(".*\\.(%s)$".formatted(String.join("|", COMMON_EXTENSIONS)));

    public URLExpressionEvaluator(){
        parser.addFunction(new Function("MATCHES", this, "_matches", 2, 2, ValueType.STRING, ValueType.STRING));
        parser.addFunction(new Function("EXTRACT", this, "_extract", 2, 2, ValueType.STRING, ValueType.STRING));
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

    public Value _extract(Token function ,Stack<Token> stack){
        String regex = ((Token)stack.pop()).asString();
        String value = ((Token)stack.pop()).asString();
        if(value == null || regex == null) return NULL;
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(value);
        if(!matcher.matches()) return NULL;
        return new Value().setValue(matcher.group("value"));
    }

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
            parser.addVariable("url", new Value().setValue(url));
            parser.addVariable("protocol", new Value().setValue(parsed.getScheme()));
            parser.addVariable("host", new Value().setValue(parsed.getHost()));
            parser.addVariable("path", new Value().setValue(parsed.getPath()));
            parser.addVariable("port", new Value().setValue(new BigDecimal(parsed.getPort())));
            parser.addVariable("query", new Value().setValue(parsed.getQuery()));
            parser.addVariable("fragment", new Value().setValue(parsed.getFragment()));
            parser.addVariable("contentType", new Value().setValue(contentType.orElse(null)));
    
            var result = parser.eval(expression);
            var objectResult = result.asObject();
            if (objectResult instanceof Throwable) {
                throw new RuntimeException("Error evaluating expression: " + expression, (Throwable) objectResult);
            }
            checkResultType(expectedType, result);
            return parseType(expectedType, result);
        }catch(IllegalArgumentException ex){
            logger.warn("Error parsing url: " + url, ex);
            return Optional.empty();
        }
    }

    private <T> Optional<T> parseType(Class<T> expected, Value result){
        var type = result.getType();
        Object value = UNPARSED;
        if(expected == Boolean.class) value = result.asBoolean();
        if(expected == String.class) value = result.asString();
        if(expected == Integer.class) value = Integer.valueOf(result.asNumber().intValue());
        if(expected == Long.class) value = Long.valueOf(result.asNumber().longValue());
        if(expected == BigDecimal.class) value = result.asNumber();
        if(expected == List.class) value = result.getArray();
        if(expected == Date.class) value = result.asDate();
        if(expected == Instant.class) value = result.asDate().toInstant();
        if(value == UNPARSED){
            throw new RuntimeException("Unable to parse result type " + type + " as " + expected.getSimpleName());
        }
        return Optional.of((T) value);
    }

    private void checkResultType(Class<?> expected, Value result){
        var compatibleTypes = COMPATIBLE_TYPES_MAP.get(result.getType());
        if(!compatibleTypes.contains(expected)){
            throw new RuntimeException("Expression result is not of the expected type:  Expected: " +
             expected.getSimpleName() + " -  Result: "+ result.getType().name());
        }
    }

}
