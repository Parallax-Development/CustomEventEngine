package com.darkbladedev.cee.core.definition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TaskReturnNodeDefinition implements FlowNodeDefinition {
    private final Map<String, Object> values;

    public TaskReturnNodeDefinition(Map<String, Object> values) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (values != null) {
            map.putAll(values);
        }
        this.values = Map.copyOf(map);
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
