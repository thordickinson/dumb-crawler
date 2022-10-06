package com.thordickinson.dumbcrawler.util;

import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;

import com.creativewidgetworks.expressionparser.*;

public class ExpressionFunctions {
    private static final Set<String> COMMON_EXTENSIONS = Set.of("css", "js", "sass", "less", "ico", "jpeg", "jpg",
            "png",
            "webp",
            "pdf", "mpeg", "mpg", "mp3", "mp4", "avi", "ogg", "wav", "iso");

    private final Pattern RESOURCE_PATTERN = Pattern
            .compile(".*\\.(%s)$".formatted(String.join("|", COMMON_EXTENSIONS)));

    private static final Value FALSE = new Value().setValue(false);
    private static final Value TRUE = new Value().setValue(true);
    private static final Value NULL = new Value();

    public static final ExpressionFunctions instance = new ExpressionFunctions();

    private ExpressionFunctions(){
    }


    public static void register(Parser parser){
        parser.addFunction(new Function("MATCHES", instance, "_matches", 2, 2, ValueType.STRING, ValueType.STRING));
        parser.addFunction(new Function("EXTRACT", instance, "_extract", 2, 2, ValueType.STRING, ValueType.STRING));
        parser.addFunction(new Function("ISRESOURCE", instance, "_isResource", 1, 1, ValueType.STRING));
        parser.addFunction(new Function("CONTAINSELEMENT", instance, "_containsElement", 2, 2, ValueType.OBJECT, ValueType.STRING));
    }

    public Value _matches(Token function, Stack<Token> stack) {
        String regex = ((Token) stack.pop()).asString();
        String value = ((Token) stack.pop()).asString();
        if (value == null || regex == null)
            return FALSE;
        return value.matches(regex) ? TRUE : FALSE;
    }

    public Value _isResource(Token funciton, Stack<Token> stack) {
        String path = ((Token) stack.pop()).asString();
        if (path == null)
            return FALSE;
        var matcher = RESOURCE_PATTERN.matcher(path);
        return matcher.matches() ? TRUE : FALSE;
    }

    public Value _extract(Token function, Stack<Token> stack) {
        String regex = ((Token) stack.pop()).asString();
        String value = ((Token) stack.pop()).asString();
        if (value == null || regex == null)
            return NULL;
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(value);
        if (!matcher.matches())
            return NULL;
        return new Value().setValue(matcher.group("value"));
    }

    public Value _containsElement(Token function, Stack<Token> stack) {
        String cssQuery = ((Token)stack.pop()).asString();
        Document doc = (Document)((Token)stack.pop()).asObject();
        var elements = doc.select(cssQuery);
        return new Value().setValue(elements.isEmpty()? FALSE: TRUE);
    }

}
