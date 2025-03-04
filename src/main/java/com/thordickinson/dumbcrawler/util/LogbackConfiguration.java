package com.thordickinson.dumbcrawler.util;

import java.nio.file.Path;

import static com.thordickinson.dumbcrawler.util.Misc.getUserHome;


public class LogbackConfiguration {

    public static void setLogbackConfigurationFile() {
        final var userHome = getUserHome();
        final var logbackFile = Path.of(userHome).resolve(".apricoot").resolve("crawler").resolve("logback.xml");
        if(logbackFile.toFile().isFile()){
            System.out.println("Using logback configuration from user dir");
            System.setProperty("logging.config", logbackFile.toString());
        }else{
            System.out.println("Using default logback configuration");
            System.setProperty("logging.config", "./conf/logback.xml");
        }
    }
}
