package com.darkbladedev.cee.core.commands.impl;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MessageService {
    private final Plugin plugin;
    private final boolean placeholderApiAvailable;
    private final Method placeholderMethod;
    private YamlConfiguration config;
    private String prefix;

    public MessageService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Method method = null;
        boolean available = false;
        try {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                method = api.getMethod("setPlaceholders", Player.class, String.class);
                available = true;
            }
        } catch (Exception ignored) {
            available = false;
            method = null;
        }
        this.placeholderApiAvailable = available;
        this.placeholderMethod = method;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = config.getString("prefix", "");
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = format(key, placeholders);
        if (message.isBlank()) {
            return;
        }
        if (placeholderApiAvailable && sender instanceof Player player && placeholderMethod != null) {
            try {
                message = String.valueOf(placeholderMethod.invoke(null, player, message));
            } catch (Exception ignored) {
            }
        }
        sender.sendMessage(message);
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, "");
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Map<String, String> resolved = new HashMap<>(placeholders);
        resolved.putIfAbsent("prefix", prefix);
        String output = raw;
        for (Map.Entry<String, String> entry : resolved.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', output);
    }
}
