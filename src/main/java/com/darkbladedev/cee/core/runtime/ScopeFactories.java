package com.darkbladedev.cee.core.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.Scope;
import com.darkbladedev.cee.api.ScopeFactory;

public final class ScopeFactories {
    private ScopeFactories() {
    }

    public static ScopeFactory chunkRadius() {
        return (Map<String, Object> config) -> {
            int radius = 0;
            Object value = config.get("radius");
            if (value instanceof Number number) {
                radius = number.intValue();
            }
            int finalRadius = radius;
            return (Scope) (ChunkPos origin) -> {
                Set<ChunkPos> chunks = new HashSet<>();
                for (int dx = -finalRadius; dx <= finalRadius; dx++) {
                    for (int dz = -finalRadius; dz <= finalRadius; dz++) {
                        chunks.add(origin.offset(dx, dz));
                    }
                }
                return chunks;
            };
        };
    }
}
