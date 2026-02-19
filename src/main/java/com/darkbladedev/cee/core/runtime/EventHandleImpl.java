package com.darkbladedev.cee.core.runtime;

import java.util.UUID;

import com.darkbladedev.cee.api.EventHandle;
import com.darkbladedev.cee.api.EventState;

public final class EventHandleImpl implements EventHandle {
    private final EventRuntime runtime;

    public EventHandleImpl(EventRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String getEventId() {
        return runtime.getEventId();
    }

    @Override
    public UUID getRuntimeId() {
        return runtime.getRuntimeId();
    }

    @Override
    public EventState getState() {
        return runtime.getState();
    }

    @Override
    public void cancel() {
        runtime.setState(EventState.CANCELLED);
    }
}
