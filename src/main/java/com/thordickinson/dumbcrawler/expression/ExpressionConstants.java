package com.thordickinson.dumbcrawler.expression;

import com.creativewidgetworks.expressionparser.Value;

public interface ExpressionConstants {
    Value FALSE = new Value().setValue(false);
    Value TRUE = new Value().setValue(true);
    Value NULL = new Value();
}
