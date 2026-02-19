package com.darkbladedev.cee.core.runtime;

import org.bukkit.World;

import com.darkbladedev.cee.api.ChunkSelectionStrategy;
import com.darkbladedev.cee.api.TriggerContext;
import com.darkbladedev.cee.util.ChunkUtil;

import java.util.Optional;
import java.util.Random;

public final class ChunkSelectionStrategies {
    private ChunkSelectionStrategies() {
    }

    public static ChunkSelectionStrategy randomLoadedChunk() {
        return (World world, TriggerContext context) -> {
            if (world.getLoadedChunks().length == 0) {
                return Optional.empty();
            }
            int index = new Random().nextInt(world.getLoadedChunks().length);
            return Optional.of(ChunkUtil.fromChunk(world.getLoadedChunks()[index]));
        };
    }
}
