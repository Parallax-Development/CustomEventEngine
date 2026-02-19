package com.darkbladedev.cee.core.flow;

import java.util.Objects;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class ActionInstruction implements Instruction {
    private final Action action;

    public ActionInstruction(Action action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        action.execute(runtime.getContext());
        return ExecutionResult.CONTINUE;
    }
}
