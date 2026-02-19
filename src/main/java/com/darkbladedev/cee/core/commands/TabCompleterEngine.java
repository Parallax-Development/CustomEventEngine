package com.darkbladedev.cee.core.commands;

import com.darkbladedev.cee.core.commands.impl.CommandServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TabCompleterEngine {
    private final CommandRegistry registry;
    private final ArgumentParser parser;
    private final CommandServices services;

    public TabCompleterEngine(CommandRegistry registry, ArgumentParser parser, CommandServices services) {
        this.registry = registry;
        this.parser = parser;
        this.services = services;
    }

    public List<String> suggest(CommandContext baseContext, String[] args) {
        if (args.length == 0) {
            return registry.suggestChildren(List.of(), "");
        }
        List<String> tokens = new ArrayList<>(Arrays.asList(args));
        String last = tokens.get(tokens.size() - 1);
        List<String> path = tokens.subList(0, tokens.size() - 1);
        List<String> childSuggestions = registry.suggestChildren(path, last);
        if (!childSuggestions.isEmpty()) {
            return childSuggestions;
        }
        CommandRegistry.Match match = registry.match(path);
        if (match == null) {
            return List.of();
        }
        int consumed = match.consumed();
        if (consumed < path.size()) {
            return List.of();
        }
        SubCommand command = match.command();
        int argIndex = path.size() - consumed;
        if (argIndex < 0) {
            return List.of();
        }
        List<ArgumentDefinition> definitions = command.arguments();
        if (argIndex >= definitions.size()) {
            return List.of();
        }
        ArgumentDefinition definition = definitions.get(argIndex);
        CommandContext context = new CommandContext(baseContext.sender(), baseContext.player().orElse(null), baseContext.arguments(), baseContext.rawArguments(), services);
        List<String> suggestions = parser.suggest(definition, context, last);
        String normalized = last.toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
