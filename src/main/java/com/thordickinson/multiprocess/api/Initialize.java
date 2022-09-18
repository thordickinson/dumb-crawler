package com.thordickinson.multiprocess.api;

public interface Initialize {
    default void initialize(ProcessingContext context) {}
    default void terminate() {}
}
