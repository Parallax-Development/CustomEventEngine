package com.darkbladedev.cee.core.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class IntervalScheduleRegistry {
    public record IntervalStatus(String eventId,
                                 long intervalTicks,
                                 long lastTriggerTick,
                                 long nextTriggerTick,
                                 long remainingTicks) {
    }

    private static final class Entry {
        private final String eventId;
        private final long intervalTicks;
        private final Runnable task;
        private volatile long lastTriggerTick;
        private volatile long nextTriggerTick;

        private Entry(String eventId, long intervalTicks, long currentTick, Runnable task) {
            this.eventId = eventId;
            this.intervalTicks = Math.max(1L, intervalTicks);
            this.task = task;
            this.lastTriggerTick = -1L;
            this.nextTriggerTick = currentTick + this.intervalTicks;
        }

        private void onTick(long currentTick) {
            while (currentTick >= nextTriggerTick) {
                lastTriggerTick = nextTriggerTick;
                nextTriggerTick += intervalTicks;
                task.run();
            }
        }

        private IntervalStatus status(long currentTick) {
            long remaining = Math.max(0L, nextTriggerTick - currentTick);
            return new IntervalStatus(eventId, intervalTicks, lastTriggerTick, nextTriggerTick, remaining);
        }
    }

    private final Map<String, Entry> entries;

    public IntervalScheduleRegistry() {
        this.entries = new ConcurrentHashMap<>();
    }

    public void register(String eventId, long intervalTicks, long currentTick, Runnable task) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(task, "task");
        entries.put(eventId, new Entry(eventId, intervalTicks, currentTick, task));
    }

    public void unregister(String eventId) {
        if (eventId == null) {
            return;
        }
        entries.remove(eventId);
    }

    public void tick(long currentTick) {
        for (Entry entry : entries.values()) {
            entry.onTick(currentTick);
        }
    }

    public List<IntervalStatus> statuses(long currentTick) {
        List<IntervalStatus> list = new ArrayList<>(entries.size());
        for (Entry entry : entries.values()) {
            list.add(entry.status(currentTick));
        }
        list.sort(Comparator.comparingLong(IntervalStatus::remainingTicks)
            .thenComparing(IntervalStatus::eventId));
        return list;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
