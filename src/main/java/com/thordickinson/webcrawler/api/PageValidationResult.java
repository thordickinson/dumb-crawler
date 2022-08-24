package com.thordickinson.webcrawler.api;

import java.util.Optional;

public record PageValidationResult(PageValidationAction action, Optional<String> message) {
    public PageValidationResult(PageValidationAction action){
        this(action, Optional.empty());
    }
    public static PageValidationResult EMPTY = new PageValidationResult(PageValidationAction.PROCESS);
}
