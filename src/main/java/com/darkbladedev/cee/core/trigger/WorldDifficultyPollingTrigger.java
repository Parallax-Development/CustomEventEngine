package com.darkbladedev.cee.core.trigger;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;
import com.darkbladedev.cee.core.runtime.RuntimeScheduler;
import com.darkbladedev.cee.util.DurationParser;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class WorldDifficultyPollingTrigger implements Trigger {
    private final Plugin plugin;
    private final String eventId;
    private final long intervalTicks;
    private final TriggerCallback callback;
    private final RuntimeScheduler scheduler;
    private final Map<String, org.bukkit.Difficulty> lastByWorld;
    private boolean registered;

    public WorldDifficultyPollingTrigger(Plugin plugin, String eventId, Map<String, Object> config, TriggerCallback callback, RuntimeScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.callback = Objects.requireNonNull(callback, "callback");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.intervalTicks = Math.max(1L, DurationParser.parseTicks(config.getOrDefault("every", "20t")));
        this.lastByWorld = new HashMap<>();
    }

    @Override
    public void register(TriggerContext context) {
        if (registered) {
            return;
        }
        lastByWorld.clear();
        for (World world : plugin.getServer().getWorlds()) {
            lastByWorld.put(world.getName(), world.getDifficulty());
        }
        scheduler.registerInterval(eventId + "#world_difficulty_poll", intervalTicks, () -> {
            for (World world : plugin.getServer().getWorlds()) {
                String key = world.getName();
                org.bukkit.Difficulty current = world.getDifficulty();
                org.bukkit.Difficulty last = lastByWorld.put(key, current);
                if (last != null && last != current) {
                    callback.onTrigger(new TriggerContext(eventId, world, null));
                }
            }
        });
        registered = true;
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        scheduler.unregisterInterval(eventId + "#world_difficulty_poll");
        registered = false;
    }
}
