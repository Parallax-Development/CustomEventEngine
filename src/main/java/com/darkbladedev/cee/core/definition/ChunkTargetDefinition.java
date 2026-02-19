package com.darkbladedev.cee.core.definition;

import java.util.Map;
import java.util.Objects;

public final class ChunkTargetDefinition {
    private final String strategy;
    private final Map<String, Object> config;

    public ChunkTargetDefinition(String strategy, Map<String, Object> config) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.config = Map.copyOf(config);
    }

    public String getStrategy() {
        return strategy;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
