package com.darkbladedev.cee.core.trigger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;

import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class IntervalTrigger implements Trigger {
    private final Plugin plugin;
    private final String eventId;
    private final long intervalTicks;
    private final TriggerCallback callback;
    private BukkitRunnable task;

    public IntervalTrigger(Plugin plugin, String eventId, long intervalTicks, TriggerCallback callback) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.intervalTicks = intervalTicks;
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    @Override
    public void register(TriggerContext context) {
        if (task != null) {
            return;
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    callback.onTrigger(new TriggerContext(eventId, world, null));
                }
            }
        };
        task.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    @Override
    public void unregister() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
