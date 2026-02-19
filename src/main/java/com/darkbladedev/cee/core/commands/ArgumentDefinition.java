package com.darkbladedev.cee.core.commands;

import java.util.Objects;

public final class ArgumentDefinition {
    private final String name;
    private final ArgumentType type;
    private final boolean optional;
    private final Object defaultValue;
    private final Class<? extends Enum<?>> enumType;
    private final boolean greedy;
    private final SuggestionProvider suggestionProvider;

    private ArgumentDefinition(String name,
                               ArgumentType type,
                               boolean optional,
                               Object defaultValue,
                               Class<? extends Enum<?>> enumType,
                               boolean greedy,
                               SuggestionProvider suggestionProvider) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.enumType = enumType;
        this.greedy = greedy;
        this.suggestionProvider = suggestionProvider;
    }

    public static ArgumentDefinition required(String name, ArgumentType type) {
        return new ArgumentDefinition(name, type, false, null, null, false, null);
    }

    public static ArgumentDefinition optional(String name, ArgumentType type) {
        return new ArgumentDefinition(name, type, true, null, null, false, null);
    }

    public static ArgumentDefinition optional(String name, ArgumentType type, Object defaultValue) {
        return new ArgumentDefinition(name, type, true, defaultValue, null, false, null);
    }

    public ArgumentDefinition enumType(Class<? extends Enum<?>> enumType) {
        return new ArgumentDefinition(name, type, optional, defaultValue, enumType, greedy, suggestionProvider);
    }

    public ArgumentDefinition greedy() {
        return new ArgumentDefinition(name, type, optional, defaultValue, enumType, true, suggestionProvider);
    }

    public ArgumentDefinition suggestions(SuggestionProvider provider) {
        return new ArgumentDefinition(name, type, optional, defaultValue, enumType, greedy, provider);
    }

    public String name() {
        return name;
    }

    public ArgumentType type() {
        return type;
    }

    public boolean optional() {
        return optional;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public Class<? extends Enum<?>> enumType() {
        return enumType;
    }

    public boolean isGreedy() {
        return greedy;
    }

    public SuggestionProvider suggestionProvider() {
        return suggestionProvider;
    }
}
