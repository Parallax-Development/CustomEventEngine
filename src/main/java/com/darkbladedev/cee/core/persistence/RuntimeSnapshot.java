package com.darkbladedev.cee.core.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RuntimeSnapshot {
    private String eventId;
    private UUID worldId;
    private int originX;
    private int originZ;
    private List<ChunkSnapshot> chunks;
    private int instructionPointer;
    private long waitRemaining;
    private Map<String, Object> variables;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public int getOriginX() {
        return originX;
    }

    public void setOriginX(int originX) {
        this.originX = originX;
    }

    public int getOriginZ() {
        return originZ;
    }

    public void setOriginZ(int originZ) {
        this.originZ = originZ;
    }

    public List<ChunkSnapshot> getChunks() {
        return chunks;
    }

    public void setChunks(List<ChunkSnapshot> chunks) {
        this.chunks = chunks;
    }

    public int getInstructionPointer() {
        return instructionPointer;
    }

    public void setInstructionPointer(int instructionPointer) {
        this.instructionPointer = instructionPointer;
    }

    public long getWaitRemaining() {
        return waitRemaining;
    }

    public void setWaitRemaining(long waitRemaining) {
        this.waitRemaining = waitRemaining;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
