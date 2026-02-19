package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;

public final class ClearWeatherAction implements Action {
    @Override
    public void execute(EventContext context) {
        context.getWorld().setStorm(false);
        context.getWorld().setThundering(false);
        context.getWorld().setWeatherDuration(0);
        context.getWorld().setThunderDuration(0);
    }
}
