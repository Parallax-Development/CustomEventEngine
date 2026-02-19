package com.darkbladedev.cee.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigUtil {
    private ConfigUtil() {
    }

    public static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        if (section == null) {
            return map;
        }
        for (String key : section.getKeys(false)) {
            map.put(key, Objects.requireNonNullElse(section.get(key), ""));
        }
        return map;
    }
}
