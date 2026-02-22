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
import com.darkbladedev.cee.core.commands.impl.EventPurgeChunkCommand;
import com.darkbladedev.cee.core.commands.impl.EventPurgeRegionCommand;
import com.darkbladedev.cee.core.commands.impl.EventPurgeWorldCommand;
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
import com.darkbladedev.cee.core.commands.impl.TasksExportCommand;
import com.darkbladedev.cee.core.actions.SpawnLightningAction;
import com.darkbladedev.cee.core.actions.ExecuteConsoleCommandAction;
import com.darkbladedev.cee.core.actions.ExecuteParticipantsCommandAction;
import com.darkbladedev.cee.core.actions.LogAction;
import com.darkbladedev.cee.core.conditions.MvelCondition;
import com.darkbladedev.cee.core.conditions.PlayersOnlineCondition;
import com.darkbladedev.cee.core.conditions.RandomChanceCondition;
import com.darkbladedev.cee.core.conditions.AnyParticipantHasPermissionCondition;
import com.darkbladedev.cee.core.conditions.IsDayCondition;
import com.darkbladedev.cee.core.conditions.IsNightCondition;
import com.darkbladedev.cee.core.conditions.IsRainingCondition;
import com.darkbladedev.cee.core.conditions.IsThunderingCondition;
import com.darkbladedev.cee.core.conditions.ParticipantsCountGreaterCondition;
import com.darkbladedev.cee.core.conditions.ParticipantsCountLessCondition;
import com.darkbladedev.cee.core.conditions.VariableEqualsCondition;
import com.darkbladedev.cee.core.conditions.WorldDifficultyIsCondition;
import com.darkbladedev.cee.core.conditions.WorldTimeRangeCondition;
import com.darkbladedev.cee.core.listener.ParticipantListener;
import com.darkbladedev.cee.core.persistence.PersistenceManager;
import com.darkbladedev.cee.core.runtime.ChunkSelectionStrategies;
import com.darkbladedev.cee.core.runtime.EventEngine;
import com.darkbladedev.cee.core.runtime.ScopeFactories;
import com.darkbladedev.cee.core.trigger.CommandTrigger;
import com.darkbladedev.cee.core.trigger.EntityDeathTrigger;
import com.darkbladedev.cee.util.DurationParser;
import com.darkbladedev.cee.core.trigger.MobSpawnTrigger;
import com.darkbladedev.cee.core.trigger.PlayerBlockBreakTrigger;
import com.darkbladedev.cee.core.trigger.PlayerBlockPlaceTrigger;
import com.darkbladedev.cee.core.trigger.PlayerChunkEnterTrigger;
import com.darkbladedev.cee.core.trigger.PlayerChunkExitTrigger;
import com.darkbladedev.cee.core.trigger.PlayerDamageTrigger;
import com.darkbladedev.cee.core.trigger.PlayerDeathTrigger;
import com.darkbladedev.cee.core.trigger.PlayerJoinTrigger;
import com.darkbladedev.cee.core.trigger.ThunderStartTrigger;
import com.darkbladedev.cee.core.trigger.WeatherChangeTrigger;
import com.darkbladedev.cee.core.trigger.WorldDifficultyPollingTrigger;

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
        commandManager.registerSubCommand(new EventPurgeChunkCommand(commandServices, messageService, targetResolver));
        commandManager.registerSubCommand(new EventPurgeWorldCommand(commandServices, messageService));
        commandManager.registerSubCommand(new EventPurgeRegionCommand(commandServices, messageService));
        commandManager.registerSubCommand(new PlayerInfoCommand(commandServices, messageService));
        commandManager.registerSubCommand(new TasksExportCommand(commandServices, messageService));
        commandManager.register();
    }

    private void registerDefaults() {
        engine.registerAction("broadcast", config -> new BroadcastAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("spawn_lightning", config -> new SpawnLightningAction());
        engine.registerAction("clear_weather", config -> new ClearWeatherAction());
        engine.registerAction("set_time", config -> new SetTimeAction(config.getOrDefault("time", 1000)));
        engine.registerAction("send_participants", config -> new SendParticipantsAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("send_message", config -> new SendParticipantsAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("set_variable", config -> new SetVariableAction(String.valueOf(config.getOrDefault("key", "")), config.get("value")));
        engine.registerAction("log", config -> new LogAction(String.valueOf(config.getOrDefault("message", ""))));
        engine.registerAction("execute_console_command", config -> new ExecuteConsoleCommandAction(String.valueOf(config.getOrDefault("command", ""))));
        engine.registerAction("execute_player_command", config -> new ExecuteParticipantsCommandAction(String.valueOf(config.getOrDefault("command", ""))));
        engine.registerCondition("players_online", config -> new PlayersOnlineCondition(parseInt(config.getOrDefault("min", 1))));
        engine.registerCondition("expression", config -> new MvelCondition(String.valueOf(config.getOrDefault("expression", "true"))));
        engine.registerCondition("world_time", config -> new WorldTimeRangeCondition(parseTicks(config.getOrDefault("min", 0), 0), parseTicks(config.getOrDefault("max", 23999), 23999)));
        engine.registerCondition("random_chance", config -> new RandomChanceCondition(parseDouble(config.getOrDefault("chance", 1.0), 1.0)));
        engine.registerCondition("variable_equals", config -> new VariableEqualsCondition(String.valueOf(config.getOrDefault("key", "")), String.valueOf(config.getOrDefault("value", ""))));
        engine.registerCondition("is_day", config -> new IsDayCondition());
        engine.registerCondition("is_night", config -> new IsNightCondition());
        engine.registerCondition("is_raining", config -> new IsRainingCondition());
        engine.registerCondition("is_thundering", config -> new IsThunderingCondition());
        engine.registerCondition("participants_count_gt", config -> new ParticipantsCountGreaterCondition(parseInt(config.getOrDefault("value", 0))));
        engine.registerCondition("participants_count_lt", config -> new ParticipantsCountLessCondition(parseInt(config.getOrDefault("value", 0))));
        engine.registerCondition("any_participant_has_permission", config -> new AnyParticipantHasPermissionCondition(String.valueOf(config.getOrDefault("permission", "")).trim()));
        engine.registerCondition("world_difficulty_is", config -> new WorldDifficultyIsCondition(parseDifficulty(config.getOrDefault("difficulty", "NORMAL"))));
        engine.registerTrigger("command", (config, eventId) -> new CommandTrigger(this, eventId, config, engine));
        engine.registerTrigger("on_player_join", (config, eventId) -> new PlayerJoinTrigger(this, eventId, engine));
        engine.registerTrigger("on_player_death_inside", (config, eventId) -> new PlayerDeathTrigger(this, eventId, engine));
        engine.registerTrigger("on_player_damage_inside", (config, eventId) -> new PlayerDamageTrigger(this, eventId, engine));
        engine.registerTrigger("on_player_break_block_inside", (config, eventId) -> new PlayerBlockBreakTrigger(this, eventId, engine));
        engine.registerTrigger("on_player_place_block_inside", (config, eventId) -> new PlayerBlockPlaceTrigger(this, eventId, engine));
        engine.registerTrigger("on_chunk_enter", (config, eventId) -> new PlayerChunkEnterTrigger(this, eventId, engine));
        engine.registerTrigger("on_chunk_exit", (config, eventId) -> new PlayerChunkExitTrigger(this, eventId, engine));
        engine.registerTrigger("on_weather_change", (config, eventId) -> new WeatherChangeTrigger(this, eventId, engine));
        engine.registerTrigger("on_thunder_start", (config, eventId) -> new ThunderStartTrigger(this, eventId, engine));
        engine.registerTrigger("on_world_difficulty_change", (config, eventId) -> new WorldDifficultyPollingTrigger(this, eventId, config, engine, engine.getScheduler()));
        engine.registerTrigger("on_entity_death_inside", (config, eventId) -> new EntityDeathTrigger(this, eventId, engine));
        engine.registerTrigger("on_mob_spawn_inside", (config, eventId) -> new MobSpawnTrigger(this, eventId, false, engine));
        engine.registerScope("chunk_radius", ScopeFactories.chunkRadius());
        engine.registerChunkStrategy("random_loaded_chunk", ChunkSelectionStrategies.randomLoadedChunk());
    }

    private org.bukkit.Difficulty parseDifficulty(Object value) {
        String raw = String.valueOf(value).trim();
        if (raw.isBlank()) {
            return org.bukkit.Difficulty.NORMAL;
        }
        try {
            return org.bukkit.Difficulty.valueOf(raw.toUpperCase());
        } catch (Exception ignored) {
            return org.bukkit.Difficulty.NORMAL;
        }
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
