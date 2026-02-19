package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.core.commands.exception.InvalidArgumentException;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public final class EventPurgeRegionCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public EventPurgeRegionCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "event purge region";
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
            ArgumentDefinition.required("x1", ArgumentType.INT),
            ArgumentDefinition.required("z1", ArgumentType.INT),
            ArgumentDefinition.required("x2", ArgumentType.INT),
            ArgumentDefinition.required("z2", ArgumentType.INT),
            ArgumentDefinition.optional("flag", ArgumentType.STRING)
                .suggestions((context, input) -> List.of("--include-schedulers"))
        );
    }

    @Override
    public String description() {
        return "Purgea todos los runtimes activos en una región (rectángulo), liberando locks y tasks.";
    }

    @Override
    public void execute(CommandContext context) {
        World world = context.argument("mundo", World.class);
        int x1 = context.argument("x1", Integer.class);
        int z1 = context.argument("z1", Integer.class);
        int x2 = context.argument("x2", Integer.class);
        int z2 = context.argument("z2", Integer.class);
        String flag = context.argument("flag", String.class);
        boolean includeSchedulers = parseIncludeSchedulersFlag(flag);

        int chunkX1 = Math.floorDiv(x1, 16);
        int chunkZ1 = Math.floorDiv(z1, 16);
        int chunkX2 = Math.floorDiv(x2, 16);
        int chunkZ2 = Math.floorDiv(z2, 16);

        var result = services.engine().purgeRegionChunks(world.getUID(), chunkX1, chunkZ1, chunkX2, chunkZ2, includeSchedulers);
        if (result.runtimesPurged() == 0) {
            var alt = services.engine().purgeRegionChunks(world.getUID(), x1, z1, x2, z2, includeSchedulers);
            if (alt.runtimesPurged() > 0) {
                result = alt;
            }

            if (services.plugin().getConfig().getBoolean("debug.chunk-lookup", false)) {
                services.plugin().getLogger().info("CEE chunk-lookup purge-region: mundo=" + world.getName()
                    + " input=(" + x1 + "," + z1 + ")->(" + x2 + "," + z2 + ")"
                    + " blockChunks=(" + chunkX1 + "," + chunkZ1 + ")->(" + chunkX2 + "," + chunkZ2 + ")"
                    + " purged=" + (result.runtimesPurged() > 0)
                    + " includeSchedulers=" + includeSchedulers);
            }
        }

        if (result.runtimesPurged() == 0) {
            messages.send(context.sender(), "event.purge.none", Map.of());
            return;
        }
        messages.send(context.sender(), "event.purge.region.success", Map.of(
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
