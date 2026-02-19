package com.darkbladedev.cee.core.command;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandHelpRegistryTest {
    @Test
    void resolvesAliasesAndNames() {
        CommandHelpRegistry registry = new CommandHelpRegistry();
        for (CommandInfo info : CommandDocumentation.defaultCommands()) {
            registry.register(info);
        }
        Optional<CommandInfo> help = registry.resolve("help");
        Optional<CommandInfo> alias = registry.resolve("rl");
        Optional<CommandInfo> question = registry.resolve("?");
        assertTrue(help.isPresent());
        assertTrue(alias.isPresent());
        assertTrue(question.isPresent());
        assertEquals("reload", alias.get().name());
        assertEquals("help", question.get().name());
    }
}
