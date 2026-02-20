package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;

public final class ParticipantsCountGreaterCondition implements Condition {
    private final int thresholdExclusive;

    public ParticipantsCountGreaterCondition(int thresholdExclusive) {
        this.thresholdExclusive = thresholdExclusive;
    }

    @Override
    public boolean evaluate(EventContext context) {
        return context.getParticipants().size() > thresholdExclusive;
    }
}
