package com.thordickinson.dumbcrawler;

import com.thordickinson.dumbcrawler.api.*;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.services.*;
import com.thordickinson.dumbcrawler.services.renderer.ContentRenderer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static com.thordickinson.dumbcrawler.util.HumanReadable.formatBits;
import static com.thordickinson.dumbcrawler.util.HumanReadable.formatDuration;

@Service
public class DumbCrawler implements Runnable {

    private record TaskWrapper(CrawlingTask task, Future<CrawlingResult> future) {
    }

    private static final Logger logger = LoggerFactory.getLogger(DumbCrawler.class);
    private static final Logger errorLogger = LoggerFactory.getLogger(DumbCrawler.class.getName() + ".error");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String TERMINATION_MARKER_FILE = "terminated.marker";


    private URLStore urlStore;
    @Autowired
    private ConfigurableApplicationContext appContext;
    @Autowired
    private URLHasher urlHasher;
    @Autowired
    private UrlTagger urlTagger;
    @Autowired
    private StorageManager storageManager;
    @Autowired
    private ContentRenderer contentRenderer;
    @Autowired
    private LinkFilter linkFilter;
    @Autowired
    private ContentValidator contentValidator;
    @Autowired
    private LinkPrioritizer linkPrioritizer;
    @Autowired
    private TaskKiller taskKiller;

    private boolean stopped = false;
    private ThreadPoolExecutor executor;
    private final Set<TaskWrapper> runningTasks = new HashSet<>();

    private Set<String> seeds = Collections.emptySet();
    private long nextStatisticsPrint = -1;
    private final Runtime rt = Runtime.getRuntime();
    private CrawlingSessionContext sessionContext;

    public void run() {
        initialize();
        do {
            runLoop(this.sessionContext);
        } while (!stopped);
        terminate();
    }

    private void runLoop(CrawlingSessionContext sessionContext) {
        processCompletedTasks(sessionContext);
        scheduleNewTasks(sessionContext);
        printCounters(sessionContext);
        sessionContext.loopCompleted();
        if (taskKiller.shouldStop(sessionContext)) {
            stop();
        } else {
            sleep();
        }
    }

    private void processCompletedTasks(CrawlingSessionContext sessionContext) {
        var completedTasks = new HashSet<TaskWrapper>();
        for (var wrapper : runningTasks) {
            if (wrapper.future().isDone()) {
                processCompletedTask(wrapper, sessionContext);
                completedTasks.add(wrapper);
            }
        }
        runningTasks.removeAll(completedTasks);
    }

    private void handleCrawlingException(CrawlingException ex){
        logger.warn("Error crawling page: {}", ex.getTask().taskId());
        sessionContext.increaseCounter("ERROR_" + ex.getErrorCode());
        urlStore.markTasAsFailed(ex.getTask(), ex);
    }

    private void processCompletedTask(TaskWrapper wrapper, CrawlingSessionContext sessionContext) {
        var future = wrapper.future();
        try {
            var result = future.get();
            var links = result.links();
            if(links.isEmpty()){
                logger.warn("Page does not contains any links: {}", wrapper.task().url());
            }
            saveLinks(links);
            storageManager.storeResult(result, sessionContext);
            sessionContext.increaseCounter("PROCESSED_URLS");
            urlStore.markTaskAsProcessed(result.task());
        } catch (CrawlingException ex) {
            handleCrawlingException(ex);
        } catch (Throwable ex) {
            logger.error("An unexpected error was caught: {} -> {}", wrapper.task().taskId(), wrapper.task().url(), ex);
            var exception = ex instanceof ExecutionException && ex.getCause() != null? ex.getCause() : ex;
            if(exception instanceof  CrawlingException){
                handleCrawlingException((CrawlingException) exception);
                return;
            }
            sessionContext.increaseCounter("ERROR_" + exception.getClass().getSimpleName());
            urlStore.markTasAsFailed(wrapper.task(), ex);
        }
    }

    private CrawlingTask createLinkTask(String link) {
        var tags = urlTagger.tagUrls(link);
        var priority = linkPrioritizer.getPriorityForTag(tags);
        var urlId = urlHasher.hashUrl(link);
        return new CrawlingTask(null, urlId, link, tags, 0, priority);
    }

    private void saveLinks(Collection<String> toAdd) {
        var links = toAdd.stream().map(this::createLinkTask)
                .filter(l -> linkFilter.isURLAllowed(l, sessionContext)).toList();
        logger.debug("Adding {} new links to store", links.size());
        urlStore.addTasks(links);
    }

    private void initialize() {
        var seedTasks = seeds.stream().map(seed -> createTaskParams(seed, "seed")).toList();
        urlStore.addTasks(seedTasks);
    }

    private void terminate() {
        logger.info("Ending crawling session");
        stopComponents();
        sessionContext.destroy();
        awaitTermination();
        appContext.close();
    }

    private void awaitTermination() {
        if(executor == null) return;
        try {
            if(!executor.awaitTermination(1, TimeUnit.MINUTES)){
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            logger.error("Error while waiting for all the tasks to complete", ex);
        }
    }

    private void initializeComponents(CrawlingSessionContext context) {
        storageManager.initialize(context);
        linkFilter.initialize(context);
        contentRenderer.initialize(context);
        urlTagger.initialize(context);
        urlHasher.initialize(context);
        contentValidator.initialize(context);
        linkPrioritizer.initialize(context);
        taskKiller.initialize(context);
    }

    public void start(String jobId) {
        stopped = false;
        var outputDir = getOutputDir(jobId);
        var sessionId = getSessionId(outputDir);
        this.sessionContext = new CrawlingSessionContext(jobId, sessionId, outputDir);
        initializeComponents(sessionContext);
        logger.info("Starting crawling session: {}", sessionContext.getSessionId());

        executor = new ThreadPoolExecutor(sessionContext.getThreadCount(), sessionContext.getThreadCount(), 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        seeds = sessionContext.getSeeds();

        // TODO: once this is started we should load counters in the context.
        urlStore = new URLStore(sessionContext);
        Thread loopThread = new Thread(this, "main-thread");
        this.nextStatisticsPrint = System.currentTimeMillis() + 5000;
        this.stopped = false;
        loopThread.start();
    }

    private Path getOutputDir(String jobId){
        String outDir = System.getenv("CRAWLER_OUT_DIR");
        String userHome = System.getProperty("user.home");
        String basePath = outDir == null? userHome : outDir;
        return Path.of(basePath, ".crawler", "jobs", jobId);
    }

    private String getSessionId(Path outputDir){
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
                        return session.getName();
                    }
                }
            }
        }
        var sessionId = DATETIME_FORMAT.format(new Date());
        logger.info("Creating new session: {}", sessionId);
        return sessionId;
    }

    public void printCounters(CrawlingSessionContext ctx) {

        var now = System.currentTimeMillis();
        if (nextStatisticsPrint > now)
            return;
        long PRINT_TIMERS_TIMEOUT = 1000 * 60;
        nextStatisticsPrint = now + PRINT_TIMERS_TIMEOUT;

        StringBuilder message = new StringBuilder("\nExecution Id: ").append(ctx.getSessionId()).append("\n")
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
        var status = urlStore.getStatus();
        for (var entry : status.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        for (var entry : counters.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if(ctx.isStopRequested()){
            message.append("STOP Requested: waiting for remaining tasks");
        }
        logger.info(message.toString());
    }

    @PreDestroy
    public void stop() {
        logger.info("Stop requested, waiting for tasks to complete");
        if (sessionContext != null) {
            sessionContext.stopCrawling();
        }
    }

    private void stopComponents() {
        storageManager.destroy();
        linkFilter.destroy();
        contentRenderer.destroy();
        urlTagger.destroy();
        urlHasher.destroy();
        contentValidator.destroy();
        linkPrioritizer.destroy();
        taskKiller.destroy();
    }

    private void scheduleNewTasks(CrawlingSessionContext sessionContext) {
        if (sessionContext.isStopRequested()) {
            var active = executor.getActiveCount();
            var queue = executor.getQueue();
            if (!queue.isEmpty()) {
                logger.info("Removing {} tasks from queue", queue.size());
                queue.clear();
            }
            logger.debug("Stop requested: Waiting for {} active tasks", active);
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
        var urls = urlStore.getUnvisited(toSchedule);
        if (urls.isEmpty() && runningTasks.isEmpty()) {
            logger.warn("No more urls to schedule and no threads are running.");
            terminateSession();
            return;
        }
        var newTasks = urls.stream().map(t -> new CrawlingTaskCallable(t, contentRenderer,
                        contentValidator, sessionContext.getSessionDir()))
                .map(c -> new TaskWrapper(c.getTask(), executor.submit(c))).toList();
        runningTasks.addAll(newTasks);
    }

    private void terminateSession(){
        try {
            var flagFile = sessionContext.getSessionDir().resolve(TERMINATION_MARKER_FILE);
            flagFile.toFile().createNewFile();
        } catch (IOException e) {
            logger.error("Unable to mark session as terminated", e);
        }
        stop();
    }

    private CrawlingTask createTaskParams(String url, String... extraTags) {
        final var hash = urlHasher.hashUrl(url);
        var tags = urlTagger.tagUrls(url);
        var allTags = new LinkedList<String>(Arrays.asList(extraTags));
        int priority = linkPrioritizer.getPriorityForTag(tags);
        allTags.addAll(Arrays.asList(tags));
        logger.debug("Tag assignment: {} -> {}", allTags, url);
        return new CrawlingTask("", hash, url, allTags.toArray(new String[0]), 0, priority);
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
