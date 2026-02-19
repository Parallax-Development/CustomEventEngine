package com.darkbladedev.cee.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class CommandMessages {
    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "CEE" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    private CommandMessages() {
    }

    public static void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.GRAY + message);
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.RED + message);
    }
}
