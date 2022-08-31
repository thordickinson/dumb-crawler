package com.thordickinson.dumbcrawler.services;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;
import static com.thordickinson.webcrawler.util.HumanReadable.*;

@Service
public class DumbCrawler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DumbCrawler.class);

    private URLStore store;
    @Autowired
    private ConfigurableApplicationContext appContext;
    @Autowired
    private List<CrawlingResultHandler> resultHandlers = Collections.emptyList();

    private boolean stopped = false;
    private long sleepTime = 1000;
    private ThreadPoolExecutor executor;
    private Thread loopThread;
    private Set<Future<CrawlingResult>> runningTasks = new HashSet<>();
    private CrawlingContext crawlingContext;

    private Set<String> seeds = Collections.emptySet();
    private long nextStatisticsPrint = -1;
    private static long PRINT_TIMERS_TIMEOUT = 1000 * 60;
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
        if (completedTasks.size() > 0) {
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
        var links = completed.stream().flatMap(c -> c.links().stream()).collect(Collectors.toSet());
        store.addURLs(links);
        store.setVisited(completed.stream().map(CrawlingResult::requestedUrl).collect(Collectors.toSet()));
        for (var result : completed) {
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
        shutdownExecutor(executor);
        logger.info("Terminated");
        resultHandlers.forEach(r -> r.destroy());
        appContext.close();
    }

    private static void shutdownExecutor(ThreadPoolExecutor executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
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
        executor = new ThreadPoolExecutor(crawlingContext.getThreadCount(), crawlingContext.getThreadCount(), 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        seeds = crawlingContext.getSeeds();

        // TODO: once this is started we should load counters in the context
        store = new URLStore(crawlingContext);
        loopThread = new Thread(this, "main-thread");
        this.nextStatisticsPrint = System.currentTimeMillis() + 5000;
        this.stopped = false;
        loopThread.start();
    }

    public void printCounters() {

        var now = System.currentTimeMillis();
        if (nextStatisticsPrint > now)
            return;
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
        for (var entry : counters.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        logger.info(message.toString());
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping ");
        this.stopped = true;
    }

    private void scheduleNewTasks() {
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
        var newTasks = urls.stream().map(CrawlingTask::new).map(executor::submit).collect(Collectors.toSet());
        runningTasks.addAll(newTasks);
    }

    private void sleep() {
        try {
            logger.trace("Sleeping...");
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
        }
    }

}
