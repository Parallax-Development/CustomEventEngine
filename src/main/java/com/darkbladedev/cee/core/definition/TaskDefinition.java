package com.darkbladedev.cee.core.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TaskDefinition {
    private final String name;
    private final String description;
    private final Map<String, TaskParameterDefinition> parameters;
    private final Map<String, TaskReturnDefinition> returns;
    private final FlowDefinition flow;
    private final String source;

    public TaskDefinition(String name,
                          String description,
                          Map<String, TaskParameterDefinition> parameters,
                          Map<String, TaskReturnDefinition> returns,
                          FlowDefinition flow,
                          String source) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.flow = Objects.requireNonNull(flow, "flow");
        this.source = Objects.requireNonNullElse(source, "");

        Map<String, TaskParameterDefinition> params = new LinkedHashMap<>();
        if (parameters != null) {
            params.putAll(parameters);
        }
        this.parameters = Collections.unmodifiableMap(params);

        Map<String, TaskReturnDefinition> outs = new LinkedHashMap<>();
        if (returns != null) {
            outs.putAll(returns);
        }
        this.returns = Collections.unmodifiableMap(outs);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, TaskParameterDefinition> getParameters() {
        return parameters;
    }

    public Map<String, TaskReturnDefinition> getReturns() {
        return returns;
    }

    public FlowDefinition getFlow() {
        return flow;
    }

    public String getSource() {
        return source;
    }
}
