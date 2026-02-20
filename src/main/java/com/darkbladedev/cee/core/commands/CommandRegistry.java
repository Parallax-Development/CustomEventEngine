package com.darkbladedev.cee.core.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandRegistry {
    private final Node root = new Node();
    private final Map<SubCommand, List<String>> commandPaths = new LinkedHashMap<>();

    public void register(SubCommand command) {
        Objects.requireNonNull(command, "command");
        registerPath(command.name(), command);
        for (String alias : command.aliases()) {
            registerPath(alias, command);
        }
    }

    public Match match(List<String> args) {
        Node node = root;
        int consumed = 0;
        SubCommand lastCommand = null;
        int lastCommandConsumed = 0;
        for (String token : args) {
            Node child = node.children.get(normalize(token));
            if (child == null) {
                break;
            }
            node = child;
            consumed++;
            if (node.command != null) {
                lastCommand = node.command;
                lastCommandConsumed = consumed;
            }
        }
        if (lastCommand == null) {
            return null;
        }
        return new Match(lastCommand, lastCommandConsumed, node);
    }

    public List<String> suggestChildren(List<String> path, String input) {
        Node node = traverse(path);
        if (node == null) {
            return List.of();
        }
        String normalized = normalize(input);
        List<String> result = new ArrayList<>();
        for (String key : node.children.keysInOrder()) {
            if (key.startsWith(normalized)) {
                result.add(node.children.originalKey(key));
            }
        }
        return result;
    }

    public List<RegisteredCommand> registeredCommands() {
        List<RegisteredCommand> result = new ArrayList<>();
        for (Map.Entry<SubCommand, List<String>> entry : commandPaths.entrySet()) {
            SubCommand command = entry.getKey();
            if (entry.getValue().isEmpty()) {
                continue;
            }
            result.add(new RegisteredCommand(command, entry.getValue()));
        }
        return result;
    }

    public Optional<RegisteredCommand> findByPath(String path) {
        String normalized = normalize(path);
        for (Map.Entry<SubCommand, List<String>> entry : commandPaths.entrySet()) {
            String joined = String.join(" ", entry.getValue());
            if (normalize(joined).equals(normalized)) {
                return Optional.of(new RegisteredCommand(entry.getKey(), entry.getValue()));
            }
        }
        return Optional.empty();
    }

    private void registerPath(String path, SubCommand command) {
        List<String> tokens = tokenize(path);
        Node node = root;
        for (String token : tokens) {
            node = node.children.getOrCreate(token);
        }
        node.command = command;
        commandPaths.putIfAbsent(command, tokens);
    }

    private Node traverse(List<String> path) {
        Node node = root;
        for (String token : path) {
            node = node.children.get(normalize(token));
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public static final class Match {
        private final SubCommand command;
        private final int consumed;
        private final Node node;

        private Match(SubCommand command, int consumed, Node node) {
            this.command = command;
            this.consumed = consumed;
            this.node = node;
        }

        public SubCommand command() {
            return command;
        }

        public int consumed() {
            return consumed;
        }

        public Node node() {
            return node;
        }
    }

    public static final class RegisteredCommand {
        private final SubCommand command;
        private final List<String> path;

        private RegisteredCommand(SubCommand command, List<String> path) {
            this.command = command;
            this.path = List.copyOf(path);
        }

        public SubCommand command() {
            return command;
        }

        public List<String> path() {
            return path;
        }
    }

    public static final class Node {
        private final OrderedMap children = new OrderedMap();
        private SubCommand command;

        public OrderedMap children() {
            return children;
        }
    }

    public static final class OrderedMap {
        private final Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        private final Map<String, Node> nodes = new LinkedHashMap<>();

        public Node get(String token) {
            return nodes.get(normalize(token));
        }

        public Node getOrCreate(String token) {
            String normalized = normalize(token);
            normalizedToOriginal.putIfAbsent(normalized, token);
            return nodes.computeIfAbsent(normalized, key -> new Node());
        }

        public Iterable<String> keysInOrder() {
            return normalizedToOriginal.keySet();
        }

        public String originalKey(String normalized) {
            return normalizedToOriginal.getOrDefault(normalized, normalized);
        }

        private String normalize(String value) {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
