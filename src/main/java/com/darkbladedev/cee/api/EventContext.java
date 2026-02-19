package com.darkbladedev.cee.api;

import org.bukkit.Server;
import org.bukkit.World;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface EventContext {
    Server getServer();
    World getWorld();
    Set<UUID> getParticipants();
    Map<String, Object> getVariables();
    Object getVariable(String key);
    void setVariable(String key, Object value);
}
