package com.darkbladedev.cee.core.commands.exception;

public final class UnknownSubCommandException extends CommandException {
    private final String input;

    public UnknownSubCommandException(String input) {
        super("errors.unknown-command", "Unknown subcommand: " + input);
        this.input = input;
    }

    public String input() {
        return input;
    }
}
