package com.darkbladedev.cee.core.runtime;

import org.bukkit.Server;
import org.bukkit.World;

import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.core.definition.VariableDefinition;
import com.darkbladedev.cee.core.definition.VariableScope;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EventContextImpl implements EventContext {
    private final Server server;
    private final World world;
    private final Set<UUID> participants;
    private final Map<String, Object> localVariables;
    private final Map<String, Object> globalVariables;
    private final Map<String, VariableDefinition> variableDefinitions;
    private final Map<String, VariableDefinition> globalVariableDefinitions;
    private final Map<String, Object> variables;
    private volatile long currentTick;

    public EventContextImpl(Server server, World world) {
        this(server, world, new ConcurrentHashMap<>(), Map.of(), Map.of());
    }

    public EventContextImpl(Server server,
                            World world,
                            Map<String, Object> globalVariables,
                            Map<String, VariableDefinition> variableDefinitions,
                            Map<String, VariableDefinition> globalVariableDefinitions) {
        this.server = Objects.requireNonNull(server, "server");
        this.world = Objects.requireNonNull(world, "world");
        this.participants = ConcurrentHashMap.newKeySet();
        this.localVariables = new ConcurrentHashMap<>();
        this.globalVariables = Objects.requireNonNull(globalVariables, "globalVariables");
        this.variableDefinitions = Objects.requireNonNull(variableDefinitions, "variableDefinitions");
        this.globalVariableDefinitions = Objects.requireNonNull(globalVariableDefinitions, "globalVariableDefinitions");
        this.variables = new VariableMapView(localVariables, this.globalVariables, this.variableDefinitions, this.globalVariableDefinitions);
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
        if (key == null) {
            return null;
        }
        if (localVariables.containsKey(key)) {
            return localVariables.get(key);
        }
        return globalVariables.get(key);
    }

    @Override
    public void setVariable(String key, Object value) {
        if (key == null || key.isBlank()) {
            return;
        }
        variables.put(key, value);
    }

    public Map<String, Object> getLocalVariables() {
        return localVariables;
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public boolean isVariableDeclared(String name) {
        return variableDefinitions.containsKey(name) || globalVariableDefinitions.containsKey(name);
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(long currentTick) {
        this.currentTick = currentTick;
    }

    private static final class VariableMapView extends AbstractMap<String, Object> {
        private final Map<String, Object> local;
        private final Map<String, Object> global;
        private final Map<String, VariableDefinition> defs;
        private final Map<String, VariableDefinition> globalDefs;

        private VariableMapView(Map<String, Object> local,
                                Map<String, Object> global,
                                Map<String, VariableDefinition> defs,
                                Map<String, VariableDefinition> globalDefs) {
            this.local = local;
            this.global = global;
            this.defs = defs;
            this.globalDefs = globalDefs;
        }

        @Override
        public Object get(Object key) {
            if (!(key instanceof String name)) {
                return null;
            }
            if (local.containsKey(name)) {
                return local.get(name);
            }
            return global.get(name);
        }

        @Override
        public boolean containsKey(Object key) {
            if (!(key instanceof String name)) {
                return false;
            }
            return local.containsKey(name) || global.containsKey(name);
        }

        @Override
        public Object put(String key, Object value) {
            if (key == null || key.isBlank()) {
                return null;
            }
            if (isGlobal(key)) {
                return global.put(key, value);
            }
            return local.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            if (!(key instanceof String name)) {
                return null;
            }
            if (isGlobal(name)) {
                return global.remove(name);
            }
            return local.remove(name);
        }

        @Override
        public void clear() {
            local.clear();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            Map<String, Object> merged = new HashMap<>(global);
            merged.putAll(local);
            return merged.entrySet();
        }

        private boolean isGlobal(String name) {
            VariableDefinition def = defs.get(name);
            if (def != null) {
                return def.getScope() == VariableScope.GLOBAL;
            }
            return globalDefs.containsKey(name);
        }
    }
}
