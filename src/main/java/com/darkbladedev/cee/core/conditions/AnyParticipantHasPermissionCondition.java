package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public final class AnyParticipantHasPermissionCondition implements Condition {
    private final String permission;

    public AnyParticipantHasPermissionCondition(String permission) {
        this.permission = Objects.requireNonNull(permission, "permission").trim();
    }

    @Override
    public boolean evaluate(EventContext context) {
        if (permission.isBlank()) {
            return false;
        }
        for (UUID uuid : context.getParticipants()) {
            Player player = context.getServer().getPlayer(uuid);
            if (player != null && player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
