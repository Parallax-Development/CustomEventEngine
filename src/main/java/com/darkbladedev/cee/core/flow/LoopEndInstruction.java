package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class LoopEndInstruction implements Instruction {
    private final int loopStartIndex;
    private final long everyTicks;

    public LoopEndInstruction(int loopStartIndex, long everyTicks) {
        this.loopStartIndex = loopStartIndex;
        this.everyTicks = everyTicks;
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        if (runtime.decrementLoopCounter()) {
            if (everyTicks > 0) {
                runtime.waitFor(everyTicks);
                runtime.setInstructionPointer(loopStartIndex);
                return ExecutionResult.WAIT;
            }
            runtime.setInstructionPointer(loopStartIndex);
        } else {
            runtime.popLoopCounter();
        }
        return ExecutionResult.CONTINUE;
    }
}
