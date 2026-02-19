package com.darkbladedev.cee.core.commands;

import java.util.List;

@FunctionalInterface
public interface SuggestionProvider {
    List<String> suggest(CommandContext context, String input);
}
