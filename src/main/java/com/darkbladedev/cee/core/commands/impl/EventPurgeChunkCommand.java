package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.core.commands.exception.InvalidArgumentException;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public final class EventPurgeChunkCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;
    private final TargetResolver targetResolver;

    public EventPurgeChunkCommand(CommandServices services, MessageService messages, TargetResolver targetResolver) {
        this.services = services;
        this.messages = messages;
        this.targetResolver = targetResolver;
    }

    @Override
    public String name() {
        return "event purge";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public List<String> aliases() {
        return List.of("event purge chunk");
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
            ArgumentDefinition.optional("z", ArgumentType.INT),
            ArgumentDefinition.optional("flag", ArgumentType.STRING)
                .suggestions((context, input) -> List.of("--include-schedulers"))
        );
    }

    @Override
    public String description() {
        return "Purgea el runtime activo del chunk objetivo, liberando locks y tasks.";
    }

    @Override
    public void execute(CommandContext context) {
        Location location = targetResolver.resolveLocation(context);
        World world = context.argument("mundo", World.class);
        Integer x = context.argument("x", Integer.class);
        Integer z = context.argument("z", Integer.class);
        String flag = context.argument("flag", String.class);
        boolean includeSchedulers = parseIncludeSchedulersFlag(flag);

        var result = services.engine().purgeChunk(ChunkUtil.fromLocation(location), includeSchedulers);
        if (result.runtimesPurged() == 0 && world != null && x != null && z != null) {
            var byBlocks = ChunkUtil.fromBlockCoords(world.getUID(), x, z);
            var byChunks = ChunkUtil.fromWorld(world, x, z);
            if (!byChunks.equals(byBlocks)) {
                result = services.engine().purgeChunk(byBlocks, includeSchedulers);
                if (result.runtimesPurged() == 0) {
                    result = services.engine().purgeChunk(byChunks, includeSchedulers);
                }
            } else {
                result = services.engine().purgeChunk(byBlocks, includeSchedulers);
            }

            if (services.plugin().getConfig().getBoolean("debug.chunk-lookup", false)) {
                services.plugin().getLogger().info("CEE chunk-lookup purge: mundo=" + world.getName()
                    + " inputX=" + x + " inputZ=" + z
                    + " blockChunk=" + byBlocks.getX() + "," + byBlocks.getZ()
                    + " directChunk=" + byChunks.getX() + "," + byChunks.getZ()
                    + " purged=" + (result.runtimesPurged() > 0)
                    + " includeSchedulers=" + includeSchedulers);
            }
        }

        if (result.runtimesPurged() == 0) {
            messages.send(context.sender(), "event.purge.none", Map.of());
            return;
        }
        messages.send(context.sender(), "event.purge.chunk.success", Map.of(
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
