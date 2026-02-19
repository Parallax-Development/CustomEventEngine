package com.darkbladedev.cee.api;

import java.util.Map;

@FunctionalInterface
public interface ConditionFactory {
    Condition create(Map<String, Object> config);
}
