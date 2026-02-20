package com.darkbladedev.cee.core.trigger;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class MobSpawnTrigger implements Trigger, Listener {
    private final Plugin plugin;
    private final String eventId;
    private final TriggerCallback callback;
    private final boolean monstersOnly;
    private boolean registered;

    public MobSpawnTrigger(Plugin plugin, String eventId, boolean monstersOnly, TriggerCallback callback) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.monstersOnly = monstersOnly;
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    @Override
    public void register(TriggerContext context) {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (monstersOnly && !(event.getEntity() instanceof Monster)) {
            return;
        }
        Location location = event.getLocation();
        callback.onTrigger(new TriggerContext(eventId, location.getWorld(), location));
    }
}
