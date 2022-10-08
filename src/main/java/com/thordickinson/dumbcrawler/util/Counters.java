package com.thordickinson.dumbcrawler.util;

import java.util.HashMap;
import java.util.Map;

public class Counters {
    private final Map<String, Integer> counters = new HashMap<>();
    public void increase(String name, int amount){
        var value = get(name);
        counters.put(name, value + amount);
    }

    public void increase(String name){
        increase(name, 1);
    }

    public int get(String name){
        return counters.getOrDefault(name, 0);
    }

    public Map<String,Integer> toMap(){
        return counters;
    }
}
