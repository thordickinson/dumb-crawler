package com.thordickinson.multiprocess.meli;

import com.jsoniter.any.Any;

import static com.thordickinson.dumbcrawler.util.JsonUtil.*;

import com.thordickinson.multiprocess.api.ProcessingContext;
import com.thordickinson.multiprocess.api.ResultHandler;
import com.thordickinson.multiprocess.api.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FailedTaskHandler<T> implements ResultHandler<Any, T> {

    private static final Logger logger = LoggerFactory.getLogger(FailedTaskHandler.class);
    private Path root;

    @Override
    public void initialize(ProcessingContext context) {
        root = context.getExecutionDir().resolve("errors");
    }

    @Override
    public void handleResult(TaskResult<Any, T> result) {
        if (result.error().isPresent()) {
            var error = result.error().get();
            saveErrorContent(result.taskId(),  result.input(), error);
        }
    }

    private void saveErrorContent(String taskId, Any input, Throwable error) {
        var data = get(input, "content").map(Any::toString);
        if (data.isEmpty()) {
            logger.error("Task failed with no content: {}", taskId);
            return;
        }
        try {

            var file = root.resolve(error.getClass().getSimpleName())
                    .resolve(taskId + ".html");
            file.getParent().toFile().mkdirs();
            logger.error("Saving error to {}", file.toAbsolutePath());
            Files.write(file, data.get().getBytes());
        } catch (IOException ex) {
            logger.error("Error writing failed result");
        }
    }
}
