package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.DurationParser;
import com.darkbladedev.cee.util.ValueResolver;

public final class SetTimeAction implements Action {
    private final Object time;

    public SetTimeAction(Object time) {
        this.time = time;
    }

    @Override
    public void execute(EventContext context) {
        Object resolved = ValueResolver.resolveValue(time, context);
        if (resolved == null) {
            return;
        }
        long parsed;
        try {
            parsed = DurationParser.parseTicks(resolved);
        } catch (Exception ignored) {
            if (resolved instanceof Number number) {
                parsed = number.longValue();
            } else {
                return;
            }
        }
        context.getWorld().setTime(Math.max(0L, parsed));
    }
}
