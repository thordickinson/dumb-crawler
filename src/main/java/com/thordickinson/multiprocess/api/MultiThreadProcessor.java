package com.thordickinson.multiprocess.api;

import com.thordickinson.multiprocess.impl.StatsHandler;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MultiThreadProcessor<I, O> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadProcessor.class);
    private static final Logger errorLogger = LoggerFactory.getLogger(MultiThreadProcessor.class.getName() + ".error");

    private ThreadPoolExecutor executor;
    private final DataSource<I> dataSource;
    private final List<ResultHandler<I, O>> resultHandlers = new LinkedList<>();
    private Set<TaskWrapper<I, O>> runningTasks = new HashSet<>();
    private final TaskFactory<I, O> taskFactory;
    private boolean stopped = false;
    private boolean stopRequested = false;
    private long sleepTime = 1000;
    private ProcessingContext currentContext;

    @Getter
    @Setter
    private long taskTimeout = 10000;
    private final Runtime rt = Runtime.getRuntime();
    private Thread loopThread;
    private final String jobId;

    public MultiThreadProcessor(String jobId, DataSource<I> dataSource, TaskFactory<I, O> taskFactory) {
        this(jobId, dataSource, taskFactory, 60_000L);
    }


    public MultiThreadProcessor(String jobId, DataSource<I> dataSource, TaskFactory<I, O> taskFactory, Long logPeriod) {
        this.dataSource = dataSource;
        this.taskFactory = taskFactory;
        addHandler(new StatsHandler<>(logPeriod));
        this.jobId = jobId;
    }

    public void addHandler(ResultHandler<I, O> handler) {
        if (handler != null) resultHandlers.add(handler);
    }

    @Override
    public void run() {
        initialize();
        do {
            runLoop();
        } while (!stopped);
        terminate();
    }

    public void start() {
        loopThread = new Thread(this);
        loopThread.start();
    }

    private void initialize() {
        stopped = false;
        stopRequested = false;
        currentContext = new ProcessingContext(jobId);

        resultHandlers.forEach(h -> h.initialize(currentContext));
        dataSource.initialize(currentContext);

        int threadCount = rt.availableProcessors();
        logger.info("Starting task using {} threads", threadCount);
        executor = new ThreadPoolExecutor(threadCount, threadCount, 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    private void terminate() {
        resultHandlers.forEach(Initialize::terminate);
        dataSource.terminate();
        logger.info("Successfully closed");
    }

    private void runLoop() {
        var completedTasks = getCompletedTasks();
        if (completedTasks.size() > 0) {
            processCompletedTasks(completedTasks);
        }
        scheduleNewTasks();
        sleep();
    }

    private List<TaskResult<I, O>> getCompletedTasks() {
        List<TaskResult<I, O>> results = new LinkedList<>();
        List<TaskWrapper<I, O>> completedTasks = new LinkedList<>();
        for (var task : runningTasks) {
            if (!task.getFuture().isDone())
                continue;
            completedTasks.add(task);
            var wrapped = task.getTask();
            I input = wrapped.getDelegate().getInput();
            String taskId = wrapped.getDelegate().getId();
            try {
                results.add(new TaskResult<>(taskId, input, task.getFuture().get(), Optional.empty(), task.getScheduledAt(), wrapped.getStartedAt(), wrapped.getEndedAt()));
            } catch (Exception ex) {
                results.add(new TaskResult<>(taskId, input,Optional.empty(), Optional.of(ex), task.getScheduledAt(), wrapped.getStartedAt(), wrapped.getEndedAt()));
            }
        }
        runningTasks.removeAll(completedTasks);
        killOldTasks();
        return results;
    }

    private void killOldTasks() {
        for (var task : runningTasks) {
            var time = System.currentTimeMillis() - task.getScheduledAt();
            if (time > taskTimeout) {
                logger.warn("Task is running longer than expected [killing]: {}", String.valueOf(task.getTask()));
                task.getFuture().cancel(true);
            }
        }
    }

    private void processCompletedTasks(Collection<TaskResult<I, O>> completed) {
        for (var result : completed) {
            for (var resultHandler : resultHandlers) {
                resultHandler.handleResult(result);
            }
        }
    }

    private void scheduleNewTasks() {
        var active = executor.getActiveCount();
        var queueSize = executor.getQueue().size();
        currentContext.setCounter("activeTasks", active);
        currentContext.setCounter("taskQueueSize", queueSize);
        if (stopRequested) {
            if (queueSize > 0) {
                logger.info("Removing {} tasks from queue", queueSize);
                executor.getQueue().clear();
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
        List<I> urls = List.of();
        try {
            urls = dataSource.next(toSchedule);
        } catch (IOException ex) {
            this.stopRequested = true;
            logger.error("Error while trying to create new tasks", ex);
            return;
        }
        if (urls.isEmpty() && runningTasks.isEmpty()) {
            logger.debug("No more tasks to schedule and no threads are running.");
            stop();
            return;
        }
        for (I url : urls) {
            var task = taskFactory.createTask(UUID.randomUUID().toString(), url);
            var wrapped = new TaskExecutionWrapper<>(task);
            var future = executor.submit(wrapped);
            runningTasks.add(new TaskWrapper<>(future, wrapped));
        }
    }

    public void stop() {
        stopRequested = true;
        logger.info("Stop requested, waiting for tasks to complete");
        executor.shutdownNow();
        if (executor != null) {
            awaitTermination(1);
        }
        stopped = true;
    }

    private void awaitTermination(int minutes) {
        try {
            executor.awaitTermination(minutes, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            logger.error("Error while waiting for all the tasks to complete", ex);
        }
    }

    private void sleep() {
        try {
            logger.trace("Sleeping...");
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
        }
    }
}


class TaskExecutionWrapper<I, O> implements Callable<Optional<O>>{
    @Getter
    private final ProcessorTask<I, O> delegate;
    @Getter
    private long startedAt = -1;
    @Getter
    private long endedAt = -1;

    TaskExecutionWrapper(ProcessorTask<I, O> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<O> call() throws Exception {
        startedAt = System.currentTimeMillis();
        try{
            return delegate.call();
        }finally {
            endedAt = System.currentTimeMillis();
        }
    }
}
@Data
class TaskWrapper<I, O> {
    private final Future<Optional<O>> future;
    private final TaskExecutionWrapper<I, O> task;
    private final long scheduledAt = System.currentTimeMillis();
}
