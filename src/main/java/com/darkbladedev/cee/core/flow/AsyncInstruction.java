package com.darkbladedev.cee.core.flow;

import java.util.List;
import java.util.Objects;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class AsyncInstruction implements Instruction {
    private final List<ExecutionPlan> branches;

    public AsyncInstruction(List<ExecutionPlan> branches) {
        this.branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        if (!runtime.hasAsyncChildren()) {
            runtime.startAsync(branches);
            return ExecutionResult.WAIT;
        }
        if (runtime.areAsyncChildrenComplete()) {
            runtime.clearAsync();
            return ExecutionResult.CONTINUE;
        }
        return ExecutionResult.WAIT;
    }
}
