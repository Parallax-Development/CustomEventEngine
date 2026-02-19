package com.darkbladedev.cee.core.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.darkbladedev.cee.api.ChunkPos;

public final class ChunkEventLockManager {
    private final Map<ChunkPos, EventRuntime> occupied = new HashMap<>();

    public synchronized boolean tryLock(Set<ChunkPos> chunks, EventRuntime runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (chunks.isEmpty()) {
            return false;
        }
        for (ChunkPos pos : chunks) {
            if (occupied.containsKey(pos)) {
                return false;
            }
        }
        for (ChunkPos pos : chunks) {
            occupied.put(pos, runtime);
        }
        return true;
    }

    public synchronized Set<ChunkPos> release(EventRuntime runtime) {
        Set<ChunkPos> released = new HashSet<>();
        occupied.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(runtime)) {
                released.add(entry.getKey());
                return true;
            }
            return false;
        });
        return released;
    }

    public synchronized EventRuntime getRuntime(ChunkPos pos) {
        return occupied.get(pos);
    }

    public synchronized boolean isOccupied(ChunkPos pos) {
        return occupied.containsKey(pos);
    }

    public synchronized Set<ChunkPos> getOccupiedChunks(EventRuntime runtime) {
        Set<ChunkPos> result = new HashSet<>();
        for (Map.Entry<ChunkPos, EventRuntime> entry : occupied.entrySet()) {
            if (entry.getValue().equals(runtime)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public synchronized Map<ChunkPos, EventRuntime> snapshotOccupied() {
        return new HashMap<>(occupied);
    }
}
