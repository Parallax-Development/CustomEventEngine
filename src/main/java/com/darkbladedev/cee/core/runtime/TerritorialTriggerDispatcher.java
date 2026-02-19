package com.darkbladedev.cee.core.runtime;

import java.util.Optional;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.TriggerContext;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.registry.EngineRegistry;
import com.darkbladedev.cee.util.ChunkUtil;

public final class TerritorialTriggerDispatcher {
    private final EngineRegistry registry;
    private final EventEngine engine;

    public TerritorialTriggerDispatcher(EngineRegistry registry, EventEngine engine) {
        this.registry = registry;
        this.engine = engine;
    }

    public void dispatch(EventDefinition definition, TriggerContext context) {
        Optional<ChunkPos> target = resolveTarget(definition, context);
        if (target.isEmpty()) {
            return;
        }
        engine.startEvent(definition.getId(), target.get());
    }

    private Optional<ChunkPos> resolveTarget(EventDefinition definition, TriggerContext context) {
        if (context.getLocation().isPresent()) {
            return Optional.of(ChunkUtil.fromLocation(context.getLocation().get()));
        }
        if (context.getWorld().isEmpty()) {
            return Optional.empty();
        }
        return registry.getChunkStrategy(definition.getTarget().getStrategy())
            .flatMap(strategy -> strategy.select(context.getWorld().get(), context));
    }
}
