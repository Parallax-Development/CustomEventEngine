package com.darkbladedev.cee.core.actions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.ValueResolver;

import java.util.UUID;

public final class SendParticipantsAction implements Action {
    private final String message;

    public SendParticipantsAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(EventContext context) {
        String resolved = ValueResolver.resolveText(message, context);
        String colored = ChatColor.translateAlternateColorCodes('&', resolved);
        for (UUID playerId : context.getParticipants()) {
            Player player = context.getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage(colored);
            }
        }
    }
}
