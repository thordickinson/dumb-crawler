package com.thordickinson.webcrawler.filter;

import com.jsoniter.any.Any;
import com.thordickinson.webcrawler.api.CrawlingContext;
import com.thordickinson.webcrawler.util.JsonUtil;

import static com.thordickinson.webcrawler.util.JsonUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Decider(Optional<String> field, Decision action, String operator, Optional<String> argument) {

    public static Decider fromJSON(Any json){
        var operator = getStr(json, "operator").orElseGet(() -> "matchAll");
        var action = getStr(json, "action").map(k -> Decision.valueOf(k.toUpperCase())).orElseThrow(() -> new IllegalArgumentException("Action is required"));
        var field = getStr(json, "field");
        if(field.isEmpty() && !"matchAll".equals(operator)){
            throw new IllegalArgumentException("Field is required for all operations but matchAll");
        }
        var argument = getStr(json, "argument");
        return new Decider(field, action, operator, argument);
    }

    public static List<Decider> fromJSONArray(Any jsonArray){
        return jsonArray.asList().stream().map(Decider::fromJSON).collect(Collectors.toList());
    }
}
