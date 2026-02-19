package com.darkbladedev.cee.core.definition;

import java.util.Objects;

public final class ActionNodeDefinition implements FlowNodeDefinition {
    private final ActionDefinition action;

    public ActionNodeDefinition(ActionDefinition action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    public ActionDefinition getAction() {
        return action;
    }
}
