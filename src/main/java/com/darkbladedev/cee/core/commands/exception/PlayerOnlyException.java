package com.darkbladedev.cee.core.commands.exception;

public final class PlayerOnlyException extends CommandException {
    public PlayerOnlyException() {
        super("errors.player-only", "Player only");
    }
}
