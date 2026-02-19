package com.darkbladedev.cee.core.command;

import java.util.List;
import java.util.Objects;

public record CommandInfo(
    String name,
    String syntax,
    String description,
    String permission,
    List<String> aliases,
    List<String> examples
) {
    public CommandInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(syntax, "syntax");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(aliases, "aliases");
        Objects.requireNonNull(examples, "examples");
    }
}
