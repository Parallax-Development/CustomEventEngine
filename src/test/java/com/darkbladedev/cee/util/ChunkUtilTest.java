package com.darkbladedev.cee.util;

import com.darkbladedev.cee.api.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChunkUtilTest {
    @Test
    void fromBlockCoordsUsesFloorDivision() {
        UUID worldId = UUID.randomUUID();

        assertEquals(new ChunkPos(worldId, 0, 0), ChunkUtil.fromBlockCoords(worldId, 0, 0));
        assertEquals(new ChunkPos(worldId, 0, 0), ChunkUtil.fromBlockCoords(worldId, 15, 15));
        assertEquals(new ChunkPos(worldId, 1, 1), ChunkUtil.fromBlockCoords(worldId, 16, 16));
        assertEquals(new ChunkPos(worldId, -1, -1), ChunkUtil.fromBlockCoords(worldId, -1, -1));
        assertEquals(new ChunkPos(worldId, -1, -1), ChunkUtil.fromBlockCoords(worldId, -16, -16));
        assertEquals(new ChunkPos(worldId, -2, -2), ChunkUtil.fromBlockCoords(worldId, -17, -17));
    }
}
