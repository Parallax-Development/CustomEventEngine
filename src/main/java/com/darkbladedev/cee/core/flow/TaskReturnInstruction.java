package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ValueResolver;

import java.util.HashMap;
import java.util.Map;

public final class TaskReturnInstruction implements Instruction {
    private final Map<String, Object> values;

    public TaskReturnInstruction(Map<String, Object> values) {
        this.values = values == null ? Map.of() : Map.copyOf(values);
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            resolved.put(entry.getKey(), ValueResolver.resolveValue(entry.getValue(), runtime.getContext()));
        }
        if (!runtime.completeCurrentTask(resolved)) {
            return ExecutionResult.STOP;
        }
        return ExecutionResult.CONTINUE;
    }
}

