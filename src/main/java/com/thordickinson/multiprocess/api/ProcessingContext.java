package com.thordickinson.multiprocess.api;

import lombok.Getter;

import java.io.Serializable;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProcessingContext {
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");


    private final Map<String, Serializable> counters = new HashMap<>();
    @Getter
    private final Long startedAt = System.currentTimeMillis();
    @Getter
    private final String executionId = DATETIME_FORMAT.format(new Date());
    @Getter
    private final String jobId;
    @Getter
    private final Path executionDir;

    @Getter
    private final Path jobDir;

    public ProcessingContext(String jobId) {
        this.jobId = jobId;
        this.jobDir = Path.of("./data/jobs").resolve(jobId);
        this.executionDir = jobDir.resolve("executions").resolve(this.executionId);
    }

    public int increaseCounter(String key) {
        return this.increaseCounter(key, 1);
    }

    public int increaseCounter(String key, int amount) {
        int count = (int) this.counters.getOrDefault(key, 0);
        count += amount;
        this.counters.put(key, count);
        return count;
    }

    public void setCounter(String key, Serializable value) {
        this.counters.put(key, value);
    }

    public Map<String, Serializable> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

}
