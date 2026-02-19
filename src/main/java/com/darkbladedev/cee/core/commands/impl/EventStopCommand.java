package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.Location;
import org.bukkit.World;

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
        World world = context.argument("mundo", World.class);
        Integer x = context.argument("x", Integer.class);
        Integer z = context.argument("z", Integer.class);

        Optional<EventHandle> handle = services.engine().getActiveEvent(ChunkUtil.fromLocation(location));
        if (handle.isEmpty() && world != null && x != null && z != null) {
            var byBlocks = ChunkUtil.fromBlockCoords(world.getUID(), x, z);
            var byChunks = ChunkUtil.fromWorld(world, x, z);
            if (!byChunks.equals(byBlocks)) {
                handle = services.engine().getActiveEvent(byBlocks);
                if (handle.isEmpty()) {
                    handle = services.engine().getActiveEvent(byChunks);
                }
            }

            if (services.plugin().getConfig().getBoolean("debug.chunk-lookup", false)) {
                services.plugin().getLogger().info("CEE chunk-lookup stop: mundo=" + world.getName()
                    + " inputX=" + x + " inputZ=" + z
                    + " blockChunk=" + byBlocks.getX() + "," + byBlocks.getZ()
                    + " directChunk=" + byChunks.getX() + "," + byChunks.getZ()
                    + " found=" + handle.isPresent());
            }
        }

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
