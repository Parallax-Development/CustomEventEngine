package com.darkbladedev.cee.core.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TaskCallNodeDefinition implements FlowNodeDefinition {
    private final String taskName;
    private final Map<String, Object> arguments;
    private final Map<String, String> into;
    private final TaskDefinition override;
    private final int maxDepth;

    public TaskCallNodeDefinition(String taskName,
                                  Map<String, Object> arguments,
                                  Map<String, String> into,
                                  TaskDefinition override,
                                  int maxDepth) {
        this.taskName = Objects.requireNonNullElse(taskName, "").trim();
        Map<String, Object> args = new LinkedHashMap<>();
        if (arguments != null) {
            args.putAll(arguments);
        }
        this.arguments = Map.copyOf(args);

        Map<String, String> mapping = new LinkedHashMap<>();
        if (into != null) {
            mapping.putAll(into);
        }
        this.into = Map.copyOf(mapping);

        this.override = override;
        this.maxDepth = Math.max(1, maxDepth);
    }

    public String getTaskName() {
        return taskName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Map<String, String> getInto() {
        return into;
    }

    public TaskDefinition getOverride() {
        return override;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
