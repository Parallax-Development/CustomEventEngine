package com.darkbladedev.cee.api;

import java.util.Map;

@FunctionalInterface
public interface TriggerFactory {
    Trigger create(Map<String, Object> config, String eventId);
}
