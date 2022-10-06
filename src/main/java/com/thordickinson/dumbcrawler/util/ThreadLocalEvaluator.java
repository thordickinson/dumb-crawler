package com.thordickinson.dumbcrawler.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.creativewidgetworks.expressionparser.*;

public class ThreadLocalEvaluator {

    private static final Object UNPARSED = new Object();
    private static final Map<ValueType, Set<Class<?>>> COMPATIBLE_TYPES_MAP = Map.of(ValueType.BOOLEAN,
            Set.of(Boolean.class),
            ValueType.DATE, Set.of(Date.class),
            ValueType.ARRAY, Set.of(List.class),
            ValueType.NUMBER, Set.of(BigDecimal.class),
            ValueType.STRING, Set.of(String.class),
            ValueType.UNDEFINED, Set.of(Void.class));
   
    private static ThreadLocal<ThreadLocalEvaluator> evaluators = new ThreadLocal<>();

    private final Parser parser;

    public ThreadLocalEvaluator(){
        parser = createParser();
    }
    
    public static ThreadLocalEvaluator getThreadEvaluator(){
        var evaluator = evaluators.get();
        if(evaluator == null){
            evaluator = new ThreadLocalEvaluator();
        }
        return evaluator;
    }
    
    private Parser createParser(){
        var parser = new Parser();
        FunctionToolbox.register(parser);
        ExpressionFunctions.register(parser);
        return parser;
    }

    public <T> Optional<T> evaluateAs(Class<T> expectedType, String expression, Map<String,Object> variables){
        parser.clearVariables();
        variables.forEach((k, v) -> parser.addVariable(k, new Value().setValue(v)));
        var result = parser.eval(expression);
        var objectResult = result.asObject();
        if (objectResult instanceof Throwable) {
            throw new RuntimeException("Error evaluating expression: " + expression, (Throwable) objectResult);
        }
        checkResultType(expectedType, result);
        return parseType(expectedType, result);
    }


    
    private static <T> Optional<T> parseType(Class<T> expected, Value result){
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

    private static void checkResultType(Class<?> expected, Value result){
        var compatibleTypes = COMPATIBLE_TYPES_MAP.get(result.getType());
        if(!compatibleTypes.contains(expected)){
            throw new RuntimeException("Expression result is not of the expected type:  Expected: " +
             expected.getSimpleName() + " -  Result: "+ result.getType().name());
        }
    }
}
