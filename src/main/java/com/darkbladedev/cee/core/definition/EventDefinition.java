package com.darkbladedev.cee.core.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EventDefinition {
    private final String id;
    private final TriggerDefinition trigger;
    private final List<ConditionDefinition> conditions;
    private final FlowDefinition flow;
    private final ScopeDefinition scope;
    private final ExpansionDefinition expansion;
    private final ChunkTargetDefinition target;
    private final ChunkLoadRules chunkLoadRules;
    private final ChunkUnloadRules chunkUnloadRules;
    private final Map<String, VariableDefinition> variables;
    private final Map<String, TaskDefinition> tasks;

    public EventDefinition(String id,
                           TriggerDefinition trigger,
                           List<ConditionDefinition> conditions,
                           FlowDefinition flow,
                           ScopeDefinition scope,
                           ExpansionDefinition expansion,
                           ChunkTargetDefinition target,
                           ChunkLoadRules chunkLoadRules,
                           ChunkUnloadRules chunkUnloadRules,
                           Map<String, VariableDefinition> variables,
                           Map<String, TaskDefinition> tasks) {
        this.id = Objects.requireNonNull(id, "id");
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.conditions = List.copyOf(conditions);
        this.flow = Objects.requireNonNull(flow, "flow");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.expansion = Objects.requireNonNull(expansion, "expansion");
        this.target = Objects.requireNonNull(target, "target");
        this.chunkLoadRules = Objects.requireNonNull(chunkLoadRules, "chunkLoadRules");
        this.chunkUnloadRules = Objects.requireNonNull(chunkUnloadRules, "chunkUnloadRules");
        Map<String, VariableDefinition> map = new LinkedHashMap<>();
        if (variables != null) {
            map.putAll(variables);
        }
        this.variables = Collections.unmodifiableMap(map);

        Map<String, TaskDefinition> taskMap = new LinkedHashMap<>();
        if (tasks != null) {
            taskMap.putAll(tasks);
        }
        this.tasks = Collections.unmodifiableMap(taskMap);
    }

    public String getId() {
        return id;
    }

    public TriggerDefinition getTrigger() {
        return trigger;
    }

    public List<ConditionDefinition> getConditions() {
        return conditions;
    }

    public FlowDefinition getFlow() {
        return flow;
    }

    public ScopeDefinition getScope() {
        return scope;
    }

    public ExpansionDefinition getExpansion() {
        return expansion;
    }

    public ChunkTargetDefinition getTarget() {
        return target;
    }

    public ChunkLoadRules getChunkLoadRules() {
        return chunkLoadRules;
    }

    public ChunkUnloadRules getChunkUnloadRules() {
        return chunkUnloadRules;
    }

    public Map<String, VariableDefinition> getVariables() {
        return variables;
    }

    public Map<String, TaskDefinition> getTasks() {
        return tasks;
    }
}
