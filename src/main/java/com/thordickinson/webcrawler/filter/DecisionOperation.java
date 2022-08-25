package com.thordickinson.webcrawler.filter;


import java.util.Optional;

public interface DecisionOperation {
    String getKey();
    boolean evaluate(Optional<Object> value, Optional<String> argument);
}
