package com.darkbladedev.cee.core.conditions;

import com.darkbladedev.cee.api.Condition;
import com.darkbladedev.cee.api.EventContext;
import org.bukkit.Difficulty;

import java.util.Objects;

public final class WorldDifficultyIsCondition implements Condition {
    private final Difficulty difficulty;

    public WorldDifficultyIsCondition(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }

    @Override
    public boolean evaluate(EventContext context) {
        return context.getWorld().getDifficulty() == difficulty;
    }
}
