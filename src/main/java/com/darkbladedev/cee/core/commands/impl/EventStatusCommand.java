package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EventStatusCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;
    private final TargetResolver targetResolver;

    public EventStatusCommand(CommandServices services, MessageService messages, TargetResolver targetResolver) {
        this.services = services;
        this.messages = messages;
        this.targetResolver = targetResolver;
    }

    @Override
    public String name() {
        return "event status";
    }

    @Override
    public String permission() {
        return "cee.view";
    }

    @Override
    public List<String> aliases() {
        return List.of("event state");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(
            ArgumentDefinition.optional("mundo", ArgumentType.WORLD),
            ArgumentDefinition.optional("x", ArgumentType.INT),
            ArgumentDefinition.optional("z", ArgumentType.INT)
        );
    }

    @Override
    public String description() {
        return "Muestra estado del evento activo en el chunk objetivo.";
    }

    @Override
    public void execute(CommandContext context) {
        Location location = targetResolver.resolveLocation(context);
        Optional<EventHandle> handle = services.engine().getActiveEvent(ChunkUtil.fromLocation(location));
        if (handle.isEmpty()) {
            messages.send(context.sender(), "event.status.none", Map.of());
            return;
        }
        messages.send(context.sender(), "event.status.active", Map.of(
            "event", handle.get().getEventId(),
            "state", handle.get().getState().name()
        ));
    }
}
