package com.thordickinson.dumbcrawler.services.storage;

import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.util.ConfigurationSupport;
import org.apache.orc.OrcConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.TypeDescription;
import org.apache.orc.Writer;

import com.thordickinson.dumbcrawler.api.CrawlingContext;
import com.thordickinson.dumbcrawler.api.CrawlingResult;
import com.thordickinson.dumbcrawler.api.CrawlingResultHandler;
import com.thordickinson.dumbcrawler.util.HumanReadable;

@Service
public class ORCResultHandler extends AbstractCrawlingComponent implements CrawlingResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(ORCResultHandler.class);

    private final TypeDescription schema = TypeDescription
            .fromString("struct<timestamp:timestamp,url:string,content:string>");
    private ConfigurationSupport configuration;

    private org.apache.hadoop.fs.Path target;
    private Writer writer;
    private TimestampColumnVector timestampColumn;
    private BytesColumnVector urlColumn;
    private BytesColumnVector contentColumn;
    private VectorizedRowBatch batch;
    private int stripeSize = -1;

    public ORCResultHandler() {
        super("orcStorage");
    }

    @Override
    public void handleCrawlingResult(CrawlingResult result) {
        if (!isEnabled()) return;
        if (!evaluate(result.page().url())) {
            increaseCounter("ignoredPages");
            logger.trace("Ignoring url: {}", result.page().url());
            return;
        }
        logger.debug("Handling result: {}", result.page().url());
        increaseCounter("storedPages");
        try {
            write(result);
        } catch (Exception ex) {
            logger.error("Error writing page: {}", result.requestedUrl(), ex);
        }
    }

    @Override
    protected void loadConfigurations(CrawlingContext context) {
        try {
            tryInitialize();
        } catch (Exception ex) {
            throw new RuntimeException("Error initializing orc file", ex);
        }
    }

    private void tryInitialize() throws IOException {

        var context = getContext();
        var directory = context.getExecutionDir().resolve("orc");
        var fileName = "%s".formatted(context.getExecutionId());

        Path filePath = directory.resolve(fileName + ".orc");
        int counter = 1;

        while (filePath.toFile().isFile())
            filePath = directory.resolve(fileName + "_" + String.valueOf(counter++) + ".orc");
        this.target = new org.apache.hadoop.fs.Path(filePath.toString());

        var MB = 1024 * 1024;
        stripeSize = 20 * MB;

        var configuration = new Configuration();
        configuration.set(OrcConf.ROWS_BETWEEN_CHECKS.getAttribute(), String.valueOf(100));
        var options = OrcFile.writerOptions(configuration)
                .stripeSize(stripeSize)
                .setSchema(schema);

        writer = OrcFile.createWriter(target, options);
        batch = schema.createRowBatch(5);
        timestampColumn = (TimestampColumnVector) batch.cols[0];
        urlColumn = (BytesColumnVector) batch.cols[1];
        contentColumn = (BytesColumnVector) batch.cols[2];
    }

    public void write(CrawlingResult page) throws IOException {

        if (page.page().content().isEmpty()) {
            logger.warn("Empty content received: {} -> {}", page.page().resultCode(), page.requestedUrl());
            return;
        }

        int rowNum = batch.size++;
        timestampColumn.set(rowNum, Timestamp.from(Instant.now()));
        var urlBytes = page.requestedUrl().getBytes(StandardCharsets.UTF_8);
        urlColumn.setRef(rowNum, urlBytes, 0, urlBytes.length);
        var content = page.page().content().get();
        contentColumn.setVal(rowNum, content.getBytes(StandardCharsets.UTF_8));
        setCounter("orcWriterMemory", HumanReadable.formatBits(writer.estimateMemory()));

        if (batch.size == batch.getMaxSize()) {
            addRowBatch();
        }
    }

    private void addRowBatch() throws IOException {
        if (batch.size > 0) {
            writer.addRowBatch(batch);
            batch.reset();
        }
    }

    public void destroy() {
        if (writer != null) {
            try {
                logger.info("Closing file writer");
                addRowBatch();
                writer.close();
            } catch (Exception ex) {
                logger.error("Error destroying service", ex);
            }
        }
    }

}
