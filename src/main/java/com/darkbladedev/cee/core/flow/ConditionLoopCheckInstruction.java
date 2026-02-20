package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Objects;

public final class ConditionLoopCheckInstruction implements Instruction {
    private final Serializable compiled;
    private final int afterLoopIndex;

    public ConditionLoopCheckInstruction(String expression, int afterLoopIndex) {
        this.compiled = MVEL.compileExpression(Objects.requireNonNullElse(expression, ""));
        this.afterLoopIndex = afterLoopIndex;
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        Object result = MVEL.executeExpression(compiled, runtime.getContext().getVariables());
        if (result instanceof Boolean ok && ok) {
            return ExecutionResult.CONTINUE;
        }
        runtime.popLoopCounter();
        runtime.setInstructionPointer(afterLoopIndex - 1);
        return ExecutionResult.CONTINUE;
    }
}
