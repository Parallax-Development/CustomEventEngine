package com.darkbladedev.cee.core.loader;

import com.darkbladedev.cee.core.definition.AsyncNodeDefinition;
import com.darkbladedev.cee.core.definition.ConditionLoopNodeDefinition;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.FlowNodeDefinition;
import com.darkbladedev.cee.core.definition.RepeatNodeDefinition;
import com.darkbladedev.cee.core.definition.TaskCallNodeDefinition;
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.definition.TaskParameterDefinition;
import com.darkbladedev.cee.core.definition.TaskReturnDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TaskValidator {
    public List<String> validate(EventDefinition definition, Map<String, TaskDefinition> globalTasks) {
        Map<String, TaskDefinition> merged = new HashMap<>();
        if (globalTasks != null) {
            merged.putAll(globalTasks);
        }
        merged.putAll(definition.getTasks());

        List<String> errors = new ArrayList<>();

        for (TaskDefinition task : definition.getTasks().values()) {
            validateTask(task, merged, "task:" + task.getName(), errors);
        }

        validateFlow(definition.getFlow(), merged, "event:" + definition.getId(), errors);

        return errors;
    }

    public List<String> validate(TaskDefinition task, Map<String, TaskDefinition> available) {
        Map<String, TaskDefinition> merged = new HashMap<>();
        if (available != null) {
            merged.putAll(available);
        }
        List<String> errors = new ArrayList<>();
        validateTask(task, merged, "task:" + task.getName(), errors);
        return errors;
    }

    private void validateTask(TaskDefinition task,
                              Map<String, TaskDefinition> available,
                              String path,
                              List<String> errors) {
        for (TaskParameterDefinition param : task.getParameters().values()) {
            if (!isValidName(param.getName())) {
                errors.add(path + ": parámetro inválido '" + param.getName() + "'.");
            }
        }
        for (TaskReturnDefinition ret : task.getReturns().values()) {
            if (!isValidName(ret.getName())) {
                errors.add(path + ": retorno inválido '" + ret.getName() + "'.");
            }
        }
        validateFlow(task.getFlow(), available, path, errors);
    }

    private void validateFlow(FlowDefinition flow,
                              Map<String, TaskDefinition> available,
                              String path,
                              List<String> errors) {
        for (FlowNodeDefinition node : flow.getNodes()) {
            if (node instanceof TaskCallNodeDefinition call) {
                validateTaskCall(call, available, path, errors);
            } else if (node instanceof RepeatNodeDefinition repeat) {
                validateFlow(repeat.getFlow(), available, path + "/repeat", errors);
            } else if (node instanceof ConditionLoopNodeDefinition loop) {
                validateFlow(loop.getFlow(), available, path + "/condition_loop", errors);
            } else if (node instanceof AsyncNodeDefinition async) {
                int index = 0;
                for (FlowDefinition branch : async.getBranches()) {
                    validateFlow(branch, available, path + "/async[" + index + "]", errors);
                    index++;
                }
            }
        }
    }

    private void validateTaskCall(TaskCallNodeDefinition call,
                                  Map<String, TaskDefinition> available,
                                  String path,
                                  List<String> errors) {
        if (call.getTaskName().isBlank() && call.getOverride() == null) {
            errors.add(path + ": task call sin nombre.");
            return;
        }

        TaskDefinition target = call.getOverride();
        if (target == null) {
            target = available.get(call.getTaskName());
        }

        if (target == null) {
            errors.add(path + ": task inexistente '" + call.getTaskName() + "'.");
            return;
        }

        if (call.getOverride() != null) {
            validateTask(call.getOverride(), available, path + "/override:" + call.getOverride().getName(), errors);
        }

        for (String key : call.getArguments().keySet()) {
            if (!target.getParameters().containsKey(key)) {
                errors.add(path + ": parámetro desconocido '" + key + "' para task '" + target.getName() + "'.");
            }
        }

        for (TaskParameterDefinition param : target.getParameters().values()) {
            boolean provided = call.getArguments().containsKey(param.getName());
            if (!provided && param.isRequired() && param.getDefaultValue() == null) {
                errors.add(path + ": falta parámetro requerido '" + param.getName() + "' en task '" + target.getName() + "'.");
            }
        }

        for (Map.Entry<String, String> entry : call.getInto().entrySet()) {
            String returnName = entry.getKey();
            String varName = entry.getValue();
            if (!target.getReturns().containsKey(returnName)) {
                errors.add(path + ": retorno desconocido '" + returnName + "' en task '" + target.getName() + "'.");
            }
            if (varName == null || varName.isBlank()) {
                errors.add(path + ": variable destino vacía para retorno '" + returnName + "'.");
            }
        }

        if (call.getMaxDepth() < 1) {
            errors.add(path + ": max_depth inválido.");
        }
    }

    private boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (!(Character.isLetter(c) || c == '_')) {
                    return false;
                }
            } else {
                if (!(Character.isLetterOrDigit(c) || c == '_')) {
                    return false;
                }
            }
        }
        return true;
    }
}
