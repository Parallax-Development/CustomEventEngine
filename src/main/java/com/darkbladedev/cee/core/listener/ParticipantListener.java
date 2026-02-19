package com.darkbladedev.cee.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.core.runtime.EventEngine;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ChunkUtil;

import java.util.Optional;
import java.util.UUID;

public final class ParticipantListener implements Listener {
    private final EventEngine engine;

    public ParticipantListener(EventEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateParticipant(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeParticipant(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updateParticipant(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        updateParticipant(event.getPlayer());
    }

    private void updateParticipant(Player player) {
        ChunkPos pos = ChunkUtil.fromLocation(player.getLocation());
        Optional<EventRuntime> runtime = engine.getRuntime(pos);
        if (runtime.isPresent()) {
            runtime.get().addParticipant(player.getUniqueId());
            return;
        }
        removeParticipant(player);
    }

    private void removeParticipant(Player player) {
        UUID uuid = player.getUniqueId();
        for (EventRuntime runtime : engine.getScheduler().getRuntimes().values()) {
            runtime.removeParticipant(uuid);
        }
    }
}
