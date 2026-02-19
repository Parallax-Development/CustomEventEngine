package com.darkbladedev.cee.core.trigger;

import org.bukkit.World;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;

import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class IntervalTrigger implements Trigger {
    private final Plugin plugin;
    private final String eventId;
    private final long intervalTicks;
    private final TriggerCallback callback;
    private final com.darkbladedev.cee.core.runtime.RuntimeScheduler scheduler;

    public IntervalTrigger(Plugin plugin, String eventId, long intervalTicks, TriggerCallback callback, com.darkbladedev.cee.core.runtime.RuntimeScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.intervalTicks = intervalTicks;
        this.callback = Objects.requireNonNull(callback, "callback");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void register(TriggerContext context) {
        scheduler.registerInterval(eventId, intervalTicks, () -> {
            for (World world : plugin.getServer().getWorlds()) {
                callback.onTrigger(new TriggerContext(eventId, world, null));
            }
        });
    }

    @Override
    public void unregister() {
        scheduler.unregisterInterval(eventId);
    }
}
