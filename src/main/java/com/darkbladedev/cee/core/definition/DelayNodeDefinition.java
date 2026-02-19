package com.darkbladedev.cee.core.definition;

public final class DelayNodeDefinition implements FlowNodeDefinition {
    private final long ticks;

    public DelayNodeDefinition(long ticks) {
        this.ticks = ticks;
    }

    public long getTicks() {
        return ticks;
    }
}
