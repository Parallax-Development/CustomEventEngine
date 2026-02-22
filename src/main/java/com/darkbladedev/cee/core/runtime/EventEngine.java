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
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.definition.VariableDefinition;
import com.darkbladedev.cee.core.definition.VariableScope;
import com.darkbladedev.cee.core.definition.VariableType;
import com.darkbladedev.cee.core.expansion.ExpansionManager;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import com.darkbladedev.cee.core.flow.FlowCompiler;
import com.darkbladedev.cee.core.loader.EventLoader;
import com.darkbladedev.cee.core.loader.TaskLoader;
import com.darkbladedev.cee.core.loader.TaskValidator;
import com.darkbladedev.cee.core.registry.EngineRegistry;
import com.darkbladedev.cee.core.trigger.IntervalTrigger;
import com.darkbladedev.cee.core.trigger.TriggerCallback;
import com.darkbladedev.cee.util.DurationParser;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.mvel2.MVEL;

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
    private final Map<String, VariableDefinition> globalVariableDefinitions;
    private final Map<String, Object> globalVariables;
    private final Map<String, Serializable> expressionCache;
    private final TaskLoader taskLoader;
    private final TaskValidator taskValidator;
    private Map<String, TaskDefinition> globalTasks;
    private BukkitRunnable expansionTask;

    public EventEngine(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new EngineRegistry();
        this.loader = new EventLoader();
        this.taskLoader = new TaskLoader(loader);
        this.taskValidator = new TaskValidator();
        this.globalTasks = Map.of();
        this.lockManager = new ChunkEventLockManager();
        this.definitions = new HashMap<>();
        this.triggers = new HashMap<>();
        this.runtimes = new HashMap<>();
        this.runtimeDefinitions = new HashMap<>();
        this.lastExpansionTick = new HashMap<>();
        this.globalVariableDefinitions = new HashMap<>();
        this.globalVariables = new ConcurrentHashMap<>();
        this.expressionCache = new ConcurrentHashMap<>();
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
        File tasksFolder = new File(plugin.getDataFolder(), "tasks");
        loadDefinitions(folder, tasksFolder);
    }

    public void loadDefinitions(File eventsFolder, File tasksFolder) {
        Map<String, TaskDefinition> loadedTasks = taskLoader.loadFromFolder(tasksFolder);
        Map<String, TaskDefinition> validatedTasks = new HashMap<>(loadedTasks);
        for (Map.Entry<String, TaskDefinition> entry : loadedTasks.entrySet()) {
            List<String> errors = taskValidator.validate(entry.getValue(), loadedTasks);
            if (!errors.isEmpty()) {
                plugin.getLogger().warning("CEE: task '" + entry.getKey() + "' inválida. No se cargará.");
                for (String error : errors) {
                    plugin.getLogger().warning(" - " + error);
                }
                validatedTasks.remove(entry.getKey());
            }
        }
        globalTasks = validatedTasks;
        definitions.clear();
        for (EventDefinition definition : loader.loadFromFolder(eventsFolder)) {
            List<String> errors = taskValidator.validate(definition, globalTasks);
            if (!errors.isEmpty()) {
                plugin.getLogger().warning("CEE: evento '" + definition.getId() + "' inválido. No se cargará.");
                for (String error : errors) {
                    plugin.getLogger().warning(" - " + error);
                }
                continue;
            }
            definitions.put(definition.getId(), definition);
        }
        syncGlobalVariableDefinitions();
        syncGlobalVariables();
    }

    public void reloadDefinitions(File folder, Server server) {
        File tasksFolder = new File(plugin.getDataFolder(), "tasks");
        reloadDefinitions(folder, tasksFolder, server);
    }

    public void reloadDefinitions(File eventsFolder, File tasksFolder, Server server) {
        for (Trigger trigger : triggers.values()) {
            trigger.unregister();
        }
        triggers.clear();
        loadDefinitions(eventsFolder, tasksFolder);
        registerTriggers(server);
    }

    public Map<String, TaskDefinition> getGlobalTasks() {
        return globalTasks;
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

    public record PurgeResult(int runtimesPurged, int chunksFreed, int schedulersDisabled) {
    }

    public PurgeResult purgeWorld(World world) {
        return purgeWorld(world.getUID(), false);
    }

    public PurgeResult purgeWorld(UUID worldId) {
        return purgeWorld(worldId, false);
    }

    public PurgeResult purgeWorld(UUID worldId, boolean includeSchedulers) {
        return purgeByChunkPredicate(pos -> pos.getWorldId().equals(worldId), includeSchedulers);
    }

    public PurgeResult purgeRegionChunks(UUID worldId, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
        return purgeRegionChunks(worldId, chunkX1, chunkZ1, chunkX2, chunkZ2, false);
    }

    public PurgeResult purgeRegionChunks(UUID worldId, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2, boolean includeSchedulers) {
        int minX = Math.min(chunkX1, chunkX2);
        int maxX = Math.max(chunkX1, chunkX2);
        int minZ = Math.min(chunkZ1, chunkZ2);
        int maxZ = Math.max(chunkZ1, chunkZ2);
        return purgeByChunkPredicate(pos -> pos.getWorldId().equals(worldId)
            && pos.getX() >= minX && pos.getX() <= maxX
            && pos.getZ() >= minZ && pos.getZ() <= maxZ, includeSchedulers);
    }

    public PurgeResult purgeChunk(ChunkPos chunkPos) {
        return purgeChunk(chunkPos, false);
    }

    public PurgeResult purgeChunk(ChunkPos chunkPos, boolean includeSchedulers) {
        EventRuntime runtime = lockManager.getRuntime(chunkPos);
        if (runtime == null) {
            return new PurgeResult(0, 0, 0);
        }
        int chunksFreed = lockManager.getOccupiedChunks(runtime).size();
        int schedulersDisabled = 0;
        if (includeSchedulers) {
            disableIntervalSchedule(runtime.getEventId());
            schedulersDisabled = 1;
        }
        runtime.setState(EventState.CANCELLED);
        scheduler.purgeRuntime(runtime);
        return new PurgeResult(1, chunksFreed, schedulersDisabled);
    }

    public PurgeResult purgeRuntimes(Collection<EventRuntime> targets) {
        return purgeRuntimes(targets, false);
    }

    public PurgeResult purgeRuntimes(Collection<EventRuntime> targets, boolean includeSchedulers) {
        if (targets == null || targets.isEmpty()) {
            return new PurgeResult(0, 0, 0);
        }
        int chunksFreed = 0;
        int schedulersDisabled = 0;
        Set<String> disabled = new HashSet<>();
        for (EventRuntime runtime : targets) {
            if (runtime == null) {
                continue;
            }
            chunksFreed += lockManager.getOccupiedChunks(runtime).size();
            if (includeSchedulers && disabled.add(runtime.getEventId())) {
                disableIntervalSchedule(runtime.getEventId());
                schedulersDisabled++;
            }
            runtime.setState(EventState.CANCELLED);
            scheduler.purgeRuntime(runtime);
        }
        return new PurgeResult(targets.size(), chunksFreed, schedulersDisabled);
    }

    private PurgeResult purgeByChunkPredicate(Predicate<ChunkPos> predicate, boolean includeSchedulers) {
        Map<ChunkPos, EventRuntime> snapshot = lockManager.snapshotOccupied();
        Set<EventRuntime> runtimesToPurge = new HashSet<>();
        int chunksFreed = 0;
        for (Map.Entry<ChunkPos, EventRuntime> entry : snapshot.entrySet()) {
            if (!predicate.test(entry.getKey())) {
                continue;
            }
            chunksFreed++;
            runtimesToPurge.add(entry.getValue());
        }
        int schedulersDisabled = 0;
        Set<String> disabled = new HashSet<>();
        for (EventRuntime runtime : runtimesToPurge) {
            if (runtime == null) {
                continue;
            }
            if (includeSchedulers && disabled.add(runtime.getEventId())) {
                disableIntervalSchedule(runtime.getEventId());
                schedulersDisabled++;
            }
            runtime.setState(EventState.CANCELLED);
            scheduler.purgeRuntime(runtime);
        }
        return new PurgeResult(runtimesToPurge.size(), chunksFreed, schedulersDisabled);
    }

    public java.util.List<IntervalScheduleRegistry.IntervalStatus> getIntervalStatuses() {
        return scheduler.getIntervalStatuses();
    }

    public void disableIntervalSchedule(String eventId) {
        scheduler.unregisterInterval(eventId);
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
        EventContextImpl context = new EventContextImpl(plugin.getServer(), world, globalVariables, definition.getVariables(), globalVariableDefinitions);
        initializeContext(context, definition);
        EventRuntime runtime = createRuntime(definition, context, chunkPos);
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

    public EventRuntime createRuntime(EventDefinition definition, EventContextImpl context, ChunkPos origin) {
        FlowCompiler compiler = new FlowCompiler(this::buildAction);
        ExecutionPlan plan = compiler.compile(definition.getFlow());
        Map<String, TaskDefinition> tasks = new HashMap<>(globalTasks);
        tasks.putAll(definition.getTasks());
        return new EventRuntime(UUID.randomUUID(), definition.getId(), plan, compiler, context, origin, tasks);
    }

    public void addRuntime(EventRuntime runtime, EventDefinition definition) {
        scheduler.addRuntime(runtime);
        runtimes.put(runtime.getRuntimeId(), runtime);
        runtimeDefinitions.put(runtime.getRuntimeId(), definition);
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public Map<String, VariableDefinition> getGlobalVariableDefinitions() {
        return globalVariableDefinitions;
    }

    public void initializeContext(EventContextImpl context, EventDefinition definition) {
        initializeLocalVariables(context, definition.getVariables());
    }

    private void syncGlobalVariableDefinitions() {
        globalVariableDefinitions.clear();
        for (EventDefinition definition : definitions.values()) {
            for (Map.Entry<String, VariableDefinition> entry : definition.getVariables().entrySet()) {
                VariableDefinition variable = entry.getValue();
                if (variable.getScope() != VariableScope.GLOBAL) {
                    continue;
                }
                VariableDefinition existing = globalVariableDefinitions.get(entry.getKey());
                if (existing != null && existing.getType() != variable.getType()) {
                    throw new IllegalStateException("Variable global duplicada con tipo distinto: " + entry.getKey());
                }
                globalVariableDefinitions.put(entry.getKey(), variable);
            }
        }
    }

    private void syncGlobalVariables() {
        globalVariables.keySet().removeIf(key -> !globalVariableDefinitions.containsKey(key));

        Map<String, VariableDefinition> pending = new LinkedHashMap<>();
        for (Map.Entry<String, VariableDefinition> entry : globalVariableDefinitions.entrySet()) {
            String name = entry.getKey();
            VariableDefinition def = entry.getValue();
            if (globalVariables.containsKey(name)) {
                coerceToType(name, def.getType(), globalVariables.get(name));
                continue;
            }
            pending.put(name, def);
        }

        Map<String, Exception> lastErrors = new HashMap<>();
        boolean progressed = true;
        int passes = 0;
        while (!pending.isEmpty() && progressed && passes < 32) {
            progressed = false;
            passes++;
            for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, VariableDefinition> entry = it.next();
                String name = entry.getKey();
                VariableDefinition def = entry.getValue();
                try {
                    Object resolved = resolveInitialValue(name, def, globalVariables);
                    globalVariables.put(name, resolved);
                    it.remove();
                    progressed = true;
                } catch (Exception ex) {
                    lastErrors.put(name, ex);
                }
            }
        }

        if (!pending.isEmpty()) {
            StringBuilder message = new StringBuilder("No se pudieron resolver variables globales: ");
            boolean first = true;
            for (String name : pending.keySet()) {
                if (!first) {
                    message.append(", ");
                }
                first = false;
                Exception ex = lastErrors.get(name);
                if (ex == null) {
                    message.append(name);
                } else {
                    message.append(name).append(" (").append(ex.getMessage()).append(")");
                }
            }
            throw new IllegalStateException(message.toString());
        }
    }

    private void initializeLocalVariables(EventContextImpl context, Map<String, VariableDefinition> variables) {
        Map<String, VariableDefinition> pending = new LinkedHashMap<>();
        for (Map.Entry<String, VariableDefinition> entry : variables.entrySet()) {
            if (entry.getValue().getScope() == VariableScope.LOCAL) {
                pending.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Exception> lastErrors = new HashMap<>();
        boolean progressed = true;
        int passes = 0;
        while (!pending.isEmpty() && progressed && passes < 32) {
            progressed = false;
            passes++;
            for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, VariableDefinition> entry = it.next();
                String name = entry.getKey();
                VariableDefinition def = entry.getValue();
                try {
                    Object resolved = resolveInitialValue(name, def, context.getVariables());
                    context.setVariable(name, resolved);
                    it.remove();
                    progressed = true;
                } catch (Exception ex) {
                    lastErrors.put(name, ex);
                }
            }
        }

        if (!pending.isEmpty()) {
            StringBuilder message = new StringBuilder("No se pudieron resolver variables locales en '");
            message.append(context.getWorld().getName()).append("': ");
            boolean first = true;
            for (String name : pending.keySet()) {
                if (!first) {
                    message.append(", ");
                }
                first = false;
                Exception ex = lastErrors.get(name);
                if (ex == null) {
                    message.append(name);
                } else {
                    message.append(name).append(" (").append(ex.getMessage()).append(")");
                }
            }
            throw new IllegalStateException(message.toString());
        }
    }

    private Object resolveInitialValue(String name, VariableDefinition def, Map<String, Object> environment) {
        Object raw = def.getInitial();
        Object value;
        if (raw == null) {
            value = defaultForType(def.getType());
        } else if (raw instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.startsWith("=")) {
                String expr = trimmed.substring(1).trim();
                Serializable compiled = expressionCache.computeIfAbsent(expr, MVEL::compileExpression);
                value = MVEL.executeExpression(compiled, environment);
            } else if (trimmed.startsWith("${") && trimmed.endsWith("}") && trimmed.length() > 3) {
                String ref = trimmed.substring(2, trimmed.length() - 1).trim();
                if (!environment.containsKey(ref)) {
                    throw new IllegalStateException("Referencia aún no resuelta: " + ref);
                }
                value = environment.get(ref);
            } else {
                value = str;
            }
        } else {
            value = raw;
        }

        return coerceToType(name, def.getType(), value);
    }

    private Object defaultForType(VariableType type) {
        return switch (type) {
            case STRING -> "";
            case NUMBER -> 0;
            case BOOLEAN -> false;
            case ARRAY -> List.of();
            case OBJECT -> Map.of();
        };
    }

    private Object coerceToType(String name, VariableType type, Object value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case STRING -> {
                if (!(value instanceof String)) {
                    throw new IllegalStateException("Variable '" + name + "' debe ser string.");
                }
                yield value;
            }
            case NUMBER -> {
                if (!(value instanceof Number)) {
                    throw new IllegalStateException("Variable '" + name + "' debe ser number.");
                }
                yield value;
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    throw new IllegalStateException("Variable '" + name + "' debe ser boolean.");
                }
                yield value;
            }
            case ARRAY -> {
                if (!(value instanceof List<?>)) {
                    throw new IllegalStateException("Variable '" + name + "' debe ser array.");
                }
                yield value;
            }
            case OBJECT -> {
                if (!(value instanceof Map<?, ?>)) {
                    throw new IllegalStateException("Variable '" + name + "' debe ser object.");
                }
                yield value;
            }
        };
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
            .orElse((config, eventId) -> new IntervalTrigger(plugin, eventId, DurationParser.parseTicks(config.get("every")), this, scheduler));
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
