package com.darkbladedev.cee.core.definition;

import java.util.List;
import java.util.Objects;

public final class EventDefinition {
    private final String id;
    private final TriggerDefinition trigger;
    private final List<ConditionDefinition> conditions;
    private final FlowDefinition flow;
    private final ScopeDefinition scope;
    private final ExpansionDefinition expansion;
    private final ChunkTargetDefinition target;
    private final ChunkPolicy chunkPolicy;
    private final ChunkUnloadPolicy chunkUnloadPolicy;

    public EventDefinition(String id,
                           TriggerDefinition trigger,
                           List<ConditionDefinition> conditions,
                           FlowDefinition flow,
                           ScopeDefinition scope,
                           ExpansionDefinition expansion,
                           ChunkTargetDefinition target,
                           ChunkPolicy chunkPolicy,
                           ChunkUnloadPolicy chunkUnloadPolicy) {
        this.id = Objects.requireNonNull(id, "id");
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.conditions = List.copyOf(conditions);
        this.flow = Objects.requireNonNull(flow, "flow");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.expansion = Objects.requireNonNull(expansion, "expansion");
        this.target = Objects.requireNonNull(target, "target");
        this.chunkPolicy = Objects.requireNonNull(chunkPolicy, "chunkPolicy");
        this.chunkUnloadPolicy = Objects.requireNonNull(chunkUnloadPolicy, "chunkUnloadPolicy");
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

    public ChunkPolicy getChunkPolicy() {
        return chunkPolicy;
    }

    public ChunkUnloadPolicy getChunkUnloadPolicy() {
        return chunkUnloadPolicy;
    }
}
