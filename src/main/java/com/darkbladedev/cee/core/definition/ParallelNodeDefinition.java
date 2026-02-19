package com.darkbladedev.cee.core.definition;

import java.util.List;
import java.util.Objects;

public final class ParallelNodeDefinition implements FlowNodeDefinition {
    private final List<FlowDefinition> branches;

    public ParallelNodeDefinition(List<FlowDefinition> branches) {
        this.branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    }

    public List<FlowDefinition> getBranches() {
        return branches;
    }
}
