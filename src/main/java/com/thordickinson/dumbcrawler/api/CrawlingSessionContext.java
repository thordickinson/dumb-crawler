package com.thordickinson.dumbcrawler.api;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.util.JsonUtil;

import lombok.Getter;

public class CrawlingSessionContext {

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
    @Getter
    private boolean stopRequested = false;
    @Getter
    private final long startedAt = System.currentTimeMillis();
    private final Map<String, Serializable> counters = new HashMap<>();

    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public CrawlingSessionContext(String jobId, Optional<String> executionId) {
        this.jobId = jobId;
        this.executionId = executionId.orElseGet(() -> DATETIME_FORMAT.format(new Date()));
        jobDir = Path.of("./conf/jobs");
        jobDescriptor = loadJob(jobDir);
        executionDir = getOutDir(jobId, this.executionId);
        if(!executionDir.toFile().mkdirs()){
            throw new RuntimeException("Unable to create output dirs");
        }
    }

    private Path getOutDir(String jobId, String executionId){
        String outDir = System.getenv("CRAWLER_OUT_DIR");
        String userHome = System.getProperty("user.home");
        String basePath = outDir == null? userHome : outDir;
        return Path.of(basePath, ".crawler", "jobs", jobId, "executions", executionId, "crawl");
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

    public int getIntConf(String path, int defValue) {
        return getConfig(path).orElse(Any.wrap(defValue)).as(Integer.class);
    }

    public String getStringConf(String path, String defValue) {
        return getConfig(path).orElse(Any.wrap(defValue)).as(String.class);
    }

    public boolean getBoolConf(String path, boolean defValue) {
        return getConfig(path).orElse(Any.wrap(defValue)).as(Boolean.class);
    }

    private Any loadJob(Path dataDir) {
        var configFile = dataDir.resolve(jobId + ".json");
        try {
            return JsonIterator.deserialize(Files.readAllBytes(configFile));
        } catch (IOException ex) {
            throw new RuntimeException("Error reading config file: " + configFile, ex);
        }
    }

    public void increaseCounter(String key) {
        this.increaseCounter(key, 1);
    }

    public void increaseCounter(String key, int amount) {
        int count = (int) this.counters.getOrDefault(key, 0);
        count += amount;
        this.counters.put(key, count);
    }

    public void setCounter(String key, Serializable value) {
        this.counters.put(key, value);
    }

    public Map<String, Serializable> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

    public void stopCrawling() {
        this.stopRequested = true;
    }
}
