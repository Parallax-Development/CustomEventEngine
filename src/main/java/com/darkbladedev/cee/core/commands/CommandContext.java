package com.darkbladedev.cee.core.commands;

import com.darkbladedev.cee.core.commands.impl.CommandServices;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandContext {
    private final CommandSender sender;
    private final Player player;
    private final Map<String, Object> arguments;
    private final List<String> rawArguments;
    private final CommandServices services;

    public CommandContext(CommandSender sender,
                          Player player,
                          Map<String, Object> arguments,
                          List<String> rawArguments,
                          CommandServices services) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.player = player;
        this.arguments = Collections.unmodifiableMap(arguments);
        this.rawArguments = List.copyOf(rawArguments);
        this.services = Objects.requireNonNull(services, "services");
    }

    public CommandSender sender() {
        return sender;
    }

    public Optional<Player> player() {
        return Optional.ofNullable(player);
    }

    public boolean isPlayer() {
        return player != null;
    }

    public Map<String, Object> arguments() {
        return arguments;
    }

    public List<String> rawArguments() {
        return rawArguments;
    }

    public CommandServices services() {
        return services;
    }

    public Object argument(String name) {
        return arguments.get(name);
    }

    public <T> T argument(String name, Class<T> type) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
}
