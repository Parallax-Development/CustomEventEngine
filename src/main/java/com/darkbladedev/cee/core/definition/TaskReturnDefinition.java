package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class TaskReturnDefinition {
    private final String name;
    private final VariableType type;

    public TaskReturnDefinition(String name, VariableType type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getName() {
        return name;
    }

    public VariableType getType() {
        return type;
    }
}
