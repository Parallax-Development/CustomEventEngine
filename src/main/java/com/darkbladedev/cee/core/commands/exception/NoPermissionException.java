package com.darkbladedev.cee.core.commands.exception;

public final class NoPermissionException extends CommandException {
    private final String permission;

    public NoPermissionException(String permission) {
        super("errors.no-permission", "No permission: " + permission);
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}
