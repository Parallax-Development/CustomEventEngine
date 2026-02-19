package com.darkbladedev.cee.api;

import java.util.Set;

public interface Scope {
    Set<ChunkPos> resolveChunks(ChunkPos origin);
}
