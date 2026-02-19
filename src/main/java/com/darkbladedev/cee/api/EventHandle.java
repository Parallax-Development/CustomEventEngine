package com.darkbladedev.cee.api;

import java.util.UUID;

public interface EventHandle {
    String getEventId();
    UUID getRuntimeId();
    EventState getState();
    void cancel();
}
