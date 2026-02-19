package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class LoopStartInstruction implements Instruction {
    private final int times;

    public LoopStartInstruction(int times) {
        this.times = times;
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        runtime.pushLoopCounter(times);
        return ExecutionResult.CONTINUE;
    }
}
