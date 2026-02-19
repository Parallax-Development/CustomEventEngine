package com.darkbladedev.cee.core.definition;

import java.util.Map;
import java.util.Objects;

public final class ActionDefinition {
    private final String type;
    private final Map<String, Object> config;

    public ActionDefinition(String type, Map<String, Object> config) {
        this.type = Objects.requireNonNull(type, "type");
        this.config = Map.copyOf(config);
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
