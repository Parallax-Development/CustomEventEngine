package com.darkbladedev.cee.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class HelpFormatter {
    public List<String> formatIndex(CommandSender sender, CommandHelpRegistry registry) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + "Comandos disponibles:");
        for (CommandInfo info : registry.list()) {
            if (!isAllowed(sender, info.permission())) {
                continue;
            }
            lines.add(ChatColor.GOLD + info.syntax() + ChatColor.GRAY + " - " + info.description());
        }
        return lines;
    }

    public List<String> formatDetail(CommandInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "Comando: " + info.name());
        lines.add(ChatColor.YELLOW + "Sintaxis: " + ChatColor.WHITE + info.syntax());
        lines.add(ChatColor.YELLOW + "Permiso: " + ChatColor.WHITE + info.permission());
        if (!info.aliases().isEmpty()) {
            lines.add(ChatColor.YELLOW + "Alias: " + ChatColor.WHITE + String.join(", ", info.aliases()));
        }
        lines.add(ChatColor.YELLOW + "Descripción: " + ChatColor.WHITE + info.description());
        if (!info.examples().isEmpty()) {
            lines.add(ChatColor.YELLOW + "Ejemplos:");
            for (String example : info.examples()) {
                lines.add(ChatColor.WHITE + "  " + example);
            }
        }
        return lines;
    }

    private boolean isAllowed(CommandSender sender, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return sender.hasPermission(permission);
    }
}
