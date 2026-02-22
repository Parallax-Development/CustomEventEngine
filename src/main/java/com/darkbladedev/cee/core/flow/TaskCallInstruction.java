package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.definition.TaskParameterDefinition;
import com.darkbladedev.cee.core.runtime.EventContextImpl;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ValueResolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TaskCallInstruction implements Instruction {
    private final String taskName;
    private final Map<String, Object> arguments;
    private final Map<String, String> into;
    private final TaskDefinition override;
    private final int maxDepth;

    public TaskCallInstruction(String taskName,
                               Map<String, Object> arguments,
                               Map<String, String> into,
                               TaskDefinition override,
                               int maxDepth) {
        this.taskName = Objects.requireNonNullElse(taskName, "").trim();
        this.arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        this.into = into == null ? Map.of() : Map.copyOf(into);
        this.override = override;
        this.maxDepth = Math.max(1, maxDepth);
    }

    @Override
    public ExecutionResult execute(EventRuntime runtime) {
        TaskDefinition task = override != null ? override : runtime.getTaskDefinition(taskName);
        if (task == null) {
            runtime.fail("Task inexistente: " + taskName);
            return ExecutionResult.STOP;
        }
        if (runtime.getTaskDepth() + 1 > maxDepth) {
            runtime.fail("Task max_depth excedido (" + maxDepth + "): " + task.getName());
            return ExecutionResult.STOP;
        }

        EventContextImpl context = runtime.getContext();
        Map<String, Object> resolvedArgs = new HashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            resolvedArgs.put(entry.getKey(), ValueResolver.resolveValue(entry.getValue(), context));
        }

        Map<String, Object> localBackup = new HashMap<>();
        Set<String> localMissing = new HashSet<>();
        Map<String, Object> globalBackup = new HashMap<>();
        Set<String> globalMissing = new HashSet<>();

        Map<String, Object> local = context.getLocalVariables();
        Map<String, Object> global = context.getGlobalVariables();

        for (TaskParameterDefinition param : task.getParameters().values()) {
            String key = param.getName();
            if (local.containsKey(key)) {
                localBackup.put(key, local.get(key));
            } else {
                localMissing.add(key);
            }
            if (global.containsKey(key)) {
                globalBackup.put(key, global.get(key));
            } else {
                globalMissing.add(key);
            }

            Object value;
            if (resolvedArgs.containsKey(key)) {
                value = resolvedArgs.get(key);
            } else if (param.getDefaultValue() != null) {
                value = ValueResolver.resolveValue(param.getDefaultValue(), context);
            } else {
                value = null;
            }
            context.setVariable(key, value);
        }

        ExecutionPlan plan = override != null ? runtime.getCompiler().compile(task.getFlow()) : runtime.getOrCompileTaskPlan(task);
        EventRuntime.TaskCallFrame frame = new EventRuntime.TaskCallFrame(
            localBackup,
            localMissing,
            globalBackup,
            globalMissing,
            into,
            runtime.getLoopCounterDepth()
        );
        runtime.beginTask(frame, plan);

        return ExecutionResult.CONTINUE;
    }
}

