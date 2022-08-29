package com.thordickinson.webcrawler.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.TypeDescription;
import org.apache.orc.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.github.slugify.Slugify;
import com.thordickinson.webcrawler.api.AbstractFilteredPageHandler;
import com.thordickinson.webcrawler.api.CrawlingContext;

import edu.uci.ics.crawler4j.crawler.Page;

class OrcPageWriter {
    private final TypeDescription schema = TypeDescription
            .fromString("struct<timestamp:timestamp,url:string,content:string>");

    private final org.apache.hadoop.fs.Path target;
    private Writer writer;
    private TimestampColumnVector timestampColumn;
    private BytesColumnVector urlColumn;
    private BytesColumnVector contentColumn;
    private VectorizedRowBatch batch;
    private boolean initialized = false;

    public OrcPageWriter(Path target) {
        this.target = new org.apache.hadoop.fs.Path(target.toString());
    }

    private void initialize() throws IOException {
        if (initialized)
            return;

        Configuration configuration = new Configuration();
        writer = OrcFile.createWriter(target,
                OrcFile.writerOptions(configuration).setSchema(schema));
        batch = schema.createRowBatch();
        timestampColumn = (TimestampColumnVector) batch.cols[0];
        urlColumn = (BytesColumnVector) batch.cols[1];
        contentColumn = (BytesColumnVector) batch.cols[2];
        initialized = true;
    }

    public void write(Page page) throws IOException {
        initialize();
        int rowNum = batch.size++;
        timestampColumn.set(rowNum, Timestamp.from(Instant.now()));
        var urlBytes = page.getWebURL().toString().getBytes(StandardCharsets.UTF_8);
        urlColumn.setRef(rowNum, urlBytes, 0, urlBytes.length);
        contentColumn.setVal(rowNum, page.getContentData());

        if (batch.size == batch.getMaxSize()) {
            addRowBatch();
            batch.reset();
        }
    }

    private void addRowBatch() throws IOException {
        if (batch.size > 0) {
            writer.addRowBatch(batch);
        }
    }

    public void close() throws IOException {
        if (writer != null) {
            addRowBatch();
            writer.close();
        }
    }
}

@Service
public class ORCStorage extends AbstractFilteredPageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ORCStorage.class);
    private final Slugify slugify = Slugify.builder().lowerCase(true).underscoreSeparator(true).build();
    private final ConcurrentHashMap<Thread, OrcPageWriter> writers = new ConcurrentHashMap<>();

    public ORCStorage() {
        super("orcStorage");
    }

    private OrcPageWriter createWriter(Thread thread, CrawlingContext context) {
        String fileName = slugify.slugify(context.getExecutionId() + "_" + thread.getName());
        var path = context.getExecutionDir().resolve("orc").resolve(fileName + ".orc");
        logger.info("Creating ORC file in {}", path);
        return new OrcPageWriter(path);
    }

    private OrcPageWriter getWriter(CrawlingContext context) {
        var local = writers.computeIfAbsent(Thread.currentThread(), (t) -> createWriter(t, context));
        return local;
    }

    @Override
    protected void handleFilteredPage(Page page, CrawlingContext context) throws Exception {
        context.increaseCounter("storedPages");
        var writer = getWriter(context);
        writer.write(page);
    }

    @Override
    protected void onPageRejected(Page page, CrawlingContext ctx) {
    }

    @PreDestroy
    void close() {
        writers.values().forEach(w -> {
            try {
                w.close();
            } catch (IOException ex) {
                logger.warn("Error closing file", ex);
            }
        });
    }

}
