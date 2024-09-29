package com.thordickinson.multiprocess.meli;

import com.jsoniter.any.Any;
import com.thordickinson.multiprocess.api.ProcessorTask;
import com.thordickinson.multiprocess.api.TaskFactory;
import com.thordickinson.multiprocess.api.MultiThreadProcessor;

import java.text.SimpleDateFormat;

public class MeliDataExtractor implements TaskFactory<Any, Any> {

    private MultiThreadProcessor<Any, Any> parser;

    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");

    public MeliDataExtractor(String jobId, String executionId){
        parser = new MultiThreadProcessor<>("meli-parse", new AvroDataSource(jobId, executionId), this, 10_000L);
        parser.addHandler(new AvroWriterHandler());
        parser.addHandler(new FailedTaskHandler<>());
        parser.setTaskTimeout(3_000);
    }
    public void start(){
        parser.start();
    }

    public void stop(){
        parser.stop();
    }
    @Override
    public ProcessorTask<Any, Any> createTask(String id, Any input) {
        return new ParserTask(id, input);
    }

    public static void main(String[] args) {
        final var extractor = new MeliDataExtractor("meli", "20220905_1142");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping");
            extractor.stop();
        }));
        extractor.start();
    }
}
