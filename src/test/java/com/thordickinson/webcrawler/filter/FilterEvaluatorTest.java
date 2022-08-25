package com.thordickinson.webcrawler.filter;

import com.thordickinson.webcrawler.api.filter.FilterPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilterEvaluatorTest {

    @Autowired
    private FilterEvaluator evaluator;

    @Test
    void testHostFilter(){
        var pageLink = FilterPage.fromUri("https://stackoverflow.com/questions/19430737/find-protocol-corresponding-to-uri-in-java");
        var decider = new Decider(Optional.of("uri.host"), Decision.ACCEPT, "contains", Optional.of("stackoverflow"));
        var result = evaluator.evaluate(List.of(decider), pageLink);
        assertEquals(Decision.ACCEPT, result);
    }

    @Test
    void testRegexFilter(){
        var pageLink = FilterPage.fromUri("https://stackoverflow.com/questions/19430737/find-protocol-corresponding-to-uri-in-java");
        var decider = new Decider(Optional.of("uri.host"), Decision.REJECT, "pattern", Optional.of("^stack.*"));
        var result = evaluator.evaluate(List.of(decider), pageLink);
        assertEquals(Decision.REJECT, result);
    }

    @Test
    void compositeFilter(){
        var pageLink = FilterPage.fromUri("https://stackoverflow.com/questions/19430737/find-protocol-corresponding-to-uri-in-java");
        var decider = new Decider(Optional.of("uri.host"), Decision.ACCEPT, "pattern", Optional.of("^stack.*"));
        var decider2 = new Decider(Optional.of("uri.path"), Decision.REJECT, "contains", Optional.of("java"));
        var result = evaluator.evaluate(List.of(decider, decider2), pageLink);
        assertEquals(Decision.REJECT, result);
    }

    @Test
    void compositeFilterNone(){
        var pageLink = FilterPage.fromUri("https://stackoverflow.com/questions/19430737/find-protocol-corresponding-to-uri-in-java");
        var decider = new Decider(Optional.of("uri.host"), Decision.ACCEPT, "pattern", Optional.of("^stack.*"));
        var decider2 = new Decider(Optional.of("uri.path"), Decision.NONE, "contains", Optional.of("java"));
        var result = evaluator.evaluate(List.of(decider, decider2), pageLink);
        assertEquals(Decision.ACCEPT, result);
    }

}