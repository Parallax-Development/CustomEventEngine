package com.darkbladedev.cee.core.loader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.darkbladedev.cee.core.definition.ActionDefinition;
import com.darkbladedev.cee.core.definition.ActionNodeDefinition;
import com.darkbladedev.cee.core.definition.ChunkPolicy;
import com.darkbladedev.cee.core.definition.ChunkTargetDefinition;
import com.darkbladedev.cee.core.definition.ChunkUnloadPolicy;
import com.darkbladedev.cee.core.definition.ConditionDefinition;
import com.darkbladedev.cee.core.definition.DelayNodeDefinition;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.definition.ExpansionDefinition;
import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.FlowNodeDefinition;
import com.darkbladedev.cee.core.definition.ParallelNodeDefinition;
import com.darkbladedev.cee.core.definition.RepeatNodeDefinition;
import com.darkbladedev.cee.core.definition.ScopeDefinition;
import com.darkbladedev.cee.core.definition.TriggerDefinition;
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
            ChunkPolicy chunkPolicy = ChunkPolicy.valueOf(eventSection.getString("chunk_policy", "REJECT").toUpperCase());
            ChunkUnloadPolicy unloadPolicy = ChunkUnloadPolicy.valueOf(eventSection.getString("chunk_unload_policy", "PAUSE").toUpperCase());
            definitions.add(new EventDefinition(id, trigger, conditions, flow, scope, expansion, target, chunkPolicy, unloadPolicy));
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
        List<FlowNodeDefinition> nodes = new ArrayList<>();
        for (Map<?, ?> nodeMap : section.getMapList("nodes")) {
            if (nodeMap.containsKey("action")) {
                String type = String.valueOf(nodeMap.get("action"));
                Map<String, Object> config = new HashMap<>();
                Object rawConfig = nodeMap.get("config");
                if (rawConfig instanceof Map<?, ?> rawMap) {
                    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                        config.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                } else {
                    for (Map.Entry<?, ?> entry : nodeMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        if (key.equals("action") || key.equals("delay") || key.equals("config")) {
                            continue;
                        }
                        config.put(key, entry.getValue());
                    }
                }
                nodes.add(new ActionNodeDefinition(new ActionDefinition(type, config)));
            }
            if (nodeMap.containsKey("delay")) {
                nodes.add(new DelayNodeDefinition(DurationParser.parseTicks(nodeMap.get("delay"))));
            }
        }
        return new FlowDefinition(nodes);
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
