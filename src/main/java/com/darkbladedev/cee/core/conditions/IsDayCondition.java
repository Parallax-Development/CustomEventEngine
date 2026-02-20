package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class IsDayCondition implements Condition {
    @Override
    public boolean evaluate(EventContext context) {
        long time = Math.floorMod(context.getWorld().getTime(), 24000);
        return time < 12300 || time >= 23850;
    }
}
