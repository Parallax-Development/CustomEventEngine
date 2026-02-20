package com.darkbladedev.cee.core.loader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import org.mvel2.MVEL;

import com.darkbladedev.cee.core.definition.ActionDefinition;
import com.darkbladedev.cee.core.definition.ActionNodeDefinition;
import com.darkbladedev.cee.core.definition.AsyncNodeDefinition;
import com.darkbladedev.cee.core.definition.ChunkLoadRules;
import com.darkbladedev.cee.core.definition.ChunkTargetDefinition;
import com.darkbladedev.cee.core.definition.ChunkUnloadRules;
import com.darkbladedev.cee.core.definition.ConditionDefinition;
import com.darkbladedev.cee.core.definition.DelayNodeDefinition;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.definition.ExpansionDefinition;
import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.FlowNodeDefinition;
import com.darkbladedev.cee.core.definition.ConditionLoopNodeDefinition;
import com.darkbladedev.cee.core.definition.RepeatNodeDefinition;
import com.darkbladedev.cee.core.definition.ScopeDefinition;
import com.darkbladedev.cee.core.definition.TriggerDefinition;
import com.darkbladedev.cee.core.definition.VariableDefinition;
import com.darkbladedev.cee.core.definition.VariableScope;
import com.darkbladedev.cee.core.definition.VariableType;
import com.darkbladedev.cee.util.ConfigUtil;
import com.darkbladedev.cee.util.DurationParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventLoader {
    public List<EventDefinition> loadFromFolder(File folder) {
        List<EventDefinition> definitions = new ArrayList<>();
        if (!folder.exists()) {
            return definitions;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return definitions;
        }
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection eventSection = config.getConfigurationSection("event");
            if (eventSection == null) {
                continue;
            }
            String id = eventSection.getString("id", file.getName().replace(".yml", ""));
            TriggerDefinition trigger = readTrigger(eventSection.getConfigurationSection("trigger"));
            List<ConditionDefinition> conditions = readConditions(eventSection.getConfigurationSection("conditions"));
            FlowDefinition flow = readFlow(eventSection.getConfigurationSection("flow"));
            ScopeDefinition scope = readScope(eventSection.getConfigurationSection("scope"));
            ExpansionDefinition expansion = readExpansion(eventSection.getConfigurationSection("expansion"));
            ChunkTargetDefinition target = readTarget(eventSection.getConfigurationSection("target"));
            ChunkLoadRules chunkLoadRules = ChunkLoadRules.valueOf(readStringOrFallback(eventSection, "chunk_load_rules", List.of("chunk_load_policy", "chunk_policy"), "REJECT").toUpperCase());
            ChunkUnloadRules chunkUnloadRules = ChunkUnloadRules.valueOf(readStringOrFallback(eventSection, "chunk_unload_rules", List.of("chunk_unload_policy"), "PAUSE").toUpperCase());
            Map<String, VariableDefinition> variables = readVariables(eventSection.getConfigurationSection("variables"));
            definitions.add(new EventDefinition(id, trigger, conditions, flow, scope, expansion, target, chunkLoadRules, chunkUnloadRules, variables));
        }
        return definitions;
    }

    private TriggerDefinition readTrigger(ConfigurationSection section) {
        if (section == null) {
            return new TriggerDefinition("interval", Map.of("every", "60s"));
        }
        String type = section.getString("type", "interval");
        return new TriggerDefinition(type, ConfigUtil.toMap(section));
    }

    private List<ConditionDefinition> readConditions(ConfigurationSection section) {
        List<ConditionDefinition> list = new ArrayList<>();
        if (section == null) {
            return list;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection cond = section.getConfigurationSection(key);
            if (cond != null) {
                list.add(new ConditionDefinition(cond.getString("type", key), ConfigUtil.toMap(cond)));
            }
        }
        return list;
    }

    private FlowDefinition readFlow(ConfigurationSection section) {
        if (section == null) {
            return new FlowDefinition(List.of());
        }
        return new FlowDefinition(readNodes(section.getMapList("nodes")));
    }

    private List<FlowNodeDefinition> readNodes(List<Map<?, ?>> rawNodes) {
        List<FlowNodeDefinition> nodes = new ArrayList<>();
        for (Map<?, ?> nodeMap : rawNodes) {
            nodes.addAll(parseNodeMap(nodeMap));
        }
        return nodes;
    }

    private List<FlowNodeDefinition> parseNodeMap(Map<?, ?> nodeMap) {
        List<FlowNodeDefinition> nodes = new ArrayList<>();

        if (nodeMap.containsKey("action")) {
            String type = String.valueOf(nodeMap.get("action"));
            nodes.add(new ActionNodeDefinition(new ActionDefinition(type, readActionConfig(nodeMap))));
        }

        if (nodeMap.containsKey("delay")) {
            nodes.add(new DelayNodeDefinition(DurationParser.parseTicks(nodeMap.get("delay"))));
        }

        if (nodeMap.containsKey("repeat")) {
            Object rawRepeat = nodeMap.get("repeat");
            Map<?, ?> repeatMap = rawRepeat instanceof Map<?, ?> map ? map : Map.of("times", rawRepeat);
            Object timesRaw = repeatMap.get("times");
            int times = parseInt(timesRaw != null ? timesRaw : 1, 1);
            Object everyRaw = repeatMap.get("every");
            long everyTicks = DurationParser.parseTicks(everyRaw != null ? everyRaw : 0);

            Object rawFlow = repeatMap.get("flow");
            Object rawNodes = rawFlow instanceof Map<?, ?> flowMap ? flowMap.get("nodes") : repeatMap.get("nodes");
            FlowDefinition flow = new FlowDefinition(readNodesFromObject(rawNodes));

            nodes.add(new RepeatNodeDefinition(times, everyTicks, flow));
        }

        if (nodeMap.containsKey("loop")) {
            Object rawLoop = nodeMap.get("loop");
            Map<?, ?> loopMap = rawLoop instanceof Map<?, ?> map ? map : Map.of("times", rawLoop);
            Object timesRaw = loopMap.get("times");
            int times = parseInt(timesRaw != null ? timesRaw : 1, 1);

            Object rawFlow = loopMap.get("flow");
            Object rawNodes = rawFlow instanceof Map<?, ?> flowMap ? flowMap.get("nodes") : loopMap.get("nodes");
            if (rawNodes == null) {
                Object outerFlow = nodeMap.get("flow");
                rawNodes = outerFlow instanceof Map<?, ?> flowMap ? flowMap.get("nodes") : nodeMap.get("nodes");
            }
            FlowDefinition flow = new FlowDefinition(readNodesFromObject(rawNodes));

            nodes.add(new RepeatNodeDefinition(times, 0L, flow));
        }

        if (nodeMap.containsKey("condition_loop")) {
            Object rawLoop = nodeMap.get("condition_loop");
            Map<?, ?> loopMap = rawLoop instanceof Map<?, ?> map ? map : Map.of();
            Object timesRaw = loopMap.get("times");
            int times = parseInt(timesRaw != null ? timesRaw : 1, 1);
            Object exprRaw = loopMap.get("expression");
            String expression = String.valueOf(exprRaw != null ? exprRaw : "true").trim();

            Object rawFlow = loopMap.get("flow");
            Object rawNodes = rawFlow instanceof Map<?, ?> flowMap ? flowMap.get("nodes") : loopMap.get("nodes");
            FlowDefinition flow = new FlowDefinition(readNodesFromObject(rawNodes));

            nodes.add(new ConditionLoopNodeDefinition(times, expression, flow));
        }

        if (nodeMap.containsKey("async")) {
            Object rawAsync = nodeMap.get("async");
            Object rawBranches = rawAsync;
            if (rawAsync instanceof Map<?, ?> asyncMap) {
                rawBranches = asyncMap.containsKey("branches") ? asyncMap.get("branches") : asyncMap.get("flows");
            }
            List<FlowDefinition> branches = new ArrayList<>();
            if (rawBranches instanceof List<?> list) {
                for (Object entry : list) {
                    branches.add(new FlowDefinition(readBranchNodes(entry)));
                }
            }
            nodes.add(new AsyncNodeDefinition(branches));
        }

        return nodes;
    }

    private Map<String, Object> readActionConfig(Map<?, ?> nodeMap) {
        Map<String, Object> config = new HashMap<>();
        Object rawConfig = nodeMap.get("config");
        if (rawConfig instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                config.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return config;
        }
        for (Map.Entry<?, ?> entry : nodeMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.equals("action") || key.equals("delay") || key.equals("config") || key.equals("repeat") || key.equals("loop") || key.equals("condition_loop") || key.equals("async")) {
                continue;
            }
            config.put(key, entry.getValue());
        }
        return config;
    }

    private List<FlowNodeDefinition> readNodesFromObject(Object rawNodes) {
        if (rawNodes instanceof List<?> list) {
            List<Map<?, ?>> maps = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    maps.add(map);
                }
            }
            return readNodes(maps);
        }
        return List.of();
    }

    private List<FlowNodeDefinition> readBranchNodes(Object branchEntry) {
        if (branchEntry instanceof Map<?, ?> map) {
            Object nodes = map.get("nodes");
            return readNodesFromObject(nodes);
        }
        if (branchEntry instanceof List<?> list) {
            return readNodesFromObject(list);
        }
        return List.of();
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String readStringOrFallback(ConfigurationSection section, String key, List<String> fallbackKeys, String defaultValue) {
        String value = section.getString(key);
        if (value != null) {
            return value;
        }
        for (String fallback : fallbackKeys) {
            value = section.getString(fallback);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    private Map<String, VariableDefinition> readVariables(ConfigurationSection section) {
        Map<String, VariableDefinition> result = new java.util.LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection variableSection = section.getConfigurationSection(key);
            if (variableSection == null) {
                Object initial = section.get(key);
                VariableType type = inferType(initial);
                result.put(key, new VariableDefinition(key, type, VariableScope.LOCAL, initial, ""));
                continue;
            }

            String rawType = String.valueOf(variableSection.getString("type", "string")).trim();
            VariableType type = VariableType.valueOf(rawType.toUpperCase());

            String rawScope = String.valueOf(variableSection.getString("scope", "local")).trim();
            VariableScope scope = VariableScope.valueOf(rawScope.toUpperCase());

            Object initial = variableSection.contains("initial") ? variableSection.get("initial") : variableSection.get("value");
            String description = String.valueOf(variableSection.getString("description", "")).trim();

            if (!isValidVariableName(key)) {
                throw new IllegalArgumentException("Nombre de variable inválido: " + key);
            }

            validateInitialValue(key, type, initial);
            result.put(key, new VariableDefinition(key, type, scope, initial, description));
        }
        return result;
    }

    private boolean isValidVariableName(String name) {
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

    private VariableType inferType(Object value) {
        if (value instanceof Number) {
            return VariableType.NUMBER;
        }
        if (value instanceof Boolean) {
            return VariableType.BOOLEAN;
        }
        if (value instanceof List) {
            return VariableType.ARRAY;
        }
        if (value instanceof Map) {
            return VariableType.OBJECT;
        }
        return VariableType.STRING;
    }

    private void validateInitialValue(String name, VariableType type, Object initial) {
        if (initial == null) {
            return;
        }
        if (initial instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.startsWith("=")) {
                String expr = trimmed.substring(1).trim();
                try {
                    MVEL.compileExpression(expr);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Expresión inválida en variable '" + name + "': " + ex.getMessage());
                }
                return;
            }
            if (trimmed.startsWith("${") && trimmed.endsWith("}") && trimmed.length() > 3) {
                return;
            }
        }

        switch (type) {
            case STRING -> {
                if (!(initial instanceof String)) {
                    throw new IllegalArgumentException("Variable '" + name + "' debe ser string.");
                }
            }
            case NUMBER -> {
                if (!(initial instanceof Number)) {
                    throw new IllegalArgumentException("Variable '" + name + "' debe ser number.");
                }
            }
            case BOOLEAN -> {
                if (!(initial instanceof Boolean)) {
                    throw new IllegalArgumentException("Variable '" + name + "' debe ser boolean.");
                }
            }
            case ARRAY -> {
                if (!(initial instanceof List<?>)) {
                    throw new IllegalArgumentException("Variable '" + name + "' debe ser array.");
                }
            }
            case OBJECT -> {
                if (!(initial instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("Variable '" + name + "' debe ser object.");
                }
            }
        }
    }

    private ScopeDefinition readScope(ConfigurationSection section) {
        if (section == null) {
            return new ScopeDefinition("chunk_radius", Map.of("radius", 0));
        }
        return new ScopeDefinition(section.getString("type", "chunk_radius"), ConfigUtil.toMap(section));
    }

    private ExpansionDefinition readExpansion(ConfigurationSection section) {
        if (section == null) {
            return new ExpansionDefinition(false, 0, 0, 0);
        }
        boolean enabled = section.getBoolean("enabled", false);
        int maxRadius = section.getInt("max_radius", 0);
        int step = section.getInt("step", 1);
        long interval = DurationParser.parseTicks(section.get("interval", 20));
        return new ExpansionDefinition(enabled, maxRadius, step, interval);
    }

    private ChunkTargetDefinition readTarget(ConfigurationSection section) {
        if (section == null) {
            return new ChunkTargetDefinition("random_loaded_chunk", Map.of());
        }
        return new ChunkTargetDefinition(section.getString("strategy", "random_loaded_chunk"), ConfigUtil.toMap(section));
    }
}
