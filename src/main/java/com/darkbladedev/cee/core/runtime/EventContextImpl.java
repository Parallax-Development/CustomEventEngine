package com.darkbladedev.cee.core.runtime;

import org.bukkit.Server;
import org.bukkit.World;

import com.darkbladedev.cee.api.EventContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EventContextImpl implements EventContext {
    private final Server server;
    private final World world;
    private final Set<UUID> participants;
    private final Map<String, Object> variables;
    private volatile long currentTick;

    public EventContextImpl(Server server, World world) {
        this.server = Objects.requireNonNull(server, "server");
        this.world = Objects.requireNonNull(world, "world");
        this.participants = ConcurrentHashMap.newKeySet();
        this.variables = new ConcurrentHashMap<>();
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public Set<UUID> getParticipants() {
        return participants;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Object getVariable(String key) {
        return variables.get(key);
    }

    @Override
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(long currentTick) {
        this.currentTick = currentTick;
    }
}
