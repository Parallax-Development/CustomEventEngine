package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ListCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public ListCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return "cee.view";
    }

    @Override
    public List<String> aliases() {
        return List.of("ls");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(ArgumentDefinition.optional("pagina", ArgumentType.INT, 1));
    }

    @Override
    public String description() {
        return "Lista eventos cargados.";
    }

    @Override
    public void execute(CommandContext context) {
        List<String> ids = new ArrayList<>(services.eventIds());
        ids.sort(String::compareToIgnoreCase);
        if (ids.isEmpty()) {
            messages.send(context.sender(), "list.empty", Map.of());
            return;
        }
        int page = context.argument("pagina", Integer.class) == null ? 1 : context.argument("pagina", Integer.class);
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil(ids.size() / (double) pageSize));
        if (page < 1 || page > totalPages) {
            messages.send(context.sender(), "list.invalid-page", Map.of("max", String.valueOf(totalPages)));
            return;
        }
        int from = (page - 1) * pageSize;
        int to = Math.min(ids.size(), from + pageSize);
        List<String> slice = ids.subList(from, to);
        messages.send(context.sender(), "list.page", Map.of(
            "page", String.valueOf(page),
            "pages", String.valueOf(totalPages),
            "events", String.join(", ", slice)
        ));
    }
}
