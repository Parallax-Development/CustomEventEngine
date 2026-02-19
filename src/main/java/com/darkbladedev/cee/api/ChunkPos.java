package com.darkbladedev.cee.api;

import java.util.Objects;
import java.util.UUID;

public final class ChunkPos {
    private final UUID worldId;
    private final int x;
    private final int z;

    public ChunkPos(UUID worldId, int x, int z) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.x = x;
        this.z = z;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public ChunkPos offset(int dx, int dz) {
        return new ChunkPos(worldId, x + dx, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkPos other)) return false;
        return x == other.x && z == other.z && worldId.equals(other.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, z);
    }

    @Override
    public String toString() {
        return "ChunkPos{" + "worldId=" + worldId + ", x=" + x + ", z=" + z + '}';
    }
}
