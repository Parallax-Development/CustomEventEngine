package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.util.ValueResolver;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ExecuteParticipantsCommandAction implements Action {
    private final String command;

    public ExecuteParticipantsCommandAction(String command) {
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
        for (UUID uuid : context.getParticipants()) {
            Player player = context.getServer().getPlayer(uuid);
            if (player != null) {
                player.performCommand(resolved);
            }
        }
    }
}
