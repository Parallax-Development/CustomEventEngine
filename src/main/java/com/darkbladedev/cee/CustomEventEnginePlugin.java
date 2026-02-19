package com.darkbladedev.cee;

import org.bukkit.plugin.java.JavaPlugin;

import com.darkbladedev.cee.core.actions.BroadcastAction;
import com.darkbladedev.cee.core.actions.ClearWeatherAction;
import com.darkbladedev.cee.core.actions.SendParticipantsAction;
import com.darkbladedev.cee.core.actions.SetTimeAction;
import com.darkbladedev.cee.core.actions.SetVariableAction;
import com.darkbladedev.cee.core.commands.ArgumentParser;
import com.darkbladedev.cee.core.commands.BaseCommand;
import com.darkbladedev.cee.core.commands.CommandManager;
import com.darkbladedev.cee.core.commands.CommandRegistry;
import com.darkbladedev.cee.core.commands.TabCompleterEngine;
import com.darkbladedev.cee.core.commands.impl.CommandServices;
import com.darkbladedev.cee.core.commands.impl.EventInspectCommand;
import com.darkbladedev.cee.core.commands.impl.EventStartCommand;
import com.darkbladedev.cee.core.commands.impl.EventStatusCommand;
import com.darkbladedev.cee.core.commands.impl.EventStopCommand;
import com.darkbladedev.cee.core.commands.impl.HelpCommand;
import com.darkbladedev.cee.core.commands.impl.ListAllCommand;
import com.darkbladedev.cee.core.commands.impl.ListCommand;
import com.darkbladedev.cee.core.commands.impl.MessageService;
import com.darkbladedev.cee.core.commands.impl.PlayerInfoCommand;
import com.darkbladedev.cee.core.commands.impl.ReloadCommand;
import com.darkbladedev.cee.core.commands.impl.ReloadSilentCommand;
import com.darkbladedev.cee.core.commands.impl.TargetResolver;
import com.darkbladedev.cee.core.actions.SpawnLightningAction;
import com.darkbladedev.cee.core.conditions.MvelCondition;
import com.darkbladedev.cee.core.conditions.PlayersOnlineCondition;
import com.darkbladedev.cee.core.conditions.RandomChanceCondition;
import com.darkbladedev.cee.core.conditions.VariableEqualsCondition;
import com.darkbladedev.cee.core.conditions.WorldTimeRangeCondition;
import com.darkbladedev.cee.core.listener.ParticipantListener;
import com.darkbladedev.cee.core.persistence.PersistenceManager;
import com.darkbladedev.cee.core.runtime.ChunkSelectionStrategies;
import com.darkbladedev.cee.core.runtime.EventEngine;
import com.darkbladedev.cee.core.runtime.ScopeFactories;
import com.darkbladedev.cee.core.trigger.IntervalTrigger;
import com.darkbladedev.cee.util.DurationParser;

import java.io.File;
import java.util.List;

public final class CustomEventEnginePlugin extends JavaPlugin {
    private EventEngine engine;
    private PersistenceManager persistence;
    private CommandManager commandManager;
    private MessageService messageService;
    private CommandServices commandServices;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        engine = new EventEngine(this);
        persistence = new PersistenceManager(this, engine);
        registerDefaults();
        File eventsFolder = new File(getDataFolder(), "events");
        if (!eventsFolder.exists()) {
            eventsFolder.mkdirs();
        }
        engine.loadDefinitions(eventsFolder);
        engine.registerTriggers(getServer());
        long interval = getConfig().getLong("scheduler.tick-interval", 1L);
        engine.startScheduler(interval);
        long expansionInterval = getConfig().getLong("expansion.interval-ticks", 20L);
        engine.startExpansionTask(expansionInterval);
        persistence.load(engine.getDefinitions());
        getServer().getPluginManager().registerEvents(new ParticipantListener(engine), this);
        setupCommands();
    }

    @Override
    public void onDisable() {
        if (persistence != null) {
            persistence.save();
        }
        if (engine != null) {
            engine.shutdown();
        }
    }

    private void setupCommands() {
        messageService = new MessageService(this);
        commandServices = new CommandServices(this, engine, messageService);
        CommandRegistry registry = new CommandRegistry();
        ArgumentParser parser = new ArgumentParser();
        TabCompleterEngine tabEngine = new TabCompleterEngine(registry, parser, commandServices);
        BaseCommand baseCommand = new BaseCommand("cee", List.of(), "", "help");
        commandManager = new CommandManager(this, baseCommand, registry, parser, tabEngine, commandServices);
        TargetResolver targetResolver = new TargetResolver();
        commandManager.registerSubCommand(new HelpCommand(registry, messageService, baseCommand.name()));
        commandManager.registerSubCommand(new ListCommand(commandServices, messageService));
        commandManager.registerSubCommand(new ListAllCommand(commandServices, messageService));
        commandManager.registerSubCommand(new ReloadCommand(commandServices, messageService));
        commandManager.registerSubCommand(new ReloadSilentCommand(commandServices, messageService));
        commandManager.registerSubCommand(new EventStartCommand(commandServices, messageService, targetResolver));
        commandManager.registerSubCommand(new EventStopCommand(commandServices, messageService, targetResolver));
        commandManager.registerSubCommand(new EventStatusCommand(commandServices, messageService, targetResolver));
        commandManager.registerSubCommand(new EventInspectCommand(commandServices, messageService, targetResolver));
        commandManager.registerSubCommand(new PlayerInfoCommand(commandServices, messageService));
        commandManager.register();
    }

    private void registerDefaults() {
        engine.registerAction("broadcast", config -> new BroadcastAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("spawn_lightning", config -> new SpawnLightningAction());
        engine.registerAction("clear_weather", config -> new ClearWeatherAction());
        engine.registerAction("set_time", config -> new SetTimeAction(parseTicks(config.getOrDefault("time", 1000), 1000)));
        engine.registerAction("send_participants", config -> new SendParticipantsAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("set_variable", config -> new SetVariableAction(String.valueOf(config.getOrDefault("key", "")), config.get("value")));
        engine.registerCondition("players_online", config -> new PlayersOnlineCondition(parseInt(config.getOrDefault("min", 1))));
        engine.registerCondition("expression", config -> new MvelCondition(String.valueOf(config.getOrDefault("expression", "true"))));
        engine.registerCondition("world_time", config -> new WorldTimeRangeCondition(parseTicks(config.getOrDefault("min", 0), 0), parseTicks(config.getOrDefault("max", 23999), 23999)));
        engine.registerCondition("random_chance", config -> new RandomChanceCondition(parseDouble(config.getOrDefault("chance", 1.0), 1.0)));
        engine.registerCondition("variable_equals", config -> new VariableEqualsCondition(String.valueOf(config.getOrDefault("key", "")), String.valueOf(config.getOrDefault("value", ""))));
        engine.registerTrigger("interval", (config, eventId) -> new IntervalTrigger(this, eventId, DurationParser.parseTicks(config.get("every")), engine));
        engine.registerScope("chunk_radius", ScopeFactories.chunkRadius());
        engine.registerChunkStrategy("random_loaded_chunk", ChunkSelectionStrategies.randomLoadedChunk());
    }

    private int parseInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private double parseDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long parseTicks(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return DurationParser.parseTicks(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
