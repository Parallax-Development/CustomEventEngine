package com.darkbladedev.cee.core.conditions;

import org.mvel2.MVEL;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

import java.io.Serializable;
import java.util.Map;

public final class MvelCondition implements Condition {
    private final Serializable compiled;

    public MvelCondition(String expression) {
        this.compiled = MVEL.compileExpression(expression);
    }

    @Override
    public boolean evaluate(EventContext context) {
        Map<String, Object> vars = context.getVariables();
        Object result = MVEL.executeExpression(compiled, vars);
        if (result instanceof Boolean value) {
            return value;
        }
        return false;
    }
}
