package com.thordickinson.dumbcrawler.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LoggerConfig {


    public static void addFileAppender(Path crawlingPath, Class... loggers){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("timestamp");
        // set the file name
        fileAppender.setFile(crawlingPath.resolve("crawling.log").toString());

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%r %thread %level - %msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();
        for(var logger : loggers){
            // attach the rolling file appender to the logger of your choice
            Logger logbackLogger = loggerContext.getLogger(logger.getName());
            logbackLogger.addAppender(fileAppender);
        }
    }
}
