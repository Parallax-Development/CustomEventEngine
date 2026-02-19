package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class ReloadCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public ReloadCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public List<String> aliases() {
        return List.of("rl");
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
        return "Recarga eventos y mensajes.";
    }

    @Override
    public void execute(CommandContext context) {
        File eventsFolder = new File(services.plugin().getDataFolder(), "events");
        services.engine().reloadDefinitions(eventsFolder, services.plugin().getServer());
        services.refreshCaches();
        messages.reload();
        messages.send(context.sender(), "reload.success", Map.of());
    }
}
