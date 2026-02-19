package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EventInspectCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;
    private final TargetResolver targetResolver;

    public EventInspectCommand(CommandServices services, MessageService messages, TargetResolver targetResolver) {
        this.services = services;
        this.messages = messages;
        this.targetResolver = targetResolver;
    }

    @Override
    public String name() {
        return "event inspect";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public List<String> aliases() {
        return List.of("event info");
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
        return "Muestra detalles del runtime activo.";
    }

    @Override
    public void execute(CommandContext context) {
        Location location = targetResolver.resolveLocation(context);
        World world = context.argument("mundo", World.class);
        Integer x = context.argument("x", Integer.class);
        Integer z = context.argument("z", Integer.class);

        Optional<EventRuntime> runtime = services.engine().getRuntime(ChunkUtil.fromLocation(location));
        if (runtime.isEmpty() && world != null && x != null && z != null) {
            var byBlocks = ChunkUtil.fromBlockCoords(world.getUID(), x, z);
            var byChunks = ChunkUtil.fromWorld(world, x, z);
            if (!byChunks.equals(byBlocks)) {
                runtime = services.engine().getRuntime(byBlocks);
                if (runtime.isEmpty()) {
                    runtime = services.engine().getRuntime(byChunks);
                }
            }

            if (services.plugin().getConfig().getBoolean("debug.chunk-lookup", false)) {
                services.plugin().getLogger().info("CEE chunk-lookup inspect: mundo=" + world.getName()
                    + " inputX=" + x + " inputZ=" + z
                    + " blockChunk=" + byBlocks.getX() + "," + byBlocks.getZ()
                    + " directChunk=" + byChunks.getX() + "," + byChunks.getZ()
                    + " found=" + runtime.isPresent());
            }
        }

        if (runtime.isEmpty()) {
            messages.send(context.sender(), "event.inspect.none", Map.of());
            return;
        }
        EventRuntime active = runtime.get();
        messages.send(context.sender(), "event.inspect.header", Map.of(
            "event", active.getEventId(),
            "state", active.getState().name()
        ));
        messages.send(context.sender(), "event.inspect.pointer", Map.of("value", String.valueOf(active.getInstructionPointer())));
        messages.send(context.sender(), "event.inspect.wait", Map.of("value", String.valueOf(active.getWaitRemaining())));
        messages.send(context.sender(), "event.inspect.participants", Map.of("value", String.valueOf(active.getParticipants().size())));
        messages.send(context.sender(), "event.inspect.locks", Map.of("value", String.valueOf(active.getChunkLocks().size())));
    }
}
