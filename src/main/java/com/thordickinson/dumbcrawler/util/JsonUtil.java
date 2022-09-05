package com.thordickinson.dumbcrawler.util;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JsonUtil {
    private JsonUtil() {
    }

    public static Optional<Any> get(Any json, String key) {
        return get(json, Arrays.asList(key.split("\\.")));
    }

    public static Optional<String> getStr(Any json, String key) {
        return get(json, key).map(Any::toString).flatMap(s -> "".equals(s) ? Optional.empty() : Optional.of(s));
    }

    public static Optional<Integer> getInt(Any json, String key) {
        return get(json, key).map(Any::toInt);
    }

    public static Optional<Boolean> getBool(Any json, String key) {
        return get(json, key).map(Any::toBoolean);
    }

    public static List<Any> getList(Any json, String key) {
        return get(json, key).map(Any::asList).orElseGet(Collections::emptyList);
    }

    public static Optional<Long> getLong(Any json, String key) {
        return get(json, key).map(Any::toLong);
    }

    public static Optional<Double> getDouble(Any json, String key) {
        return get(json, key).map(Any::toDouble);
    }

    public static Optional<Any> get(Any json, List<String> path) {
        if (path.isEmpty()) return Optional.of(json);
        var next = path.get(0);
        if (!json.keys().contains(next)) {
            return Optional.empty();
        }
        var nextValue = json.get(next);
        return get(nextValue, path.subList(1, path.size()));
    }
}
