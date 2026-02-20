package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.ValueResolver;

public final class ExecuteConsoleCommandAction implements Action {
    private final String command;

    public ExecuteConsoleCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(EventContext context) {
        String resolved = ValueResolver.resolveText(command, context).trim();
        if (resolved.isBlank()) {
            return;
        }
        if (resolved.startsWith("/")) {
            resolved = resolved.substring(1).trim();
        }
        if (resolved.isBlank()) {
            return;
        }
        context.getServer().dispatchCommand(context.getServer().getConsoleSender(), resolved);
    }
}
