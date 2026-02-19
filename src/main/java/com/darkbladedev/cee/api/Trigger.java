package com.darkbladedev.cee.api;

public interface Trigger {
    void register(TriggerContext context);
    void unregister();
}
