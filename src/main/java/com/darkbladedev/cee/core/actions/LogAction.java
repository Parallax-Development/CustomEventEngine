package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.ValueResolver;

public final class LogAction implements Action {
    private final String message;

    public LogAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(EventContext context) {
        String resolved = ValueResolver.resolveText(message, context);
        if (resolved.isBlank()) {
            return;
        }
        context.getServer().getLogger().info(resolved);
    }
}
