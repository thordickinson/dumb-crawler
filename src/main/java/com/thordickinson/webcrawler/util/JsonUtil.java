package com.thordickinson.webcrawler.util;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JsonUtil {
    private JsonUtil() {
    }

    public static Optional<Any> get(Any json, String key) {
        return get(json, Arrays.asList(key.split("\\.")));
    }

    public static Optional<Any> get(Any json, List<String> path){
        if(path.isEmpty()) return Optional.of(json);
        var next = path.get(0);
        try{
            var nextValue = json.get(next);
            return get(nextValue, path.subList(1, path.size()));
        }catch (JsonException ex){
            return Optional.empty();
        }

    }
}
