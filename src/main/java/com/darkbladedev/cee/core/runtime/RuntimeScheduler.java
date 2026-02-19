package com.darkbladedev.cee.core.runtime;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RuntimeScheduler {
    private final Plugin plugin;
    private final Map<UUID, EventRuntime> runtimes;
    private final Consumer<EventRuntime> onComplete;
    private final IntervalScheduleRegistry intervalSchedules;
    private BukkitRunnable task;
    private long currentTick;
    private long tickStep;

    public RuntimeScheduler(Plugin plugin, Consumer<EventRuntime> onComplete) {
        this.plugin = plugin;
        this.runtimes = new ConcurrentHashMap<>();
        this.onComplete = onComplete;
        this.intervalSchedules = new IntervalScheduleRegistry();
        this.tickStep = 1L;
    }

    public void addRuntime(EventRuntime runtime) {
        runtimes.put(runtime.getRuntimeId(), runtime);
    }

    public void removeRuntime(UUID runtimeId) {
        runtimes.remove(runtimeId);
    }

    public Map<UUID, EventRuntime> getRuntimes() {
        return runtimes;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void registerInterval(String eventId, long intervalTicks, Runnable task) {
        intervalSchedules.register(eventId, intervalTicks, currentTick, task);
    }

    public void unregisterInterval(String eventId) {
        intervalSchedules.unregister(eventId);
    }

    public java.util.List<IntervalScheduleRegistry.IntervalStatus> getIntervalStatuses() {
        return intervalSchedules.statuses(currentTick);
    }

    public void start(long intervalTicks) {
        if (task != null) {
            return;
        }
        tickStep = Math.max(1L, intervalTicks);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                currentTick += tickStep;
                intervalSchedules.tick(currentTick);
                for (EventRuntime runtime : runtimes.values()) {
                    runtime.getContext().setCurrentTick(currentTick);
                    runtime.tick();
                    if (runtime.getState() == com.darkbladedev.cee.api.EventState.FINISHED || runtime.getState() == com.darkbladedev.cee.api.EventState.CANCELLED) {
                        runtimes.remove(runtime.getRuntimeId());
                        onComplete.accept(runtime);
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 1L, tickStep);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
