package com.thordickinson.dumbcrawler.api;

public interface Storage {
    String getFormat();
    void saveToFile(String directory, String fileName, String content);
}
