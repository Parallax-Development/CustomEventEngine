package com.darkbladedev.cee.api;

import org.bukkit.World;

import java.util.Optional;

public interface ChunkSelectionStrategy {
    Optional<ChunkPos> select(World world, TriggerContext context);
}
