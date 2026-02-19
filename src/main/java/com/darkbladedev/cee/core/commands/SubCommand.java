package com.darkbladedev.cee.core.commands;

import java.util.List;

public interface SubCommand {
    String name();
    String permission();
    boolean playerOnly();
    List<ArgumentDefinition> arguments();
    void execute(CommandContext context);

    default List<String> aliases() {
        return List.of();
    }

    default String description() {
        return "";
    }
}
