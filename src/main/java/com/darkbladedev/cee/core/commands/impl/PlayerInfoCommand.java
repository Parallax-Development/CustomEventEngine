package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.ArgumentType;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.util.ChunkUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerInfoCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public PlayerInfoCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "player info";
    }

    @Override
    public String permission() {
        return "cee.view";
    }

    @Override
    public List<String> aliases() {
        return List.of("p info");
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of(ArgumentDefinition.required("jugador", ArgumentType.STRING)
            .suggestions((context, input) -> services.plugin().getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .toList()));
    }

    @Override
    public String description() {
        return "Muestra el evento activo del jugador.";
    }

    @Override
    public void execute(CommandContext context) {
        String name = context.argument("jugador", String.class);
        Player target = services.plugin().getServer().getPlayerExact(name);
        if (target == null) {
            messages.send(context.sender(), "player.not-found", Map.of("player", name));
            return;
        }
        Optional<EventHandle> handle = services.engine().getActiveEvent(ChunkUtil.fromLocation(target.getLocation()));
        if (handle.isEmpty()) {
            messages.send(context.sender(), "player.no-event", Map.of("player", target.getName()));
            return;
        }
        messages.send(context.sender(), "player.event", Map.of(
            "player", target.getName(),
            "event", handle.get().getEventId(),
            "state", handle.get().getState().name()
        ));
    }
}
