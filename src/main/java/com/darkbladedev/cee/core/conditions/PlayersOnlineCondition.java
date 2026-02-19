package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class PlayersOnlineCondition implements Condition {
    private final int minimum;

    public PlayersOnlineCondition(int minimum) {
        this.minimum = minimum;
    }

    @Override
    public boolean evaluate(EventContext context) {
        return context.getServer().getOnlinePlayers().size() >= minimum;
    }
}
