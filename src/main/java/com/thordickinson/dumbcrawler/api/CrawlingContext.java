package com.thordickinson.dumbcrawler.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.util.JsonUtil;

import lombok.Getter;

public class CrawlingContext {

    @Getter
    private final Path jobDir;
    @Getter
    private final String jobId;
    @Getter
    private final String executionId;
    @Getter
    private final Any jobDescriptor;
    @Getter
    private final Path executionDir;
    private final Map<String, Integer> counters = new HashMap<>();
    @Getter
    private final long startedAt = System.currentTimeMillis();

    public CrawlingContext(String jobId, Optional<String> executionId) {
        this.jobId = jobId;
        this.executionId = executionId.orElseGet(() -> String.valueOf(System.currentTimeMillis()));
        jobDir = Path.of("./data/jobs").resolve(jobId);
        jobDescriptor = loadJob(jobDir);
        executionDir = jobDir.resolve("executions").resolve(this.executionId);
        executionDir.toFile().mkdirs();
    }

    public Set<String> getSeeds() {
        return JsonUtil.get(jobDescriptor, "seeds").map(Any::asList).map(l -> l.stream().map(Any::toString)
                .collect(Collectors.toSet())).orElse(Collections.emptySet());
    }

    public int getThreadCount() {
        return JsonUtil.get(jobDescriptor, "threadCount").map(Any::toInt).orElseGet(() -> 3);
    }

    public Optional<Any> getConfig(String path) {
        return JsonUtil.get(jobDescriptor, path);
    }

    private Any loadJob(Path dataDir) {
        var configFile = dataDir.resolve("job.json");
        try {
            return JsonIterator.deserialize(Files.readAllBytes(configFile));
        } catch (IOException ex) {
            throw new RuntimeException("Error reading config file: " + configFile, ex);
        }
    }

    public int increaseCounter(String key) {
        return this.increaseCounter(key, 1);
    }

    public int increaseCounter(String key, int amount) {
        int count = this.counters.getOrDefault(key, 0);
        count += amount;
        this.counters.put(key, count);
        return count;
    }

    public Map<String, Integer> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

}
