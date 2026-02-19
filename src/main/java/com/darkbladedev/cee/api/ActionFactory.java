package com.darkbladedev.cee.api;

import java.util.Map;

@FunctionalInterface
public interface ActionFactory {
    Action create(Map<String, Object> config);
}
