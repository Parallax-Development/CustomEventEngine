package com.darkbladedev.cee.core.actions;

import org.bukkit.ChatColor;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.ValueResolver;

public final class BroadcastAction implements Action {
    private final String message;

    public BroadcastAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(EventContext context) {
        String resolved = ValueResolver.resolveText(message, context);
        context.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', resolved));
    }
}
