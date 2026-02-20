package com.darkbladedev.cee.core.commands;

import com.darkbladedev.cee.core.commands.exception.InvalidArgumentException;
import com.darkbladedev.cee.core.commands.impl.CommandServices;
import org.bukkit.World;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArgumentParser {
    private final Map<ArgumentType, TypeHandler> handlers = new HashMap<>();

    public ArgumentParser() {
        handlers.put(ArgumentType.STRING, new StringHandler());
        handlers.put(ArgumentType.INT, new IntHandler());
        handlers.put(ArgumentType.BOOLEAN, new BooleanHandler());
        handlers.put(ArgumentType.ENUM, new EnumHandler());
        handlers.put(ArgumentType.EVENT_ID, new EventIdHandler());
        handlers.put(ArgumentType.WORLD, new WorldHandler());
    }

    public Map<String, Object> parse(List<ArgumentDefinition> definitions, List<String> input, CommandServices services) {
        Map<String, Object> result = new HashMap<>();
        int index = 0;
        for (ArgumentDefinition definition : definitions) {
            if (index >= input.size()) {
                if (definition.optional()) {
                    if (definition.defaultValue() != null) {
                        result.put(definition.name(), definition.defaultValue());
                    }
                    continue;
                }
                throw new InvalidArgumentException(definition.name(), "Falta el argumento: " + definition.name());
            }
            if (definition.isGreedy()) {
                String value = String.join(" ", input.subList(index, input.size()));
                Object parsed = parseSingle(definition, value, services);
                result.put(definition.name(), parsed);
                index = input.size();
                break;
            }
            String token = input.get(index);
            Object parsed = parseSingle(definition, token, services);
            result.put(definition.name(), parsed);
            index++;
        }
        if (index < input.size()) {
            throw new InvalidArgumentException("extra", "Sobran argumentos.");
        }
        return result;
    }

    public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
        SuggestionProvider provider = definition.suggestionProvider();
        if (provider != null) {
            return provider.suggest(context, input);
        }
        TypeHandler handler = handlers.get(definition.type());
        if (handler == null) {
            return List.of();
        }
        return handler.suggest(definition, context, input);
    }

    private Object parseSingle(ArgumentDefinition definition, String token, CommandServices services) {
        TypeHandler handler = handlers.get(definition.type());
        if (handler == null) {
            throw new InvalidArgumentException(definition.name(), "Tipo no soportado.");
        }
        return handler.parse(definition, token, services);
    }

    private interface TypeHandler {
        Object parse(ArgumentDefinition definition, String token, CommandServices services);
        List<String> suggest(ArgumentDefinition definition, CommandContext context, String input);
    }

    private static final class StringHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            return token;
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            return List.of();
        }
    }

    private static final class IntHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            try {
                return Integer.parseInt(token);
            } catch (NumberFormatException ex) {
                throw new InvalidArgumentException(definition.name(), "Debe ser un número entero.");
            }
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            return List.of();
        }
    }

    private static final class BooleanHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (normalized.equals("true") || normalized.equals("false")) {
                return Boolean.parseBoolean(normalized);
            }
            throw new InvalidArgumentException(definition.name(), "Debe ser true o false.");
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            return List.of("true", "false");
        }
    }

    private static final class EnumHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            Class<? extends Enum<?>> enumType = definition.enumType();
            if (enumType == null) {
                throw new InvalidArgumentException(definition.name(), "Enum no definido.");
            }
            for (Enum<?> constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(token)) {
                    return constant;
                }
            }
            throw new InvalidArgumentException(definition.name(), "Valor inválido.");
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            Class<? extends Enum<?>> enumType = definition.enumType();
            if (enumType == null) {
                return List.of();
            }
            String normalized = input.toLowerCase(Locale.ROOT);
            return List.of(enumType.getEnumConstants()).stream()
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .filter(value -> value.startsWith(normalized))
                .toList();
        }
    }

    private static final class EventIdHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            String normalized = token.trim();
            if (!services.eventIds().contains(normalized)) {
                throw new InvalidArgumentException(definition.name(), "Evento no encontrado.");
            }
            return normalized;
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            String normalized = input.toLowerCase(Locale.ROOT);
            return context.services().eventIds().stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
        }
    }

    private static final class WorldHandler implements TypeHandler {
        @Override
        public Object parse(ArgumentDefinition definition, String token, CommandServices services) {
            World world = services.plugin().getServer().getWorld(token);
            if (world == null) {
                throw new InvalidArgumentException(definition.name(), "Mundo no encontrado.");
            }
            return world;
        }

        @Override
        public List<String> suggest(ArgumentDefinition definition, CommandContext context, String input) {
            String normalized = input.toLowerCase(Locale.ROOT);
            return context.services().plugin().getServer().getWorlds().stream()
                .map(World::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
        }
    }
}
