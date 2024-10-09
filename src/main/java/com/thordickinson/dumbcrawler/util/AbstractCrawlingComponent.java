package com.thordickinson.dumbcrawler.util;

import com.jsoniter.any.Any;
import com.thordickinson.dumbcrawler.api.CrawlingComponent;
import com.thordickinson.dumbcrawler.api.CrawlingSessionContext;
import com.thordickinson.dumbcrawler.expression.ThreadLocalEvaluator;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static com.thordickinson.dumbcrawler.util.JsonUtil.get;

public class AbstractCrawlingComponent implements CrawlingComponent {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private CrawlingSessionContext context;
    @Getter
    private final String componentKey;


    public AbstractCrawlingComponent(String componentKey) {
        this.componentKey = componentKey;
    }

    @Override
    public void initialize(CrawlingSessionContext context) {
        this.context = context;
        loadConfigurations(context);
    }

    protected <T> Optional<T> evaluate(Class<T> targetType, String expression, Map<String, Object> variables){
        var evaluator = ThreadLocalEvaluator.getThreadEvaluator();
        return evaluator.evaluateAs(targetType, expression, variables);
    }

    protected void setCounter(String key, Serializable value){
        if(this.context != null) return;
        this.context.setCounter(componentKey + "." + key, value);
    }

    protected void increaseCounter(String counter) {
        increaseCounter(counter, 1);
    }

    protected void increaseCounter(String counter, int amount) {
        if (this.context != null)
            this.context.increaseCounter(componentKey + "." + counter, amount);
    }

    protected Optional<Any> getConfiguration(String key) {
        return get(context.getJobConfiguration(), componentKey + "." + key);
    }

    protected void loadConfigurations(CrawlingSessionContext context) {
    }
}
