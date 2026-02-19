package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class VariableDefinition {
    private final String name;
    private final VariableType type;
    private final VariableScope scope;
    private final Object initial;
    private final String description;

    public VariableDefinition(String name, VariableType type, VariableScope scope, Object initial, String description) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.initial = initial;
        this.description = description == null ? "" : description;
    }

    public String getName() {
        return name;
    }

    public VariableType getType() {
        return type;
    }

    public VariableScope getScope() {
        return scope;
    }

    public Object getInitial() {
        return initial;
    }

    public String getDescription() {
        return description;
    }
}
