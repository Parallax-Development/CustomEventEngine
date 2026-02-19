package com.darkbladedev.cee.core.commands;

import java.util.List;
import java.util.Objects;

public final class BaseCommand {
    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String defaultSubcommand;

    public BaseCommand(String name, List<String> aliases, String permission, String defaultSubcommand) {
        this.name = Objects.requireNonNull(name, "name");
        this.aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        this.permission = permission == null ? "" : permission;
        this.defaultSubcommand = Objects.requireNonNull(defaultSubcommand, "defaultSubcommand");
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    public String permission() {
        return permission;
    }

    public String defaultSubcommand() {
        return defaultSubcommand;
    }
}
