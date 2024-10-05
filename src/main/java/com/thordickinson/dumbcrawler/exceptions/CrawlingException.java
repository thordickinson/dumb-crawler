package com.thordickinson.dumbcrawler.exceptions;

import com.thordickinson.dumbcrawler.api.CrawlingTask;
import lombok.Getter;

@Getter
public class CrawlingException extends RuntimeException {
    private final CrawlingTask task;
    private final String errorCode;
    private final boolean shouldRetry;


    public CrawlingException(CrawlingTask task, String errorCode, String message, boolean shouldRetry, Throwable cause){
        super(message, cause);
        this.task = task;
        this.errorCode = errorCode;
        this.shouldRetry = shouldRetry;
    }

    public CrawlingException(CrawlingTask task, String errorCode, String message, boolean shouldRetry){
        this(task, errorCode, message, shouldRetry, null);
    }

    public CrawlingException(CrawlingTask task, String errorCode, boolean shouldRetry){
        this(task, errorCode, null, shouldRetry, null);
    }

    public String toString(){
        return "%s: %s".formatted(getErrorCode(), getMessage());
    }
}
