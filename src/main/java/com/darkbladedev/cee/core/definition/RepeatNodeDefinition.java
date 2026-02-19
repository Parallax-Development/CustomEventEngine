package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class RepeatNodeDefinition implements FlowNodeDefinition {
    private final int times;
    private final long everyTicks;
    private final FlowDefinition flow;

    public RepeatNodeDefinition(int times, long everyTicks, FlowDefinition flow) {
        this.times = times;
        this.everyTicks = everyTicks;
        this.flow = Objects.requireNonNull(flow, "flow");
    }

    public int getTimes() {
        return times;
    }

    public long getEveryTicks() {
        return everyTicks;
    }

    public FlowDefinition getFlow() {
        return flow;
    }
}
