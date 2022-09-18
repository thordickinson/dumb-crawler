package com.thordickinson.multiprocess.api;

public abstract class AbstractTask<I,O> implements ProcessorTask<I,O>{

    private final I input;
    private final String taskId;

    protected AbstractTask(String taskId, I input) {
        this.input = input;
        this.taskId = taskId;
    }

    @Override
    public String getId() {
        return taskId;
    }

    @Override
    public I getInput() {
        return input;
    }

}
