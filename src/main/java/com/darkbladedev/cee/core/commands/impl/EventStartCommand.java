package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.api.StartResult;
import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;

public final class EventStartCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;
    private final TargetResolver targetResolver;

    public EventStartCommand(CommandServices services, MessageService messages, TargetResolver targetResolver) {
        this.services = services;
        this.messages = messages;
        this.targetResolver = targetResolver;
    }

    @Override
    public String name() {
        return "event start";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public List<String> aliases() {
        return List.of("event run");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(
            ArgumentDefinition.required("evento", ArgumentType.EVENT_ID),
            ArgumentDefinition.optional("mundo", ArgumentType.WORLD),
            ArgumentDefinition.optional("x", ArgumentType.INT),
            ArgumentDefinition.optional("z", ArgumentType.INT)
        );
    }

    @Override
    public String description() {
        return "Inicia un evento en el chunk objetivo.";
    }

    @Override
    public void execute(CommandContext context) {
        String eventId = context.argument("evento", String.class);
        Location location = targetResolver.resolveLocation(context);
        StartResult result = services.engine().startEvent(eventId, ChunkUtil.fromLocation(location));
        switch (result) {
            case SUCCESS -> messages.send(context.sender(), "event.start.success", Map.of("event", eventId));
            case NOT_FOUND -> messages.send(context.sender(), "event.start.not-found", Map.of("event", eventId));
            case CHUNK_OCCUPIED -> messages.send(context.sender(), "event.start.chunk-occupied", Map.of());
            case INVALID_TARGET -> messages.send(context.sender(), "event.start.invalid-target", Map.of());
            case CONDITIONS_FAILED -> messages.send(context.sender(), "event.start.conditions-failed", Map.of());
        }
    }
}
