package com.darkbladedev.cee.core.definition;

public final class ExpansionDefinition {
    private final boolean enabled;
    private final int maxRadius;
    private final int step;
    private final long intervalTicks;

    public ExpansionDefinition(boolean enabled, int maxRadius, int step, long intervalTicks) {
        this.enabled = enabled;
        this.maxRadius = maxRadius;
        this.step = step;
        this.intervalTicks = intervalTicks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public int getStep() {
        return step;
    }

    public long getIntervalTicks() {
        return intervalTicks;
    }
}
