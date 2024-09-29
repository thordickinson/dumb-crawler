package com.thordickinson.dumbcrawler;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import com.thordickinson.dumbcrawler.api.*;
import com.thordickinson.dumbcrawler.services.CrawlingTask;
import com.thordickinson.dumbcrawler.services.URLStore;
import com.thordickinson.dumbcrawler.services.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import static com.thordickinson.dumbcrawler.util.HumanReadable.*;

@Service
public class DumbCrawler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DumbCrawler.class);
    private static final Logger errorLogger = LoggerFactory.getLogger(DumbCrawler.class.getName() + ".error");

    private URLStore store;
    @Autowired
    private ConfigurableApplicationContext appContext;
    @Autowired
    private List<CrawlingResultHandler> resultHandlers = Collections.emptyList();
    @Autowired
    private List<ContentValidator> contentValidators = Collections.emptyList();
    @Autowired
    private HtmlRenderer renderer;
    @Autowired
    private StorageManager storageManager;

    private boolean stopped = false;
    private ThreadPoolExecutor executor;
    private final Set<Future<CrawlingResult>> runningTasks = new HashSet<>();
    private CrawlingContext crawlingContext;

    private Set<String> seeds = Collections.emptySet();
    private long nextStatisticsPrint = -1;
    private final Runtime rt = Runtime.getRuntime();

    @Override
    public void run() {
        initialize();
        do {
            runLoop();
        } while (!stopped);
        terminate();
    }

    private void runLoop() {
        var completedTasks = getCompletedTasks();
        if (!completedTasks.isEmpty()) {
            processCompletedTasks(completedTasks);
        }

        scheduleNewTasks();
        printCounters();
        sleep();
    }

    private Set<CrawlingResult> getCompletedTasks() {
        Set<CrawlingResult> results = new HashSet<>();
        Set<Future<CrawlingResult>> completedTasks = new HashSet<>();
        for (Future<CrawlingResult> task : runningTasks) {
            if (!task.isDone())
                continue;
            completedTasks.add(task);
            try {
                results.add(task.get());
            } catch (Exception ex) {
                logger.error("Unhandled execution error", ex);
            }
        }
        logger.debug("{} tasks completed", completedTasks.size());
        runningTasks.removeAll(completedTasks);
        return results;
    }

    private void processCompletedTasks(Set<CrawlingResult> completed) {

        completed.forEach(c -> {
            var counters = c.page().counters();
            counters.forEach((k, v) -> crawlingContext.increaseCounter(k, v));
        });
        var failed = completed.stream().filter(c -> c.error().isPresent()).toList();
        store.setFailed(failed.stream().map(r -> r.page().originalUrl()).collect(Collectors.toSet()));
        crawlingContext.increaseCounter("failedTasks", failed.size());
        var errors = failed.stream().map(c -> c.error().get()).map(e -> "error." + e.getClass().getName())
                .collect(Collectors.toSet());
        errors.forEach(e -> crawlingContext.increaseCounter(e));
        failed.forEach(f -> {
            errorLogger.warn("Error getting url {}", f.page().originalUrl(), f.error().get());
        });

        var success = completed.stream().filter(c -> c.error().isEmpty()).toList();
        store.setVisited(success.stream().map(r -> r.page().originalUrl()).collect(Collectors.toSet()));
        var links = success.stream().flatMap(c -> c.links().stream()).collect(Collectors.toSet());
        store.addURLs(links);
        for (var result : success) {
            for (var resultHandler : resultHandlers) {
                resultHandler.handleCrawlingResult(result);
            }
        }
    }

    private void initialize() {
        store.addSeeds(seeds);
    }

    private void terminate() {
        logger.info("Ending crawling session");
        resultHandlers.forEach(CrawlingComponent::destroy);
        executor.shutdownNow();
    }

    private void awaitTermination(int minutes) {
        try {
            executor.awaitTermination(minutes, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            logger.error("Error while waiting for all the tasks to complete", ex);
        }
    }

    private Set<String> getUrls(int quantity) {
        return store.getUnvisited(quantity);
    }

    public void start(String jobId, Optional<String> executionId) {
        stopped = false;
        crawlingContext = new CrawlingContext(jobId, executionId);
        logger.info("Starting crawling session: {}", crawlingContext.getExecutionId());
        resultHandlers.forEach(h -> h.initialize(crawlingContext));
        contentValidators.forEach(t -> t.initialize(crawlingContext));
        

        executor = new ThreadPoolExecutor(crawlingContext.getThreadCount(), crawlingContext.getThreadCount(), 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        seeds = crawlingContext.getSeeds();

        // TODO: once this is started we should load counters in the context
        store = new URLStore(crawlingContext);
        Thread loopThread = new Thread(this, "main-thread");
        this.nextStatisticsPrint = System.currentTimeMillis() + 5000;
        this.stopped = false;
        loopThread.start();
    }

    public void printCounters() {

        var now = System.currentTimeMillis();
        if (nextStatisticsPrint > now)
            return;
        long PRINT_TIMERS_TIMEOUT = 1000 * 60;
        nextStatisticsPrint = now + PRINT_TIMERS_TIMEOUT;
        var ctx = this.crawlingContext;

        StringBuilder message = new StringBuilder("\nExecution Id: ").append(ctx.getExecutionId()).append("\n")
                .append("Time since start: ")
                .append(formatDuration(Duration.ofMillis(now - ctx.getStartedAt()))).append("\n");
        var max = rt.maxMemory();
        var total = rt.totalMemory();
        var free = rt.freeMemory();
        var used = total - free;
        var percent = Math.round((((double) used * 100D) / (double) max));
        message.append(
                "Memory: Max: %s | Total %s | Free: %s | Used: %s [%d%%]\n".formatted(formatBits(max),
                        formatBits(total),
                        formatBits(free), formatBits(used), percent));
        var counters = ctx.getCounters();
        if (counters.isEmpty()) {
            message.append("No counters to display");
        }
        var status = store.getStatus();
        for (var entry : status.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        for (var entry : counters.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        logger.info(message.toString());
    }

    @PreDestroy
    public void stop() {
        logger.info("Stop requested, waiting for tasks to complete");
        crawlingContext.stopCrawling();
        if (executor != null) {
            awaitTermination(10);
        }
        appContext.close();
    }

    private void scheduleNewTasks() {
        if (crawlingContext.isStopRequested()) {
            var active = executor.getActiveCount();
            var queue = executor.getQueue();
            if (!queue.isEmpty()) {
                logger.info("Removing {} tasks from queue", queue.size());
                queue.clear();
            }
            logger.info("Stop requested: Waiting for {} active tasks", active);
            if (active == 0) {
                this.stopped = true;
            }
            return;
        }
        int size = executor.getQueue().size();
        int maxQueuedTasks = executor.getMaximumPoolSize() * 2;
        int scheduleLimit = (int) Math.round(executor.getMaximumPoolSize() * 1.5);
        if (size >= scheduleLimit) {
            logger.debug("No tasks to add, current tasks {}", size);
            return;
        }

        int toSchedule = maxQueuedTasks - size;
        var urls = getUrls(toSchedule);
        if (urls.isEmpty() && runningTasks.isEmpty()) {
            logger.debug("No more urls to schedule and no threads are running.");
            stop();
            return;
        }
        var newTasks = urls.stream().map(u -> new CrawlingTask(u, contentValidators))
                .map(executor::submit).collect(Collectors.toSet());
        runningTasks.addAll(newTasks);
    }


    private void sleep() {
        try {
            logger.trace("Sleeping...");
            long sleepTime = 1000;
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            //Exception is ignored
        }
    }

}
