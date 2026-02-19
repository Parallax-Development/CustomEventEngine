package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class VariableEqualsCondition implements Condition {
    private final String key;
    private final String expected;

    public VariableEqualsCondition(String key, String expected) {
        this.key = key;
        this.expected = expected;
    }

    @Override
    public boolean evaluate(EventContext context) {
        if (key == null || key.isBlank()) {
            return false;
        }
        Object value = context.getVariable(key);
        if (value == null) {
            return false;
        }
        return String.valueOf(value).equals(expected);
    }
}
