package com.thordickinson.multiprocess.impl;

import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage;
import com.thordickinson.multiprocess.api.ProcessingContext;
import com.thordickinson.multiprocess.api.ResultHandler;
import com.thordickinson.multiprocess.api.TaskResult;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class AvroWriterHandler<T> implements ResultHandler<T, Any> {

    private final Logger logger = LoggerFactory.getLogger(AvroWriterHandler.class);
    private DataFileWriter writer;
    private int processed = 0;

    public AvroWriterHandler() {
    }


    @Override
    public void initialize(ProcessingContext ctx) {
        var file = ctx.getExecutionDir().resolve("out").resolve(ctx.getExecutionId() + ".avro");
        file.getParent().toFile().mkdirs();
        var datumWriter = new GenericDatumWriter<>();
        writer = new DataFileWriter<>(datumWriter);
        writer.setCodec(CodecFactory.bzip2Codec());
        try {
            if (file.toFile().isFile()) {
                writer.appendTo(file.toFile());
            } else {
                writer.create(WebPage.getClassSchema(), file.toFile());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error creating file", ex);
        }
    }

    @Override
    public void terminate() {
        if (writer != null) {
            try {
                logger.info("Closing file");
                writer.close();
            } catch (IOException ex) {
                logger.error("Error closing avro file", ex);
            }
        }
    }

    @Override
    public void handleResult(TaskResult<T, Any> result) {
        if (result.result().isPresent()) {
            try {
                var json = JsonStream.serialize(result.result().get());
                if(processed++ == 0){
                    System.out.println(json);
                }
                writer.appendEncoded(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
               // logger.debug("Processed count: {}", (processed++));
            } catch (IOException ex) {
                throw new RuntimeException("Error writing result");
            }
        }
    }
}
