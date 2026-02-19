package com.darkbladedev.cee.core.actions;

import org.bukkit.ChatColor;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;

public final class BroadcastAction implements Action {
    private final String message;

    public BroadcastAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(EventContext context) {
        context.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
