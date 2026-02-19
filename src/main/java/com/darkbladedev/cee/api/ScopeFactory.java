package com.darkbladedev.cee.api;

import java.util.Map;

@FunctionalInterface
public interface ScopeFactory {
    Scope create(Map<String, Object> config);
}
