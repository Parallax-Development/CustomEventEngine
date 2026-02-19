package com.darkbladedev.cee.api;

import java.util.Optional;

public interface CustomEventEngine {
    void registerAction(String id, ActionFactory factory);
    void registerCondition(String id, ConditionFactory factory);
    void registerTrigger(String id, TriggerFactory factory);
    void registerScope(String id, ScopeFactory factory);
    void registerChunkStrategy(String id, ChunkSelectionStrategy strategy);
    Optional<EventHandle> getActiveEvent(ChunkPos chunkPos);
    StartResult startEvent(String eventId, ChunkPos chunkPos);
}
