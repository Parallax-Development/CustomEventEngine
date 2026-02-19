package com.darkbladedev.cee.core.runtime;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.ActionFactory;
import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.CustomEventEngine;
import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.api.EventState;
import com.darkbladedev.cee.api.Scope;
import com.darkbladedev.cee.api.StartResult;
import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;
import com.darkbladedev.cee.api.TriggerFactory;
import com.darkbladedev.cee.core.definition.ActionDefinition;
import com.darkbladedev.cee.core.definition.ActionNodeDefinition;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.expansion.ExpansionManager;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import com.darkbladedev.cee.core.flow.FlowCompiler;
import com.darkbladedev.cee.core.loader.EventLoader;
import com.darkbladedev.cee.core.registry.EngineRegistry;
import com.darkbladedev.cee.core.trigger.IntervalTrigger;
import com.darkbladedev.cee.core.trigger.TriggerCallback;
import com.darkbladedev.cee.util.DurationParser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class EventEngine implements CustomEventEngine, TriggerCallback {
    private final Plugin plugin;
    private final EngineRegistry registry;
    private final EventLoader loader;
    private final ChunkEventLockManager lockManager;
    private final RuntimeScheduler scheduler;
    private final ConditionEvaluator conditionEvaluator;
    private final TerritorialTriggerDispatcher dispatcher;
    private final ExpansionManager expansionManager;
    private final Map<String, EventDefinition> definitions;
    private final Map<String, Trigger> triggers;
    private final Map<UUID, EventRuntime> runtimes;
    private final Map<UUID, EventDefinition> runtimeDefinitions;
    private final Map<UUID, Long> lastExpansionTick;
    private BukkitRunnable expansionTask;

    public EventEngine(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new EngineRegistry();
        this.loader = new EventLoader();
        this.lockManager = new ChunkEventLockManager();
        this.definitions = new HashMap<>();
        this.triggers = new HashMap<>();
        this.runtimes = new HashMap<>();
        this.runtimeDefinitions = new HashMap<>();
        this.lastExpansionTick = new HashMap<>();
        this.scheduler = new RuntimeScheduler(plugin, runtime -> {
            lockManager.release(runtime);
            runtimes.remove(runtime.getRuntimeId());
            runtimeDefinitions.remove(runtime.getRuntimeId());
            lastExpansionTick.remove(runtime.getRuntimeId());
        });
        this.conditionEvaluator = new ConditionEvaluator();
        this.dispatcher = new TerritorialTriggerDispatcher(registry, this);
        this.expansionManager = new ExpansionManager(lockManager);
    }

    public EngineRegistry getRegistry() {
        return registry;
    }

    public Map<String, EventDefinition> getDefinitions() {
        return definitions;
    }

    public RuntimeScheduler getScheduler() {
        return scheduler;
    }

    public ChunkEventLockManager getLockManager() {
        return lockManager;
    }

    public void loadDefinitions(File folder) {
        definitions.clear();
        for (EventDefinition definition : loader.loadFromFolder(folder)) {
            definitions.put(definition.getId(), definition);
        }
    }

    public void reloadDefinitions(File folder, Server server) {
        for (Trigger trigger : triggers.values()) {
            trigger.unregister();
        }
        triggers.clear();
        loadDefinitions(folder);
        registerTriggers(server);
    }

    public void registerTriggers(Server server) {
        for (EventDefinition definition : definitions.values()) {
            Trigger trigger = createTrigger(definition);
            triggers.put(definition.getId(), trigger);
            trigger.register(new TriggerContext(definition.getId(), null, null));
        }
    }

    public void startScheduler(long intervalTicks) {
        scheduler.start(intervalTicks);
    }

    public void startExpansionTask(long intervalTicks) {
        if (expansionTask != null) {
            return;
        }
        long period = Math.max(1L, intervalTicks);
        expansionTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (EventRuntime runtime : scheduler.getRuntimes().values()) {
                    EventDefinition definition = runtimeDefinitions.get(runtime.getRuntimeId());
                    if (definition == null) {
                        continue;
                    }
                    if (!definition.getExpansion().isEnabled()) {
                        continue;
                    }
                    long interval = definition.getExpansion().getIntervalTicks();
                    if (interval <= 0) {
                        interval = period;
                    }
                    long currentTick = runtime.getContext().getCurrentTick();
                    long last = lastExpansionTick.getOrDefault(runtime.getRuntimeId(), 0L);
                    if (currentTick - last < interval) {
                        continue;
                    }
                    expansionManager.tryExpand(runtime, runtime.getOrigin(), definition.getExpansion());
                    lastExpansionTick.put(runtime.getRuntimeId(), currentTick);
                }
            }
        };
        expansionTask.runTaskTimer(plugin, period, period);
    }

    public void shutdown() {
        for (Trigger trigger : triggers.values()) {
            trigger.unregister();
        }
        if (expansionTask != null) {
            expansionTask.cancel();
            expansionTask = null;
        }
        scheduler.stop();
    }

    public Optional<EventRuntime> getRuntime(ChunkPos chunkPos) {
        return Optional.ofNullable(lockManager.getRuntime(chunkPos));
    }

    @Override
    public Optional<EventHandle> getActiveEvent(ChunkPos chunkPos) {
        return getRuntime(chunkPos).map(EventHandleImpl::new);
    }

    @Override
    public StartResult startEvent(String eventId, ChunkPos chunkPos) {
        EventDefinition definition = definitions.get(eventId);
        if (definition == null) {
            return StartResult.NOT_FOUND;
        }
        if (lockManager.isOccupied(chunkPos)) {
            return StartResult.CHUNK_OCCUPIED;
        }
        World world = plugin.getServer().getWorld(chunkPos.getWorldId());
        if (world == null) {
            return StartResult.INVALID_TARGET;
        }
        EventContextImpl context = new EventContextImpl(plugin.getServer(), world);
        ExecutionPlan plan = createExecutionPlan(definition);
        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), definition.getId(), plan, context, chunkPos);
        runtime.setState(EventState.RUNNING);
        Scope scope = registry.getScopeFactory(definition.getScope().getType())
            .map(factory -> factory.create(definition.getScope().getConfig()))
            .orElse(origin -> Set.of(origin));
        if (!lockManager.tryLock(scope.resolveChunks(chunkPos), runtime)) {
            return StartResult.CHUNK_OCCUPIED;
        }
        List<com.darkbladedev.cee.api.Condition> conditions = definition.getConditions().stream()
            .map(cond -> registry.getConditionFactory(cond.getType())
                .map(factory -> factory.create(cond.getConfig()))
                .orElse(ctx -> true))
            .toList();
        if (!conditionEvaluator.evaluateAll(conditions, runtime)) {
            lockManager.release(runtime);
            return StartResult.CONDITIONS_FAILED;
        }
        addRuntime(runtime, definition);
        return StartResult.SUCCESS;
    }

    public void addRuntime(EventRuntime runtime, EventDefinition definition) {
        scheduler.addRuntime(runtime);
        runtimes.put(runtime.getRuntimeId(), runtime);
        runtimeDefinitions.put(runtime.getRuntimeId(), definition);
    }

    @Override
    public void registerAction(String id, ActionFactory factory) {
        registry.registerAction(id, factory);
    }

    @Override
    public void registerCondition(String id, com.darkbladedev.cee.api.ConditionFactory factory) {
        registry.registerCondition(id, factory);
    }

    @Override
    public void registerTrigger(String id, TriggerFactory factory) {
        registry.registerTrigger(id, factory);
    }

    @Override
    public void registerScope(String id, com.darkbladedev.cee.api.ScopeFactory factory) {
        registry.registerScope(id, factory);
    }

    @Override
    public void registerChunkStrategy(String id, com.darkbladedev.cee.api.ChunkSelectionStrategy strategy) {
        registry.registerChunkStrategy(id, strategy);
    }

    private Trigger createTrigger(EventDefinition definition) {
        TriggerFactory factory = registry.getTriggerFactory(definition.getTrigger().getType())
            .orElse((config, eventId) -> new IntervalTrigger(plugin, eventId, DurationParser.parseTicks(config.get("every")), this));
        return factory.create(definition.getTrigger().getConfig(), definition.getId());
    }

    public ExecutionPlan createExecutionPlan(EventDefinition definition) {
        return new FlowCompiler(this::buildAction).compile(definition.getFlow());
    }

    private Action buildAction(ActionNodeDefinition actionNode) {
        ActionDefinition definition = actionNode.getAction();
        return registry.getActionFactory(definition.getType())
            .map(factory -> factory.create(definition.getConfig()))
            .orElse(context -> {});
    }

    @Override
    public void onTrigger(TriggerContext context) {
        EventDefinition definition = definitions.get(context.getEventId());
        if (definition == null) {
            return;
        }
        dispatcher.dispatch(definition, context);
    }
}
