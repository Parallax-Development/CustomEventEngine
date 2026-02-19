package com.darkbladedev.cee.core.registry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.darkbladedev.cee.api.ActionFactory;
import com.darkbladedev.cee.api.ChunkSelectionStrategy;
import com.darkbladedev.cee.api.ConditionFactory;
import com.darkbladedev.cee.api.ScopeFactory;
import com.darkbladedev.cee.api.TriggerFactory;

public final class EngineRegistry {
    private final Map<String, ActionFactory> actionFactories = new ConcurrentHashMap<>();
    private final Map<String, ConditionFactory> conditionFactories = new ConcurrentHashMap<>();
    private final Map<String, TriggerFactory> triggerFactories = new ConcurrentHashMap<>();
    private final Map<String, ScopeFactory> scopeFactories = new ConcurrentHashMap<>();
    private final Map<String, ChunkSelectionStrategy> chunkStrategies = new ConcurrentHashMap<>();

    public void registerAction(String id, ActionFactory factory) {
        actionFactories.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    public void registerCondition(String id, ConditionFactory factory) {
        conditionFactories.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    public void registerTrigger(String id, TriggerFactory factory) {
        triggerFactories.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    public void registerScope(String id, ScopeFactory factory) {
        scopeFactories.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    public void registerChunkStrategy(String id, ChunkSelectionStrategy strategy) {
        chunkStrategies.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(strategy, "strategy"));
    }

    public Optional<ActionFactory> getActionFactory(String id) {
        return Optional.ofNullable(actionFactories.get(id));
    }

    public Optional<ConditionFactory> getConditionFactory(String id) {
        return Optional.ofNullable(conditionFactories.get(id));
    }

    public Optional<TriggerFactory> getTriggerFactory(String id) {
        return Optional.ofNullable(triggerFactories.get(id));
    }

    public Optional<ScopeFactory> getScopeFactory(String id) {
        return Optional.ofNullable(scopeFactories.get(id));
    }

    public Optional<ChunkSelectionStrategy> getChunkStrategy(String id) {
        return Optional.ofNullable(chunkStrategies.get(id));
    }
}
