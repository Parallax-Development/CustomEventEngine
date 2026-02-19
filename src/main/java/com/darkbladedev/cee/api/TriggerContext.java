package com.darkbladedev.cee.api;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class TriggerContext {
    private final String eventId;
    private final World world;
    private final Location location;

    public TriggerContext(String eventId, World world, Location location) {
        this.eventId = eventId;
        this.world = world;
        this.location = location;
    }

    public String getEventId() {
        return eventId;
    }

    public Optional<World> getWorld() {
        return Optional.ofNullable(world);
    }

    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }
}
