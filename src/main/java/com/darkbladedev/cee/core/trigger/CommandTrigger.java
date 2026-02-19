package com.darkbladedev.cee.core.trigger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommandTrigger implements Trigger, Listener {
    private static final List<String> DEFAULT_PREFIX = List.of("cee", "event", "start");

    private final Plugin plugin;
    private final String eventId;
    private final TriggerCallback callback;

    private final String configuredCommand;
    private final boolean cancel;
    private final String permission;

    private boolean registered;

    public CommandTrigger(Plugin plugin, String eventId, Map<String, Object> config, TriggerCallback callback) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.callback = Objects.requireNonNull(callback, "callback");

        this.configuredCommand = normalizeCommand(String.valueOf(config.getOrDefault("command", "")).trim());
        this.cancel = parseBoolean(config.getOrDefault("cancel", true));
        this.permission = String.valueOf(config.getOrDefault("permission", "cee.admin")).trim();
    }

    @Override
    public void register(TriggerContext context) {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            return;
        }

        List<String> tokens = tokenize(event.getMessage());
        if (tokens.isEmpty()) {
            return;
        }

        if (configuredCommand.isBlank()) {
            if (!matchesDefault(tokens)) {
                return;
            }
            Location location = resolveDefaultLocation(player, tokens);
            callback.onTrigger(new TriggerContext(eventId, location.getWorld(), location));
            if (cancel) {
                event.setCancelled(true);
            }
            return;
        }

        List<String> expectedTokens = tokenize(configuredCommand);
        if (expectedTokens.isEmpty()) {
            return;
        }
        if (!startsWithIgnoreCase(tokens, expectedTokens)) {
            return;
        }

        callback.onTrigger(new TriggerContext(eventId, player.getWorld(), player.getLocation()));
        if (cancel) {
            event.setCancelled(true);
        }
    }

    private boolean matchesDefault(List<String> tokens) {
        if (tokens.size() < 4) {
            return false;
        }
        if (!startsWithIgnoreCase(tokens, DEFAULT_PREFIX)) {
            return false;
        }
        return tokens.get(3).equalsIgnoreCase(eventId);
    }

    private Location resolveDefaultLocation(Player player, List<String> tokens) {
        if (tokens.size() >= 7) {
            String worldName = tokens.get(4);
            World world = Bukkit.getWorld(worldName);
            Integer x = parseInteger(tokens.get(5));
            Integer z = parseInteger(tokens.get(6));
            if (world != null && x != null && z != null) {
                int y = world.getHighestBlockYAt(x, z);
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return player.getLocation();
    }

    private boolean startsWithIgnoreCase(List<String> input, List<String> expectedPrefix) {
        if (input.size() < expectedPrefix.size()) {
            return false;
        }
        for (int i = 0; i < expectedPrefix.size(); i++) {
            if (!input.get(i).equalsIgnoreCase(expectedPrefix.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> tokenize(String raw) {
        String normalized = normalizeCommand(raw);
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] split = normalized.split("\\s+");
        List<String> result = new ArrayList<>(split.length);
        for (String part : split) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    private String normalizeCommand(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
