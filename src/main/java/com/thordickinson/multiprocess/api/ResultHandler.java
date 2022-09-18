package com.thordickinson.multiprocess.api;

public interface ResultHandler<I, O> extends Initialize {
    void handleResult(TaskResult<I, O> result);
}
