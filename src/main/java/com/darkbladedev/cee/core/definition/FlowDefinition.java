package com.darkbladedev.cee.core.definition;

import java.util.List;
import java.util.Objects;

public final class FlowDefinition {
    private final List<FlowNodeDefinition> nodes;

    public FlowDefinition(List<FlowNodeDefinition> nodes) {
        this.nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
    }

    public List<FlowNodeDefinition> getNodes() {
        return nodes;
    }
}
