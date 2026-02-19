package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.runtime.EventEngine;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

public final class CommandServices {
    private final Plugin plugin;
    private final EventEngine engine;
    private final MessageService messages;
    private final EventIdCache eventIdCache;

    public CommandServices(Plugin plugin, EventEngine engine, MessageService messages) {
        this.plugin = plugin;
        this.engine = engine;
        this.messages = messages;
        this.eventIdCache = new EventIdCache();
        refreshCaches();
    }

    public Plugin plugin() {
        return plugin;
    }

    public EventEngine engine() {
        return engine;
    }

    public MessageService messages() {
        return messages;
    }

    public Set<String> eventIds() {
        return Collections.unmodifiableSet(eventIdCache.get());
    }

    public void refreshCaches() {
        eventIdCache.refresh(engine.getDefinitions().keySet());
    }
}
