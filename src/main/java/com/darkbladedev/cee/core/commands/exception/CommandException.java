package com.darkbladedev.cee.core.commands.exception;

public class CommandException extends RuntimeException {
    private final String messageKey;

    public CommandException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
