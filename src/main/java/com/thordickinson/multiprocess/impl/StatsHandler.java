package com.thordickinson.multiprocess.impl;

import static com.thordickinson.dumbcrawler.util.HumanReadable.*;

import com.thordickinson.dumbcrawler.util.HumanReadable;
import com.thordickinson.multiprocess.api.ProcessingContext;
import com.thordickinson.multiprocess.api.ResultHandler;
import com.thordickinson.multiprocess.api.TaskResult;
import org.apache.hadoop.fs.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class StatsHandler<A, B> implements ResultHandler<A, B> {
    private Long startedAt;
    private static final Logger logger = LoggerFactory.getLogger(StatsHandler.class);
    private final Runtime rt = Runtime.getRuntime();
    private ProcessingContext context;
    private long nextStatisticsPrint = -1;
    private long totalTime = 0;
    private long totalSuccess = 0;
    private int successfulTasks = 0;
    private long max = -1;

    private final long frequency;

    public StatsHandler(long frequency){
        this.frequency = frequency;
    }
    public StatsHandler(){
        this(60_000);
    }

    @Override
    public void initialize(ProcessingContext ctx) {
        this.context = ctx;
        nextStatisticsPrint = System.currentTimeMillis() + 1000;
        totalTime = 0;
    }

    @Override
    public void handleResult(TaskResult<A, B> result) {

        context.increaseCounter("completedTasks");
            var taskDuration =result.endedAt() - result.startedAt();
            totalTime += taskDuration;
        if(result.error().isPresent()) {
            context.increaseCounter("failedTasks");
            context.increaseCounter(result.error().get().getClass().getName());

        }
        else {
            context.increaseCounter("successfulTasks");
            totalSuccess += taskDuration;
            successfulTasks++;
            var average = totalSuccess / successfulTasks;
            context.setCounter("averageTime", HumanReadable.formatDuration(Duration.ofMillis(average)));
        }
            context.setCounter("totalTime", HumanReadable.formatDuration(Duration.ofMillis(totalTime)));
        printStats();
    }

    @Override
    public void terminate() {
        ResultHandler.super.terminate();
    }


    private void printStats(){
        var now = System.currentTimeMillis();
        if (nextStatisticsPrint > now)
            return;
        nextStatisticsPrint = now + frequency;
        var ctx = this.context;


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

}
