package com.thordickinson.webcrawler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.TypeDescription;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResponse;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.function.Consumer;

public class WARCMigrator {

    public void migrate(String path){
        Path root = Path.of("./data/jobs/meli/executions/{}".formatted(path));
        for (var file : root.toFile().listFiles()){
            migrateFile(file);
        }

    }


    public void migrateFile(Path file) throws IOException {

        var targetPath = file.getParent().resolve(file.getFileName() + ".orc");
        var target = new org.apache.hadoop.fs.Path(targetPath.toString());
        var struct = "struct<timestamp:timestamp,url:string,content:string>";
        TypeDescription schema = TypeDescription.fromString(struct);
        VectorizedRowBatch batch = schema.createRowBatch();
        Configuration configuration = new Configuration();

        TimestampColumnVector timestampColumn = (TimestampColumnVector) batch.cols[0];
        BytesColumnVector urlColumn = (BytesColumnVector) batch.cols[1];
        BytesColumnVector contentColumn = (BytesColumnVector) batch.cols[2];

        ByteBuffer buffer = ByteBuffer.allocate(100_000);

        try (WarcReader reader = new WarcReader(FileChannel.open(file))) {
            try (var writer = OrcFile.createWriter(target, OrcFile.writerOptions(configuration).setSchema(schema))) {
            for (WarcRecord record : reader) {
                if (record instanceof WarcResponse && record.contentType().base().equals(MediaType.HTTP)) {
                    WarcResponse response = (WarcResponse) record;
                    int rowNum = batch.size++;
                    timestampColumn.set(rowNum, Timestamp.from(record.date()));
                    response.body().read(buffer);
                    urlColumn.setVal(rowNum, buffer.array());
                    if(batch.size == batch.getMaxSize()){
                        writer.addRowBatch(batch);
                        batch.reset();
                    }
                }
            }
        }
        }
    }

    public static void main(String[] args) {
        new WARCMigrator().migrate("1661552637911");
    }
}
