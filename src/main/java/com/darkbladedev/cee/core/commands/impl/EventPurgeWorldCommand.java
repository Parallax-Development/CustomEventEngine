package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.core.commands.exception.InvalidArgumentException;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public final class EventPurgeWorldCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public EventPurgeWorldCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "event purge world";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(
            ArgumentDefinition.required("mundo", ArgumentType.WORLD),
            ArgumentDefinition.optional("flag", ArgumentType.STRING)
                .suggestions((context, input) -> List.of("--include-schedulers"))
        );
    }

    @Override
    public String description() {
        return "Purga todos los runtimes activos en un mundo, liberando locks y tasks.";
    }

    @Override
    public void execute(CommandContext context) {
        World world = context.argument("mundo", World.class);
        String flag = context.argument("flag", String.class);
        boolean includeSchedulers = parseIncludeSchedulersFlag(flag);

        var result = services.engine().purgeWorld(world.getUID(), includeSchedulers);
        if (result.runtimesPurged() == 0) {
            messages.send(context.sender(), "event.purge.none", Map.of());
            return;
        }
        messages.send(context.sender(), "event.purge.world.success", Map.of(
            "world", world.getName(),
            "runtimes", String.valueOf(result.runtimesPurged()),
            "chunks", String.valueOf(result.chunksFreed()),
            "schedulers", String.valueOf(result.schedulersDisabled())
        ));
    }

    private boolean parseIncludeSchedulersFlag(String flag) {
        if (flag == null || flag.isBlank()) {
            return false;
        }
        if (flag.equalsIgnoreCase("--include-schedulers")) {
            return true;
        }
        throw new InvalidArgumentException("flag", "Flag inválido. Usa --include-schedulers.");
    }
}
