package com.thordickinson.webcrawler.filter;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FilterEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(FilterEvaluator.class);
    @Autowired
    private List<DecisionOperation> operations;
    private Map<String, DecisionOperation> operationIndex = Collections.emptyMap();
    private final Decision DEFAULT_DECISION = Decision.ACCEPT;

    @PostConstruct
    void initialize(){
        operationIndex = operations.stream().collect(Collectors.toMap(DecisionOperation::getKey, d -> d));
    }

    public Decision evaluate(List<Decider> filters, Object target){
        var result =
        filters.stream().map(f -> evaluate(f, target)).filter(d -> Decision.NONE != d).collect(Collectors.toList());
        if(result.isEmpty()) return DEFAULT_DECISION;
        return result.get(result.size() - 1);
    }

    private Optional<Object> getField(Object target, Optional<String> property) throws Exception {
        return property.flatMap(p ->
        {
            try {
                return Optional.ofNullable(BeanUtils.getProperty(target, p));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    private Decision evaluate(Decider filter, Object target) {
        var operatorKey = filter.operator().trim();
        if(!operationIndex.containsKey(operatorKey)){
            throw new IllegalArgumentException("Operator with key does not exists: " + operatorKey);
        }
        var operator = operationIndex.get(operatorKey);
        try{
        Optional<Object> field =  getField(target, filter.field());
        boolean result = operator.evaluate(field, filter.argument());
        return result? filter.action() : Decision.NONE;
        }catch (Exception ex){
            logger.error("Error evaluating filter", ex);
            return Decision.NONE;
        }
    }
}
