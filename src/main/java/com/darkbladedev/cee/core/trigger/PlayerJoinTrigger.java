package com.darkbladedev.cee.core.trigger;

import com.darkbladedev.cee.api.Trigger;
import com.darkbladedev.cee.api.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PlayerJoinTrigger implements Trigger, Listener {
    private final Plugin plugin;
    private final String eventId;
    private final TriggerCallback callback;
    private boolean registered;

    public PlayerJoinTrigger(Plugin plugin, String eventId, TriggerCallback callback) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
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
    public void onJoin(PlayerJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        callback.onTrigger(new TriggerContext(eventId, location.getWorld(), location));
    }
}
