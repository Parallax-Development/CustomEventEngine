package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class IsThunderingCondition implements Condition {
    @Override
    public boolean evaluate(EventContext context) {
        return context.getWorld().isThundering();
    }
}
