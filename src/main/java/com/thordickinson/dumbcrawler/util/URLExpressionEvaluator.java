package com.thordickinson.dumbcrawler.util;

import com.creativewidgetworks.expressionparser.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import java.util.Stack;

public class URLExpressionEvaluator {
    private final Parser parser = new Parser();
    private final FunctionToolbox toolbox = FunctionToolbox.register(parser);
    private final Value FALSE = new Value().setValue(false);
    private final Value TRUE = new Value().setValue(true);

    public URLExpressionEvaluator(){
        parser.addFunction(new Function("MATCHES", this, "_matches", 2, 2, ValueType.STRING, ValueType.STRING));
    }

    public Value _matches(Token function, Stack<Token> stack){
        String regex = ((Token)stack.pop()).asString();
        String value = ((Token)stack.pop()).asString();
        if(value == null || regex == null) return FALSE;
        return value.matches(regex)? TRUE : FALSE;
    }
    public boolean evaluate(String expression, String uri) {
        return evaluate(expression, uri, Optional.empty());
    }

    public boolean evaluate(String expression, String uri, Optional<String> contentType) {        var parsed = URI.create(uri);
        return evaluate(expression, URI.create(uri), contentType);
    }

    public boolean evaluate(String expression, URI uri, Optional<String> contentType) {
        if(StringUtils.isBlank(expression)) return true;
        parser.addVariable("protocol", new Value().setValue(uri.getScheme()));
        parser.addVariable("host", new Value().setValue(uri.getHost()));
        parser.addVariable("path", new Value().setValue(uri.getPath()));
        parser.addVariable("port", new Value().setValue(new BigDecimal(uri.getPort())));
        parser.addVariable("query", new Value().setValue(uri.getQuery()));
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
