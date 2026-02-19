package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class RandomChanceCondition implements Condition {
    private final double chance;

    public RandomChanceCondition(double chance) {
        double normalized = chance;
        if (normalized > 1.0) {
            normalized = normalized / 100.0;
        }
        if (normalized < 0.0) {
            normalized = 0.0;
        }
        if (normalized > 1.0) {
            normalized = 1.0;
        }
        this.chance = normalized;
    }

    @Override
    public boolean evaluate(EventContext context) {
        return Math.random() <= chance;
    }
}
