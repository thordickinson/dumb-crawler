package com.thordickinson.multiprocess.api;

import java.util.Optional;
import java.util.concurrent.Callable;

public interface ProcessorTask<I, O> extends Callable<Optional<O>> {
    String getId();
    I getInput();
}