package com.thordickinson.multiprocess.api;

import java.util.Optional;
import java.util.function.Function;

public record TaskResult<I, O>(String taskId, I input, Optional<O> result, Optional<Throwable> error, long scheduledAt, long startedAt, long endedAt){
    public <V> TaskResult<I, V> map(Function<O, V> mapper){
        return new TaskResult<I, V>(taskId, input, result.map(v -> mapper.apply(v)), error, scheduledAt, startedAt, endedAt);
    }
}