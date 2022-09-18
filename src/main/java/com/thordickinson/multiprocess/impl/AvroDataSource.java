package com.thordickinson.multiprocess.impl;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.thordickinson.multiprocess.api.DataSource;
import lombok.Getter;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AvroDataSource implements DataSource<Any> {

    private static final Logger logger = LoggerFactory.getLogger(AvroDataSource.class);
    private DataFileReader reader;
    private final String jobId;
    private final String executionId;

    @Getter
    private int readCount = 0;

    public AvroDataSource(String jobId, String executionId) {
        this.jobId = jobId;
        this.executionId = executionId;
    }


    private DataFileReader getReader() throws IOException {
        if(reader == null){
            reader = createReader();
        }
        return reader;
    }

    public List<Any> next(int size) throws IOException {
        var fileReader = getReader();
        int count = 0;
        ArrayList<Any> elements = new ArrayList<>(size);
        while(fileReader.hasNext() && count < size){
            var json = JsonIterator.deserialize(fileReader.next().toString());
            elements.add(json);
            count++;
            readCount++;
        }
        return elements;
    }

    private @NotNull DataFileReader createReader() throws IOException {
        var file = Path.of("./data/jobs/%s/executions/%s/avro/%s.avro".formatted(jobId, executionId, executionId)).toFile();
        GenericDatumReader reader
                = new GenericDatumReader<>();
        DataFileReader fileReader = new DataFileReader(file, reader);
        return fileReader;
    }


}
