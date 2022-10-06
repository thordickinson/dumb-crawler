package com.thordickinson.dumbcrawler.services;

import java.util.Map;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.AbstractCrawlingComponent;
import com.thordickinson.dumbcrawler.api.ContentValidator;
import com.thordickinson.dumbcrawler.api.CrawlingContext;

@Service
public class ExpressionContentValidator extends AbstractCrawlingComponent implements ContentValidator  {

    private Optional<String>  expression = Optional.empty();

    public ExpressionContentValidator(){
        super("contentValidator");
    }

    @Override
    public void initialize(CrawlingContext context) {
        expression = context.getConfig("expression").map(Any::toString);
    }

    @Override
    public boolean validateContent(Document document) {
        return expression.flatMap(e -> 
            evaluate(Boolean.class, e, Map.of("document", document, "html", document.html()))
        ).orElse(true);
    }

}
