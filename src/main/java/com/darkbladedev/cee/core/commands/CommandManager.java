package com.darkbladedev.cee.core.commands;

import com.darkbladedev.cee.core.commands.exception.CommandException;
import com.darkbladedev.cee.core.commands.exception.NoPermissionException;
import com.darkbladedev.cee.core.commands.exception.PlayerOnlyException;
import com.darkbladedev.cee.core.commands.exception.UnknownSubCommandException;
import com.darkbladedev.cee.core.commands.impl.CommandServices;
import com.darkbladedev.cee.core.commands.impl.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

public final class CommandManager implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final BaseCommand baseCommand;
    private final CommandRegistry registry;
    private final ArgumentParser parser;
    private final TabCompleterEngine tabCompleter;
    private final CommandServices services;

    public CommandManager(Plugin plugin,
                          BaseCommand baseCommand,
                          CommandRegistry registry,
                          ArgumentParser parser,
                          TabCompleterEngine tabCompleter,
                          CommandServices services) {
        this.plugin = plugin;
        this.baseCommand = baseCommand;
        this.registry = registry;
        this.parser = parser;
        this.tabCompleter = tabCompleter;
        this.services = services;
    }

    public void register() {
        PluginCommand command = plugin.getServer().getPluginCommand(baseCommand.name());
        if (command == null) {
            plugin.getLogger().warning("No se encontró el comando raíz: " + baseCommand.name());
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    public void registerSubCommand(SubCommand command) {
        registry.register(command);
    }

    public void refreshCaches() {
        services.refreshCaches();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> arguments = args.length == 0 ? List.of(baseCommand.defaultSubcommand()) : List.of(args);
        try {
            CommandRegistry.Match match = registry.match(arguments);
            if (match == null) {
                throw new UnknownSubCommandException(arguments.isEmpty() ? "" : arguments.get(0));
            }
            SubCommand subCommand = match.command();
            String permission = subCommand.permission();
            if (!permission.isBlank() && !hasPermission(sender, permission)) {
                throw new NoPermissionException(permission);
            }
            if (subCommand.playerOnly() && !(sender instanceof Player)) {
                throw new PlayerOnlyException();
            }
            List<String> remainingArgs = arguments.subList(match.consumed(), arguments.size());
            Map<String, Object> parsed = parser.parse(subCommand.arguments(), remainingArgs, services);
            Player player = sender instanceof Player casted ? casted : null;
            CommandContext context = new CommandContext(sender, player, parsed, remainingArgs, services);
            subCommand.execute(context);
            return true;
        } catch (CommandException ex) {
            handleCommandException(sender, ex);
            return true;
        } catch (Exception ex) {
            MessageService messages = services.messages();
            messages.send(sender, "errors.internal", Map.of("message", ex.getMessage() == null ? "" : ex.getMessage()));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        CommandContext context = new CommandContext(sender, sender instanceof Player casted ? casted : null, Map.of(), List.of(), services);
        return tabCompleter.suggest(context, args);
    }

    private void handleCommandException(CommandSender sender, CommandException ex) {
        MessageService messages = services.messages();
        if (ex instanceof NoPermissionException permissionException) {
            messages.send(sender, ex.messageKey(), Map.of("permission", permissionException.permission()));
            return;
        }
        if (ex instanceof UnknownSubCommandException unknown) {
            messages.send(sender, ex.messageKey(), Map.of("input", unknown.input()));
            return;
        }
        messages.send(sender, ex.messageKey(), Map.of("message", ex.getMessage() == null ? "" : ex.getMessage()));
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (permission.isBlank()) {
            return true;
        }
        if (sender.hasPermission(permission)) {
            return true;
        }
        String[] parts = permission.split("\\.");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                current.append('.');
            }
            current.append(parts[i]);
            String wildcard = current + ".*";
            if (sender.hasPermission(wildcard)) {
                return true;
            }
        }
        return sender.hasPermission("*") || sender.hasPermission(baseCommand.permission());
    }
}
