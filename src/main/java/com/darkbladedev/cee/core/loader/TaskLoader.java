package com.darkbladedev.cee.core.loader;

import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.definition.TaskParameterDefinition;
import com.darkbladedev.cee.core.definition.TaskReturnDefinition;
import com.darkbladedev.cee.core.definition.VariableType;
import com.darkbladedev.cee.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TaskLoader {
    private final EventLoader eventLoader;

    public TaskLoader(EventLoader eventLoader) {
        this.eventLoader = eventLoader;
    }

    public Map<String, TaskDefinition> loadFromFolder(File folder) {
        Map<String, TaskDefinition> tasks = new LinkedHashMap<>();
        if (folder == null || !folder.exists()) {
            return tasks;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return tasks;
        }
        List<File> ordered = new ArrayList<>(List.of(files));
        ordered.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File file : ordered) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = config.getConfigurationSection("tasks");
            if (section == null) {
                continue;
            }
            for (String key : section.getKeys(false)) {
                ConfigurationSection taskSection = section.getConfigurationSection(key);
                if (taskSection == null) {
                    continue;
                }
                TaskDefinition def = readTaskDefinition(key, taskSection, "file:" + file.getName());
                tasks.put(def.getName(), def);
            }
        }
        return tasks;
    }

    private TaskDefinition readTaskDefinition(String name, ConfigurationSection section, String source) {
        String description = String.valueOf(section.getString("description", "")).trim();
        Map<String, TaskParameterDefinition> params = readTaskParameters(section.getConfigurationSection("params"));
        Map<String, TaskReturnDefinition> returns = readTaskReturns(section.getConfigurationSection("returns"));

        ConfigurationSection flowSection = section.getConfigurationSection("flow");
        List<Map<?, ?>> nodeMaps = flowSection != null ? flowSection.getMapList("nodes") : section.getMapList("nodes");
        FlowDefinition flow = new FlowDefinition(eventLoader.readNodesFromObjectPublic(nodeMaps));

        return new TaskDefinition(name, description, params, returns, flow, source);
    }

    private Map<String, TaskParameterDefinition> readTaskParameters(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Object> map = ConfigUtil.toMap(section);
        return readTaskParameters(map);
    }

    private Map<String, TaskParameterDefinition> readTaskParameters(Map<?, ?> map) {
        Map<String, TaskParameterDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = String.valueOf(entry.getKey()).trim();
            Object raw = entry.getValue();
            VariableType type = VariableType.STRING;
            boolean required = true;
            Object defaultValue = null;

            if (raw instanceof String rawType) {
                type = VariableType.valueOf(rawType.trim().toUpperCase());
            } else if (raw instanceof Map<?, ?> defMap) {
                Object typeRaw = defMap.get("type");
                if (typeRaw == null) {
                    typeRaw = "string";
                }
                type = VariableType.valueOf(String.valueOf(typeRaw).trim().toUpperCase());
                Object requiredRaw = defMap.get("required");
                if (requiredRaw == null) {
                    requiredRaw = true;
                }
                required = requiredRaw instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(requiredRaw));
                defaultValue = defMap.containsKey("default") ? defMap.get("default") : defMap.get("initial");
            }

            result.put(name, new TaskParameterDefinition(name, type, required, defaultValue));
        }
        return result;
    }

    private Map<String, TaskReturnDefinition> readTaskReturns(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Object> map = ConfigUtil.toMap(section);
        return readTaskReturns(map);
    }

    private Map<String, TaskReturnDefinition> readTaskReturns(Map<?, ?> map) {
        Map<String, TaskReturnDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = String.valueOf(entry.getKey()).trim();
            Object raw = entry.getValue();
            VariableType type;
            if (raw instanceof String rawType) {
                type = VariableType.valueOf(rawType.trim().toUpperCase());
            } else if (raw instanceof Map<?, ?> defMap) {
                Object typeRaw = defMap.get("type");
                if (typeRaw == null) {
                    typeRaw = "string";
                }
                type = VariableType.valueOf(String.valueOf(typeRaw).trim().toUpperCase());
            } else {
                type = VariableType.STRING;
            }
            result.put(name, new TaskReturnDefinition(name, type));
        }
        return result;
    }
}
