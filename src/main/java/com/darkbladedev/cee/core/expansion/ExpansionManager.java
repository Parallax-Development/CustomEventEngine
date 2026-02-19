package com.darkbladedev.cee.core.expansion;

import java.util.HashSet;
import java.util.Set;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.core.definition.ExpansionDefinition;
import com.darkbladedev.cee.core.runtime.ChunkEventLockManager;
import com.darkbladedev.cee.core.runtime.EventRuntime;

public final class ExpansionManager {
    private final ChunkEventLockManager lockManager;

    public ExpansionManager(ChunkEventLockManager lockManager) {
        this.lockManager = lockManager;
    }

    public boolean tryExpand(EventRuntime runtime, ChunkPos origin, ExpansionDefinition expansion) {
        if (!expansion.isEnabled()) {
            return false;
        }
        Set<ChunkPos> current = lockManager.getOccupiedChunks(runtime);
        int currentRadius = (int) Math.sqrt(current.size());
        int nextRadius = Math.min(expansion.getMaxRadius(), currentRadius + expansion.getStep());
        if (nextRadius <= currentRadius) {
            return false;
        }
        Set<ChunkPos> candidates = new HashSet<>();
        for (int dx = -nextRadius; dx <= nextRadius; dx++) {
            for (int dz = -nextRadius; dz <= nextRadius; dz++) {
                ChunkPos pos = origin.offset(dx, dz);
                if (!current.contains(pos)) {
                    candidates.add(pos);
                }
            }
        }
        return lockManager.tryLock(candidates, runtime);
    }
}
