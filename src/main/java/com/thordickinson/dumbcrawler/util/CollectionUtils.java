package com.thordickinson.dumbcrawler.util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionUtils {
    public static <K, V, T> Map<K, T> mapValues(Map<K, V> original, Function<V, T> mapper){
        return original.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> mapper.apply(e.getValue())));
    }
}
