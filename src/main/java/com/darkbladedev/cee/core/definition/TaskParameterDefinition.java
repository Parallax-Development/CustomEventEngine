package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class TaskParameterDefinition {
    private final String name;
    private final VariableType type;
    private final boolean required;
    private final Object defaultValue;

    public TaskParameterDefinition(String name, VariableType type, boolean required, Object defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public VariableType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
