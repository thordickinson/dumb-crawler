package com.thordickinson.multiprocess.meli;

import com.jsoniter.any.Any;
import static com.thordickinson.dumbcrawler.util.JsonUtil.*;
import com.thordickinson.multiprocess.api.AbstractTask;
import org.bson.types.ObjectId;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MatchTask extends AbstractTask<Any, Any> {

    private final Map<String, String> makeCache = new HashMap<>();

    protected MatchTask(String taskId, Any input) {
        super(taskId, input);
    }


    public Optional<ObjectId> findMakeId(Map<String,Any> make){
        return null;
    }

    public Optional<ObjectId> findModel(ObjectId makeId, Integer year, Map<String, Any> model){
        return null;
    }

    private Optional<ObjectId> getInputModel(){
        var input = getInput();
        var make = get(input, "modelInfo.brand").map(Any::asMap).flatMap(this::findMakeId);
        if(make.isEmpty()) return Optional.empty();

        var year = get(input, "modelInfo.vehicle_year.name").map(Any::toString).flatMap(this::tryParse);
        if(year.isEmpty()) return Optional.empty();

        var model = get(input, "modelInfo.model").map(Any::asMap)
                .flatMap(m -> findModel(make.get(), year.get(), m));
        return model;
    }
    private Optional<Map<String, Any>> getData(){
        return Optional.empty();
    }


    private Map<String, Any> getInformation(ObjectId modelId){
        var input = getInput();
        var timestamp = get(input, "basic.timestamp");
        var externalId = get(input, "basic.externalId");
        var price = get(input, "price.value");
        var url = get(input, "basic.url");
        var id = externalId.map( i -> Any.wrap(generateId(i.toString())));

        return Map.of("externalId", externalId,
                        "modelId", Optional.of(Any.wrap(modelId.toString())),
                        "timestamp", timestamp,
                        "price", price,
                        "url", url )
                .entrySet().stream().filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue().get()));
    }

    @Override
    public Optional<Any> call() throws Exception {
        var modelId = getInputModel();
        return modelId.map(this::getInformation).map(Any::wrap);
    }

    private static String generateId(String value){
        String myHash = DigestUtils.md5DigestAsHex(value.getBytes());
        return myHash.substring(0, 24);
    }



    private Optional<Integer> tryParse(String value){
        try{
            return Optional.of(Integer.parseInt(value));
        }catch (NumberFormatException ex){
            return Optional.empty();
        }
    }
}
