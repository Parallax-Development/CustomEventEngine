package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;

public final class SetTimeAction implements Action {
    private final long time;

    public SetTimeAction(long time) {
        this.time = Math.max(0L, time);
    }

    @Override
    public void execute(EventContext context) {
        context.getWorld().setTime(time);
    }
}
