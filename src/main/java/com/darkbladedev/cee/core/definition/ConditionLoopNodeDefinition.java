package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class ConditionLoopNodeDefinition implements FlowNodeDefinition {
    private final int times;
    private final String expression;
    private final FlowDefinition flow;

    public ConditionLoopNodeDefinition(int times, String expression, FlowDefinition flow) {
        this.times = times;
        this.expression = Objects.requireNonNullElse(expression, "");
        this.flow = Objects.requireNonNull(flow, "flow");
    }

    public int getTimes() {
        return times;
    }

    public String getExpression() {
        return expression;
    }

    public FlowDefinition getFlow() {
        return flow;
    }
}
