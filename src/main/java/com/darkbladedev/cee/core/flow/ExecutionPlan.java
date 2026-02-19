package com.darkbladedev.cee.core.flow;

import java.util.List;
import java.util.Objects;

public final class ExecutionPlan {
    private final List<Instruction> instructions;

    public ExecutionPlan(List<Instruction> instructions) {
        this.instructions = List.copyOf(Objects.requireNonNull(instructions, "instructions"));
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public int size() {
        return instructions.size();
    }

    public Instruction get(int index) {
        return instructions.get(index);
    }
}
