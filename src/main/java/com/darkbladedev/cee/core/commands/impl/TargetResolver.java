package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.exception.InvalidArgumentException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class TargetResolver {
    public Location resolveLocation(CommandContext context) {
        World world = context.argument("mundo", World.class);
        Integer x = context.argument("x", Integer.class);
        Integer z = context.argument("z", Integer.class);
        if (world == null) {
            if (x != null || z != null) {
                throw new InvalidArgumentException("mundo", "Debes indicar mundo, x y z.");
            }
            Player player = context.player().orElse(null);
            if (player == null) {
                throw new InvalidArgumentException("mundo", "Debes indicar mundo, x y z.");
            }
            return player.getLocation();
        }
        if (x == null || z == null) {
            throw new InvalidArgumentException("x", "Debes indicar x y z.");
        }
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
