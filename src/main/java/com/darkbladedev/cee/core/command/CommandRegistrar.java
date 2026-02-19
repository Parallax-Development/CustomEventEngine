package com.darkbladedev.cee.core.command;

import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.api.StartResult;
import com.darkbladedev.cee.core.runtime.EventEngine;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ChunkUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class CommandRegistrar {
    private final Plugin plugin;
    private final EventEngine engine;
    private final CommandHelpRegistry helpRegistry;
    private final HelpFormatter helpFormatter;

    public CommandRegistrar(Plugin plugin, EventEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
        this.helpRegistry = new CommandHelpRegistry();
        this.helpFormatter = new HelpFormatter();
    }

    public void registerAll() {
        for (CommandInfo info : CommandDocumentation.defaultCommands()) {
            helpRegistry.register(info);
        }
        CommandAPICommand root = new CommandAPICommand("cee")
            .withAliases("ce")
            .withSubcommand(buildHelp())
            .withSubcommand(buildList())
            .withSubcommand(buildReload())
            .withSubcommand(buildEvent())
            .withSubcommand(buildPlayer());
        root.register();
    }

    private CommandAPICommand buildHelp() {
        StringArgument commandArg = new StringArgument("comando");
        commandArg.replaceSuggestions(ArgumentSuggestions.strings(info -> helpRegistry.commandNames()));
        return new CommandAPICommand("help")
            .withAliases("?")
            .withPermission("cee.help")
            .withOptionalArguments(commandArg)
            .executes((sender, args) -> {
                try {
                    String name = (String) args.get("comando");
                    if (name == null || name.isBlank()) {
                        for (String line : helpFormatter.formatIndex(sender, helpRegistry)) {
                            CommandMessages.info(sender, line);
                        }
                        return;
                    }
                    Optional<CommandInfo> info = helpRegistry.resolve(name);
                    if (info.isEmpty()) {
                        CommandMessages.error(sender, "Comando no encontrado: " + name);
                        return;
                    }
                    for (String line : helpFormatter.formatDetail(info.get())) {
                        CommandMessages.info(sender, line);
                    }
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al mostrar ayuda.");
                }
            });
    }

    private CommandAPICommand buildList() {
        IntegerArgument pageArg = new IntegerArgument("pagina", 1, 100);
        CommandAPICommand list = new CommandAPICommand("list")
            .withAliases("ls")
            .withPermission("cee.view")
            .withOptionalArguments(pageArg)
            .executes((sender, args) -> {
                try {
                    Integer pageValue = (Integer) args.get("pagina");
                    int page = pageValue == null ? 1 : pageValue;
                    List<String> ids = new ArrayList<>(engine.getDefinitions().keySet());
                    Collections.sort(ids);
                    if (ids.isEmpty()) {
                        CommandMessages.info(sender, "No hay eventos cargados.");
                        return;
                    }
                    int pageSize = 10;
                    int totalPages = Math.max(1, (int) Math.ceil(ids.size() / (double) pageSize));
                    if (page > totalPages) {
                        CommandMessages.error(sender, "Página inválida. Máximo: " + totalPages);
                        return;
                    }
                    int from = (page - 1) * pageSize;
                    int to = Math.min(ids.size(), from + pageSize);
                    List<String> slice = ids.subList(from, to);
                    CommandMessages.info(sender, "Eventos (" + page + "/" + totalPages + "): " + String.join(", ", slice));
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al listar eventos.");
                }
            });
        CommandAPICommand listAll = new CommandAPICommand("all")
            .withPermission("cee.view")
            .executes((sender, args) -> {
                List<String> ids = new ArrayList<>(engine.getDefinitions().keySet());
                Collections.sort(ids);
                if (ids.isEmpty()) {
                    CommandMessages.info(sender, "No hay eventos cargados.");
                    return;
                }
                CommandMessages.info(sender, "Eventos: " + String.join(", ", ids));
            });
        list.withSubcommand(listAll);
        return list;
    }

    private CommandAPICommand buildReload() {
        CommandAPICommand reload = new CommandAPICommand("reload")
            .withAliases("rl")
            .withPermission("cee.admin")
            .executes((sender, args) -> {
                try {
                    engine.reloadDefinitions(new File(plugin.getDataFolder(), "events"), plugin.getServer());
                    CommandMessages.success(sender, "Eventos recargados.");
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al recargar eventos.");
                }
            });
        CommandAPICommand silent = new CommandAPICommand("silent")
            .withPermission("cee.admin")
            .executes((sender, args) -> {
                try {
                    engine.reloadDefinitions(new File(plugin.getDataFolder(), "events"), plugin.getServer());
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al recargar eventos.");
                }
            });
        reload.withSubcommand(silent);
        return reload;
    }

    private CommandAPICommand buildEvent() {
        CommandAPICommand event = new CommandAPICommand("event")
            .withAliases("ev");
        event.withSubcommand(buildStart());
        event.withSubcommand(buildStop());
        event.withSubcommand(buildStatus());
        event.withSubcommand(buildInspect());
        return event;
    }

    private CommandAPICommand buildStart() {
        StringArgument eventArg = new StringArgument("evento");
        eventArg.replaceSuggestions(ArgumentSuggestions.strings(info -> engine.getDefinitions().keySet().toArray(new String[0])));
        LocationArgument locationArg = new LocationArgument("ubicacion");
        return new CommandAPICommand("start")
            .withAliases("run")
            .withPermission("cee.admin")
            .withArguments(eventArg)
            .withOptionalArguments(locationArg)
            .executes((sender, args) -> {
                try {
                    String eventId = (String) args.get("evento");
                    Location location = resolveLocation(sender, (Location) args.get("ubicacion"));
                    if (location == null) {
                        CommandMessages.error(sender, "Debes indicar una ubicación desde consola.");
                        return;
                    }
                    StartResult result = engine.startEvent(eventId, ChunkUtil.fromLocation(location));
                    handleStartResult(sender, eventId, result);
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al iniciar evento.");
                }
            });
    }

    private CommandAPICommand buildStop() {
        LocationArgument locationArg = new LocationArgument("ubicacion");
        return new CommandAPICommand("stop")
            .withAliases("end")
            .withPermission("cee.admin")
            .withOptionalArguments(locationArg)
            .executes((sender, args) -> {
                try {
                    Location location = resolveLocation(sender, (Location) args.get("ubicacion"));
                    if (location == null) {
                        CommandMessages.error(sender, "Debes indicar una ubicación desde consola.");
                        return;
                    }
                    Optional<EventHandle> handle = engine.getActiveEvent(ChunkUtil.fromLocation(location));
                    if (handle.isEmpty()) {
                        CommandMessages.info(sender, "No hay evento activo en ese chunk.");
                        return;
                    }
                    handle.get().cancel();
                    CommandMessages.success(sender, "Evento detenido: " + handle.get().getEventId());
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al detener evento.");
                }
            });
    }

    private CommandAPICommand buildStatus() {
        LocationArgument locationArg = new LocationArgument("ubicacion");
        return new CommandAPICommand("status")
            .withAliases("state")
            .withPermission("cee.view")
            .withOptionalArguments(locationArg)
            .executes((sender, args) -> {
                try {
                    Location location = resolveLocation(sender, (Location) args.get("ubicacion"));
                    if (location == null) {
                        CommandMessages.error(sender, "Debes indicar una ubicación desde consola.");
                        return;
                    }
                    Optional<EventHandle> handle = engine.getActiveEvent(ChunkUtil.fromLocation(location));
                    if (handle.isEmpty()) {
                        CommandMessages.info(sender, "No hay evento activo en ese chunk.");
                        return;
                    }
                    CommandMessages.info(sender, "Evento activo: " + handle.get().getEventId() + " (" + handle.get().getState() + ")");
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al consultar estado.");
                }
            });
    }

    private CommandAPICommand buildInspect() {
        LocationArgument locationArg = new LocationArgument("ubicacion");
        return new CommandAPICommand("inspect")
            .withAliases("info")
            .withPermission("cee.admin")
            .withOptionalArguments(locationArg)
            .executes((sender, args) -> {
                try {
                    Location location = resolveLocation(sender, (Location) args.get("ubicacion"));
                    if (location == null) {
                        CommandMessages.error(sender, "Debes indicar una ubicación desde consola.");
                        return;
                    }
                    Optional<EventRuntime> runtime = engine.getRuntime(ChunkUtil.fromLocation(location));
                    if (runtime.isEmpty()) {
                        CommandMessages.info(sender, "No hay runtime activo en ese chunk.");
                        return;
                    }
                    EventRuntime active = runtime.get();
                    CommandMessages.info(sender, "Evento: " + active.getEventId());
                    CommandMessages.info(sender, "Estado: " + active.getState());
                    CommandMessages.info(sender, "InstructionPointer: " + active.getInstructionPointer());
                    CommandMessages.info(sender, "Espera restante: " + active.getWaitRemaining() + " ticks");
                    CommandMessages.info(sender, "Participantes: " + active.getParticipants().size());
                    CommandMessages.info(sender, "Chunks bloqueados: " + active.getChunkLocks().size());
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al inspeccionar runtime.");
                }
            });
    }

    private CommandAPICommand buildPlayer() {
        CommandAPICommand player = new CommandAPICommand("player")
            .withAliases("p");
        CommandAPICommand info = new CommandAPICommand("info")
            .withPermission("cee.view")
            .withArguments(new EntitySelectorArgument.OnePlayer("jugador"))
            .executes((sender, args) -> {
                try {
                    Player target = (Player) args.get("jugador");
                    Optional<EventHandle> handle = engine.getActiveEvent(ChunkUtil.fromLocation(target.getLocation()));
                    if (handle.isEmpty()) {
                        CommandMessages.info(sender, "El jugador no está en un evento activo.");
                        return;
                    }
                    CommandMessages.info(sender, "Jugador en evento: " + handle.get().getEventId() + " (" + handle.get().getState() + ")");
                } catch (Exception ex) {
                    handleException(sender, ex, "Error interno al consultar jugador.");
                }
            });
        player.withSubcommand(info);
        return player;
    }

    private Location resolveLocation(CommandSender sender, Location location) {
        if (location != null) {
            return location;
        }
        if (sender instanceof Player player) {
            return player.getLocation();
        }
        return null;
    }

    private void handleStartResult(CommandSender sender, String eventId, StartResult result) {
        switch (result) {
            case SUCCESS -> CommandMessages.success(sender, "Evento iniciado: " + eventId);
            case NOT_FOUND -> CommandMessages.error(sender, "Evento no encontrado: " + eventId);
            case CHUNK_OCCUPIED -> CommandMessages.error(sender, "Chunk ocupado. Evento no iniciado.");
            case INVALID_TARGET -> CommandMessages.error(sender, "Mundo inválido para el evento.");
            case CONDITIONS_FAILED -> CommandMessages.error(sender, "Condiciones del evento no cumplidas.");
        }
    }

    private void handleException(CommandSender sender, Exception ex, String fallback) {
        if (ex instanceof RuntimeException runtime && runtime.getCause() instanceof Exception) {
            ex = (Exception) runtime.getCause();
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = fallback;
        }
        CommandMessages.error(sender, message);
    }
}
