package com.thordickinson.multiprocess.api;

public interface TaskFactory<I,O> {
    ProcessorTask<I,O> createTask(String id, I input);
}