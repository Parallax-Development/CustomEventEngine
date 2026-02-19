package com.darkbladedev.cee.core.actions;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.api.EventContext;

public final class SetVariableAction implements Action {
    private final String key;
    private final Object value;

    public SetVariableAction(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void execute(EventContext context) {
        if (key == null || key.isBlank()) {
            return;
        }
        context.setVariable(key, value);
    }
}
