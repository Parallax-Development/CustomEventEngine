package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.CommandRegistry;
import com.darkbladedev.cee.core.commands.SubCommand;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HelpCommand implements SubCommand {
    private final CommandRegistry registry;
    private final MessageService messages;
    private final String baseName;

    public HelpCommand(CommandRegistry registry, MessageService messages, String baseName) {
        this.registry = registry;
        this.messages = messages;
        this.baseName = baseName;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "cee.help";
    }

    @Override
    public List<String> aliases() {
        return List.of("?");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(ArgumentDefinition.optional("comando", ArgumentType.STRING)
            .suggestions((context, input) -> registry.registeredCommands().stream()
                .map(entry -> String.join(" ", entry.path()))
                .filter(cmd -> cmd.startsWith(input))
                .toList()));
    }

    @Override
    public String description() {
        return "Muestra la ayuda de comandos.";
    }

    @Override
    public void execute(CommandContext context) {
        String target = context.argument("comando", String.class);
        if (target == null || target.isBlank()) {
            messages.send(context.sender(), "help.header", Map.of());
            List<CommandRegistry.RegisteredCommand> commands = registry.registeredCommands().stream()
                .sorted(Comparator.comparing(entry -> String.join(" ", entry.path())))
                .toList();
            for (CommandRegistry.RegisteredCommand entry : commands) {
                SubCommand command = entry.command();
                String path = String.join(" ", entry.path());
                messages.send(context.sender(), "help.entry", Map.of(
                    "command", path,
                    "description", command.description()
                ));
            }
            return;
        }
        registry.findByPath(target).ifPresentOrElse(entry -> {
            SubCommand command = entry.command();
            String path = String.join(" ", entry.path());
            String usage = buildUsage(path, command);
            messages.send(context.sender(), "help.detail", Map.of(
                "command", path,
                "description", command.description(),
                "usage", usage
            ));
        }, () -> messages.send(context.sender(), "help.not-found", Map.of("input", target)));
    }

    private String buildUsage(String path, SubCommand command) {
        if (command.arguments().isEmpty()) {
            return "/" + baseName + " " + path;
        }
        String args = command.arguments().stream()
            .map(def -> def.optional() ? "[" + def.name() + "]" : "<" + def.name() + ">")
            .collect(Collectors.joining(" "));
        return "/" + baseName + " " + path + " " + args;
    }
}
