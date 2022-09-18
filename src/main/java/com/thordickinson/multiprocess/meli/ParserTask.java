package com.thordickinson.multiprocess.meli;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import static com.thordickinson.dumbcrawler.util.JsonUtil.*;

import com.thordickinson.dumbcrawler.util.HumanReadable;
import com.thordickinson.multiprocess.api.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParserTask extends AbstractTask<Any, Any> {

    private static final Logger logger = LoggerFactory.getLogger(ParserTask.class);
    private static final Any COP = Any.wrap("COP");
    private static final Any TRUE = Any.wrap(true);

    protected ParserTask(String taskId, Any input) {
        super(taskId, input);
    }

    public static Optional<String> extract(String content){
        var start = content.indexOf("window.__PRELOADED_STATE__", 0);
        if(start == -1) return Optional.empty();
        var objectStart = content.indexOf("{", start);

        if(objectStart == -1 || (objectStart - start) > ("window.__PRELOADED_STATE__ = ".length() + 10)) return Optional.empty();
        var end = content.indexOf("}};", start);
        if(end == -1) return Optional.empty();
        String value = content.substring(objectStart, end + "}}".length()); //Adding 2 bec
        return Optional.of(value);
    }
    private Map<String, ?> removeEmptyValues(Map<String, Optional<?>> map) {
        return map.entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
    }

    private Optional<Map<String, Any>> extractAttributes(Any attr) {
        return get(attr, "id").map(key -> Map.of(key.toString().toLowerCase(),
                Any.wrap(removeEmptyValues(Map.of("id", get(attr, "value_id"),
                        "name", get(attr, "value_name"))))));
    }

    private Optional<Any> parsePictures(Any content) {
        var pictureConfig = get(content, "initialState.components.gallery.picture_config");
        var pictureIds = get(content, "initialState.components.gallery.pictures").map(Any::asList)
                .map(l -> l.stream().map(e -> get(e, "id")).filter(Optional::isPresent).map(Optional::get)
                        .collect(Collectors.toList())).map(Any::wrap);
        return Optional.of(Any.wrap(removeEmptyValues(Map.of("pictureConfig", pictureConfig, "ids", pictureIds))));
    }

    private Optional<Any> extractModelInfo(Any object) {
        return get(object, "initialState.track.melidata_event.event_data.map_item_attributes")
                .map(Any::asList)
                .map(l -> l.stream().map(this::extractAttributes)
                        .filter(Optional::isPresent)
                        .map(Optional::get).flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()))).map(Any::wrap);
    }

    private Optional<Any> parseTechSpec(Any object) {
        return get(object, "initialState.components.technical_specifications.specs")
                .map(Any::asList).map(l -> l.stream().map(this::extractTechSpec)
                        .filter(Optional::isPresent)
                        .map(Optional::get).flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()))).map(Any::wrap);
    }

    private Optional<Map<String, Any>> extractTechSpec(Any tech) {
        return get(tech, "attributes").map(Any::asList).map(l -> l.stream().map(t -> {
                    var keys = t.keys();
                    if (keys.contains("id") && keys.contains("text")) {
                        return Optional.of(List.of(t.get("id").toString(), t.get("text")));
                    }
                    return get(t, "values.value_text.text").map(v -> List.of(v.toString(), TRUE));
                }).filter(Optional::isPresent).map(Optional::get))
                .map(l -> l.collect(Collectors.toMap(v -> String.valueOf(v.get(0)), v -> ((Any) v.get(1)))));
    }

    private Optional<Any> extractLocation(Any object) {
        return get(object, "initialState.track.melidata_event.event_data")
                .map(eventData ->
                        removeEmptyValues(Map.of("neighborhood", get(eventData, "neighborhood"),
                                "city", get(eventData, "state"),
                                "sector", get(eventData, "city")
                        ))).map(Any::wrap);
    }

    private Optional<Any> extractPrice(Any object) {
        return get(object, "initialState.components.price.price").flatMap(p ->
                get(p, "value").map(v ->
                        Map.of("value", v, "currency", get(p, "currency_id").orElse(COP))
                )
        ).map(Any::wrap);
    }

    private Optional<Any> extractDescription(Any object) {
        return get(object, "initialState.components.description.content");
    }

    private Any buildBasic(String url, Optional<Any> id, Long timestamp, Optional<Any> description) {
        url = url.indexOf("#") > -1 ? url.substring(0, url.indexOf("#")) : url;
        return Any.wrap(removeEmptyValues(Map.of(
                "url", Optional.of(Any.wrap(url)),
                "id", id,
                "timestamp", Optional.of(Any.wrap(timestamp)),
                "description", description
        )));
    }

    private Any filterProperties(String url, Long timestamp, Any object) {
        var id = get(object, "initialState.id");
        var description = extractDescription(object);
        var basic = Optional.of(buildBasic(url, id, timestamp, description));

        var price = extractPrice(object);
        var location = extractLocation(object);
        var modelInfo = extractModelInfo(object);
        var techSpec = parseTechSpec(object);
        var pictures = parsePictures(object);

        return Any.wrap(removeEmptyValues(Map.of("basic", basic, "pictures", pictures,
                "modelInfo", modelInfo, "techSpec", techSpec,
                "price", price, "location", location)));
    }

    private Optional<String> findObject(){
        return Optional.empty();
    }

    @Override
    public Optional<Any> call() throws Exception {
        Any page = getInput();
        var content = get(page, "content").map(Any::toString).orElse("");
        String url = get(page, "url").map(Any::toString).orElse("");
        Long timestamp = get(page, "timestamp").map(Any::toLong).orElse(-1L);

        return extract(content).flatMap(json -> {
            try {
                var parsed = JsonIterator.deserialize(json);
                parsed = filterProperties(url, timestamp, parsed);
                return Optional.of(parsed);
            } catch (Exception ex) {
                logger.error("Error parsing content: " + ex.toString()); //Not printing stack trace
                return Optional.empty();
            }
        });
      }

    @Override
    public String toString() {
        var input = getInput();
        var url = get(input, "url").map(Any::toString).orElse("<No url>");
        var size = get(input, "content").map(Any::toString).map(String::length)
                .map(HumanReadable::formatBits).orElse("Unknown size");
        return "[%s]: %s".formatted(size, url);
    }

    public static void main(String[] args) throws Exception {
        String executionId = "20220908_1631";

        Path root = Path.of("./data/jobs/meli-parse/executions")
                .resolve(executionId).resolve("errors/CancellationException");

        var files = root.toFile().listFiles();
        for (var file : files) {
            if(!file.isFile()) continue;
            var content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Any input = Any.wrap(Map.of("content", Any.wrap(content),
                    "url", Any.wrap("http:://test.com"),
                    "timestamp", Any.wrap(System.currentTimeMillis())));
            var start  = System.currentTimeMillis();
            var result = new ParserTask("nothing", input).call();
            System.out.println("took: " + HumanReadable.formatDuration(start));
            System.out.println(result);
        }

    }
}
