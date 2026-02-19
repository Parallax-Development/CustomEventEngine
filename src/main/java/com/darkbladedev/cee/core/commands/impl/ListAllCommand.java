package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ListAllCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public ListAllCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "list all";
    }

    @Override
    public String permission() {
        return "cee.view";
    }

    @Override
    public List<String> aliases() {
        return List.of("ls all");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of();
    }

    @Override
    public String description() {
        return "Lista todos los eventos.";
    }

    @Override
    public void execute(CommandContext context) {
        List<String> ids = new ArrayList<>(services.eventIds());
        ids.sort(String::compareToIgnoreCase);
        if (ids.isEmpty()) {
            messages.send(context.sender(), "list.empty", Map.of());
            return;
        }
        messages.send(context.sender(), "list.all", Map.of("events", String.join(", ", ids)));
    }
}
