package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class DelayInstruction implements Instruction {
    private final long ticks;

    public DelayInstruction(long ticks) {
        this.ticks = ticks;
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        if (!runtime.isWaiting()) {
            runtime.waitFor(ticks);
            return ExecutionResult.WAIT;
        }
        if (runtime.isWaitingComplete()) {
            runtime.clearWaiting();
            return ExecutionResult.CONTINUE;
        }
        return ExecutionResult.WAIT;
    }
}
