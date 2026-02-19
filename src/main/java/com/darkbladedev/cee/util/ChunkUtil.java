package com.darkbladedev.cee.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import com.darkbladedev.cee.api.ChunkPos;

import java.util.UUID;

public final class ChunkUtil {
    private ChunkUtil() {
    }

    public static ChunkPos fromLocation(Location location) {
        Chunk chunk = location.getChunk();
        return new ChunkPos(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public static ChunkPos fromChunk(Chunk chunk) {
        return new ChunkPos(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public static ChunkPos fromWorld(World world, int x, int z) {
        return new ChunkPos(world.getUID(), x, z);
    }

    public static boolean sameWorld(UUID worldId, World world) {
        return world.getUID().equals(worldId);
    }
}
