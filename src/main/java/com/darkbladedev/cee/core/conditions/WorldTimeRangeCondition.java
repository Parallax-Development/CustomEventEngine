package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class WorldTimeRangeCondition implements Condition {
    private final long min;
    private final long max;

    public WorldTimeRangeCondition(long min, long max) {
        this.min = Math.floorMod(min, 24000);
        this.max = Math.floorMod(max, 24000);
    }

    @Override
    public boolean evaluate(EventContext context) {
        long time = Math.floorMod(context.getWorld().getTime(), 24000);
        if (min <= max) {
            return time >= min && time <= max;
        }
        return time >= min || time <= max;
    }
}
