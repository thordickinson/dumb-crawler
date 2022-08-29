package com.thordickinson.webcrawler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.TypeDescription;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WARCMigrator {

    private static final int BATCH_SIZE = 2048;

    public void migrate(String path) throws IOException {
        Path root = Path.of("./data/jobs/meli/executions/%s/warc".formatted(path));
        var rootDir = root.toFile();
        for (var file : rootDir.listFiles()) {
            if (file.getName().endsWith(".warc")) {
                migrateFile(file.toPath());
            }
        }
    }

    public void read(String path) throws IOException {
        Path root = Path.of("./data/jobs/meli/executions/%s/orc".formatted(path));
        var rootDir = root.toFile();
        for (var file : rootDir.listFiles()) {
            if (file.getName().endsWith(".orc")) {
                System.out.println(read(new Configuration(), file.getAbsolutePath(), System.out::println));
            }
        }
    }

    public static List<Map<String, Object>> read(Configuration configuration, String path, Consumer<Object> consumer)
            throws IOException {
        // Create a list to collect rows
        List<Map<String, Object>> rows = new LinkedList<>();

        // Create an ORC reader using the Hadoop fileSystem and path
        try (Reader reader = OrcFile.createReader(new org.apache.hadoop.fs.Path(path),
                OrcFile.readerOptions(configuration))) {
            // Extract the schema
            TypeDescription schema = reader.getSchema();

            try (RecordReader records = reader.rows(reader.options())) {
                // Read rows in batch for better performance.
                VectorizedRowBatch batch = reader.getSchema().createRowBatch(BATCH_SIZE);

                TimestampColumnVector timestampColumn = (TimestampColumnVector) batch.cols[0];
                BytesColumnVector urlColumn = (BytesColumnVector) batch.cols[1];
                BytesColumnVector contentColumn = (BytesColumnVector) batch.cols[2];

                while (records.nextBatch(batch)) {
                    for (int rowNum = 0; rowNum < batch.size; rowNum++) {
                        var url = urlColumn.toString(rowNum);
                        var content = contentColumn.toString(rowNum);
                        consumer.accept(
                                List.of(timestampColumn.asScratchTimestamp(rowNum), url, content.length()));
                    }
                }
            }
        }
        return rows;
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
            try (var writer = OrcFile.createWriter(target,
                    OrcFile.writerOptions(configuration).setSchema(schema))) {
                for (WarcRecord record : reader) {
                    if (record instanceof WarcResponse) {
                        WarcResponse response = (WarcResponse) record;
                        // System.out.println("Writing: " + response.target());

                        int rowNum = batch.size++;
                        timestampColumn.set(rowNum, Timestamp.from(record.date()));
                        response.body().read(buffer);
                        var urlBytes = response.target().getBytes(StandardCharsets.UTF_8);
                        urlColumn.setRef(rowNum, urlBytes, 0, urlBytes.length);
                        contentColumn.setVal(rowNum, buffer.array());

                        if (batch.size == batch.getMaxSize()) {
                            writer.addRowBatch(batch);
                            batch.reset();
                        }

                    }
                }
            }
        }

    }

    public static void main(String[] args) throws IOException {
        // new WARCMigrator().migrate("1661552637911");
        new WARCMigrator().read("1661784930324");
    }
}
