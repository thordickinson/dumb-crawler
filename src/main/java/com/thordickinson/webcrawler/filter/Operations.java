package com.thordickinson.webcrawler.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

abstract class AbstractDecisionOperation implements DecisionOperation {
    private final String key;
    public AbstractDecisionOperation(String key){
        this.key = key;
    }
    @Override
    public String getKey() {
        return key;
    }
}

@Configuration
public class Operations {

    @Bean
    public DecisionOperation matchAll(){
        return new AbstractDecisionOperation("matchAll") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return true;
            }
        };
    }

    @Bean
    public DecisionOperation contains(){
        return new AbstractDecisionOperation("contains") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> v.contains(a))).orElseGet(() -> false);
            }
        };
    }

    @Bean
    public DecisionOperation notContains(){
        return new AbstractDecisionOperation("notContains") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> !v.contains(a))).orElseGet(() -> true);
            }
        };
    }

    @Bean
    public DecisionOperation startsWith(){
        return new AbstractDecisionOperation("startsWith") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> v.startsWith(a))).orElseGet(() -> false);
            }
        };
    }

    @Bean
    public DecisionOperation notStartsWith(){
        return new AbstractDecisionOperation("notStartsWith") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> !v.startsWith(a))).orElseGet(() -> true);
            }
        };
    }


    @Bean
    public DecisionOperation endsWith(){
        return new AbstractDecisionOperation("endsWith") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> v.endsWith(a))).orElseGet(() -> false);
            }
        };
    }

    @Bean
    public DecisionOperation notEndsWith(){
        return new AbstractDecisionOperation("notEndsWith") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> !v.endsWith(a))).orElseGet(() -> true);
            }
        };
    }

    @Bean
    public DecisionOperation pattern(){
        return new AbstractDecisionOperation("pattern") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                return value.map(String::valueOf).flatMap(v -> argument.map(a -> v.matches(a))).orElseGet(() ->false);
            }
        };
    }

    @Bean
    public DecisionOperation equals(){
        return new AbstractDecisionOperation("equals") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                if(value.isEmpty() && argument.isEmpty()) return true;
                return value.flatMap(v -> argument.map(a -> a.equals(v))).orElseGet(() ->false);
            }
        };
    }

    @Bean
    public DecisionOperation notEquals(){
        return new AbstractDecisionOperation("notEquals") {
            @Override
            public boolean evaluate(Optional<Object> value, Optional<String> argument) {
                if(value.isEmpty() && argument.isEmpty()) return false;
                return value.flatMap(v -> argument.map(a -> !a.equals(v))).orElseGet(() ->true);
            }
        };
    }
}
