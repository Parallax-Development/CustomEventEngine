package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.runtime.EventRuntime;

public interface Instruction {
    ExecutionResult execute(EventRuntime runtime);
}
