package com.darkbladedev.cee.core.commands.exception;

public final class InvalidArgumentException extends CommandException {
    private final String argumentName;

    public InvalidArgumentException(String argumentName, String message) {
        super("errors.invalid-argument", message);
        this.argumentName = argumentName;
    }

    public String argumentName() {
        return argumentName;
    }
}
