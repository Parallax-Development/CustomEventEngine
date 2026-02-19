package com.darkbladedev.cee.core.flow;

import java.util.List;
import java.util.Objects;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class ParallelInstruction implements Instruction {
    private final List<ExecutionPlan> branches;

    public ParallelInstruction(List<ExecutionPlan> branches) {
        this.branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        if (!runtime.hasParallelChildren()) {
            runtime.startParallel(branches);
            return ExecutionResult.WAIT;
        }
        if (runtime.areParallelChildrenComplete()) {
            runtime.clearParallel();
            return ExecutionResult.CONTINUE;
        }
        return ExecutionResult.WAIT;
    }
}
