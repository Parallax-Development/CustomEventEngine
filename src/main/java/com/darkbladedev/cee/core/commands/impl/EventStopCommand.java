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

public final class EventStopCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;
    private final TargetResolver targetResolver;

    public EventStopCommand(CommandServices services, MessageService messages, TargetResolver targetResolver) {
        this.services = services;
        this.messages = messages;
        this.targetResolver = targetResolver;
    }

    @Override
    public String name() {
        return "event stop";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public List<String> aliases() {
        return List.of("event end");
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
        return "Detiene el evento activo en el chunk objetivo.";
    }

    @Override
    public void execute(CommandContext context) {
        Location location = targetResolver.resolveLocation(context);
        Optional<EventHandle> handle = services.engine().getActiveEvent(ChunkUtil.fromLocation(location));
        if (handle.isEmpty()) {
            messages.send(context.sender(), "event.stop.none", Map.of());
            return;
        }
        String eventId = handle.get().getEventId();
        handle.get().cancel();
        services.engine().disableIntervalSchedule(eventId);
        messages.send(context.sender(), "event.stop.success", Map.of("event", eventId));
    }
}
