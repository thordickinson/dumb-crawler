package com.thordickinson.dumbcrawler.api;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.DumbCrawler;
import com.thordickinson.dumbcrawler.util.JsonUtil;
import com.thordickinson.dumbcrawler.util.LoggerConfig;
import com.thordickinson.dumbcrawler.util.SQLiteConnection;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CrawlingSessionContext {

    @Getter
    private final String jobId;
    @Getter
    private final boolean isNewSession;
    @Getter
    private final String sessionId;
    @Getter
    private final Any jobConfiguration;
    @Getter
    private final Path sessionDir;
    @Getter
    private final Path crawlDir;
    @Getter
    private final Path jobOutputDir;
    @Getter
    private final Path terminationFilePath;
    @Getter
    private boolean stopRequested = false;
    @Getter
    private final long startedAt = System.currentTimeMillis();
    private final Map<String, Serializable> counters = new HashMap<>();
    private final Map<String, Serializable> variables = new HashMap<>();
    @Getter
    private final SQLiteConnection sqLiteConnection;
    private final Logger logger = LoggerFactory.getLogger(CrawlingSessionContext.class);
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String TERMINATION_MARKER_FILE = "terminated.marker";



    public CrawlingSessionContext(String jobId) {
        this.jobId = jobId;
        var home = System.getProperty("user.home");
        if("/".equals(home)){
            // Probablemente está corriendo en docker
            home = System.getenv("DOCKER_USER_HOME");
        }
        this.jobOutputDir = Paths.get(home,".apricoot", "crawler", jobId);
        final var sessionIdOptional = getLatestSession(jobOutputDir);
        this.isNewSession = sessionIdOptional.isEmpty();
        this.sessionId = sessionIdOptional.orElseGet(() -> this.createSessionId());
        this.sessionDir = jobOutputDir.resolve("sessions").resolve(sessionId);
        LoggerConfig.addFileAppender(this.sessionDir, CrawlingSessionContext.class, DumbCrawler.class);

        this.crawlDir = this.sessionDir.resolve("crawl");

        this.terminationFilePath = sessionDir.resolve(TERMINATION_MARKER_FILE);

        jobConfiguration = loadJob(this.jobOutputDir);

        if(!sessionDir.toFile().isDirectory() && !sessionDir.toFile().mkdirs()){
            throw new RuntimeException("Unable to create output dirs");
        }
        sqLiteConnection = new SQLiteConnection(sessionDir);
        initializeTables();
    }

    private void initializeTables(){
        sqLiteConnection.addTable("counters", Map.of("counter_name", "TEXT PRIMARY KEY", "counter_value", "INT DEFAULT 0"));
        var counters = sqLiteConnection.query("SELECT counter_name, counter_value from counters");
        counters.forEach(c -> {
            this.counters.put(String.valueOf(c.get(0)), (int) c.get(1));
        });
    }

    private void saveCounters(){
        for(var entry : counters.entrySet()){
            sqLiteConnection.update("INSERT INTO counters (counter_name, counter_value) VALUES (?, ?) " +
                    "ON CONFLICT(counter_name) DO UPDATE SET counter_value = ?", entry.getKey(),
                    entry.getValue(), entry.getValue());
        }
    }

    public Set<String> getSeeds() {
        return JsonUtil.get(jobConfiguration, "seeds").map(Any::asList).map(l -> l.stream().map(Any::toString)
                .collect(Collectors.toSet())).orElse(Collections.emptySet());
    }

    public int getThreadCount() {
        return JsonUtil.get(jobConfiguration, "threadCount").map(Any::toInt).orElseGet(() -> 3);
    }

    public void loopCompleted(){
        saveCounters();
    }

    public Optional<Any> getConfig(String path) {
        return JsonUtil.get(jobConfiguration, path);
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
        var configFile = dataDir.resolve("config.json");
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

    public void setVariable(String key, Serializable value){
        variables.put(key, value);
    }

    public <T extends  Serializable> T getVariable(String key, T defaultVal){
        return (T) variables.getOrDefault(key, defaultVal);
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

    public void destroy(){
        try {
            sqLiteConnection.close();
        } catch (Exception ex) {
            logger.warn("Error closing connection", ex);
        }
    }

    private Optional<String> getLatestSession(Path outputDir){
        var sessionsDir = outputDir.resolve("sessions").toFile();
        if(sessionsDir.isDirectory()){
            var sessionDirs = sessionsDir.listFiles();
            if(sessionDirs != null){
                var sessions = new ArrayList<>(Arrays.asList(sessionDirs));
                sessions.sort(Comparator.comparing(File::getName));
                var reversed = sessions.reversed();
                for(var session : reversed){
                    if(session.getName().startsWith(".")){
                        continue;
                    }
                    var terminationFile = session.toPath().resolve(TERMINATION_MARKER_FILE);
                    if(!terminationFile.toFile().exists()){
                        logger.info("Resuming session: {}", session.getName());
                        return Optional.of(session.getName());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private String createSessionId(){
        var sessionId = DATETIME_FORMAT.format(new Date());
        logger.info("Creating new session: {}", sessionId);
        return sessionId;
    }


    public void end(){
        try {
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            Files.write(this.terminationFilePath, ("{\"timestamp\": \"" + timestamp + "\" }").getBytes("utf-8"));
            logger.info("Session end marker file was created");
        } catch (IOException e) {
            logger.error("Unable to mark session as terminated", e);
        }
    }
}
