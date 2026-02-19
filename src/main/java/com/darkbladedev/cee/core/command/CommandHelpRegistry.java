package com.darkbladedev.cee.core.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommandHelpRegistry {
    private final Map<String, CommandInfo> commands = new HashMap<>();
    private final Map<String, String> aliasToName = new HashMap<>();

    public void register(CommandInfo info) {
        String key = normalize(info.name());
        commands.put(key, info);
        for (String alias : info.aliases()) {
            aliasToName.put(normalize(alias), key);
        }
    }

    public Optional<CommandInfo> resolve(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(name);
        if (commands.containsKey(key)) {
            return Optional.of(commands.get(key));
        }
        String alias = aliasToName.get(key);
        if (alias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(alias));
    }

    public List<CommandInfo> list() {
        List<CommandInfo> result = new ArrayList<>(commands.values());
        result.sort(Comparator.comparing(CommandInfo::name));
        return result;
    }

    public String[] commandNames() {
        Collection<CommandInfo> values = commands.values();
        return values.stream().map(CommandInfo::name).toArray(String[]::new);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
