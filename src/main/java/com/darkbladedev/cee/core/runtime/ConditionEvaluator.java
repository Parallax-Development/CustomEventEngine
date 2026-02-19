package com.darkbladedev.cee.core.runtime;

import java.util.List;

import com.darkbladedev.cee.api.Condition;

public final class ConditionEvaluator {
    public boolean evaluateAll(List<Condition> conditions, EventRuntime runtime) {
        for (Condition condition : conditions) {
            if (!condition.evaluate(runtime.getContext())) {
                return false;
            }
        }
        return true;
    }
}
