package com.thordickinson.webcrawler.util;

public class MutableInteger {
    private int val;
    private long lastDisplayTime = 0;

    public MutableInteger(int val) {
        this.val = val;
    }

    public int get() {
        return val;
    }

    public void set(int val) {
        this.val = val;
    }

    public long getLastDisplayTime() {
        return lastDisplayTime;
    }

    public void setLastDisplayTime(long lastDisplayTime) {
        this.lastDisplayTime = lastDisplayTime;
    }

    @Override
    public String toString() {
        return Integer.toString(val);
    }
}
