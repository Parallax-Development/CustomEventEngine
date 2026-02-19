package com.darkbladedev.cee.core.actions;

import org.bukkit.Location;
import org.bukkit.World;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;

import java.util.Random;

public final class SpawnLightningAction implements Action {
    private final Random random = new Random();

    @Override
    public void execute(EventContext context) {
        World world = context.getWorld();
        Location location = world.getSpawnLocation();
        int dx = random.nextInt(16) - 8;
        int dz = random.nextInt(16) - 8;
        Location strike = location.clone().add(dx, 0, dz);
        world.strikeLightning(strike);
    }
}
