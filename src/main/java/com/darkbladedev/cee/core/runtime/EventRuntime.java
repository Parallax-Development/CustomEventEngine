package com.darkbladedev.cee.core.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.EventState;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import com.darkbladedev.cee.core.flow.ExecutionResult;
import com.darkbladedev.cee.core.flow.Instruction;

public final class EventRuntime {
    private final UUID runtimeId;
    private final String eventId;
    private final ExecutionPlan plan;
    private final EventContextImpl context;
    private final ChunkPos origin;
    private final Set<UUID> chunkLocks;
    private final Deque<Integer> loopCounters;
    private final List<EventRuntime> asyncChildren;
    private volatile EventState state;
    private int instructionPointer;
    private long waitUntilTick;

    public EventRuntime(UUID runtimeId, String eventId, ExecutionPlan plan, EventContextImpl context, ChunkPos origin) {
        this.runtimeId = Objects.requireNonNull(runtimeId, "runtimeId");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.context = Objects.requireNonNull(context, "context");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.chunkLocks = ConcurrentHashMap.newKeySet();
        this.loopCounters = new ArrayDeque<>();
        this.asyncChildren = new ArrayList<>();
        this.state = EventState.CREATED;
    }

    public UUID getRuntimeId() {
        return runtimeId;
    }

    public String getEventId() {
        return eventId;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public EventContextImpl getContext() {
        return context;
    }

    public ChunkPos getOrigin() {
        return origin;
    }

    public int getInstructionPointer() {
        return instructionPointer;
    }

    public void setInstructionPointer(int instructionPointer) {
        this.instructionPointer = instructionPointer;
    }

    public void addParticipant(UUID playerId) {
        context.getParticipants().add(playerId);
    }

    public void removeParticipant(UUID playerId) {
        context.getParticipants().remove(playerId);
    }

    public Set<UUID> getParticipants() {
        return context.getParticipants();
    }

    public void addChunkLock(UUID chunkKey) {
        chunkLocks.add(chunkKey);
    }

    public Set<UUID> getChunkLocks() {
        return chunkLocks;
    }

    public void pushLoopCounter(int times) {
        loopCounters.push(times);
    }

    public boolean decrementLoopCounter() {
        if (loopCounters.isEmpty()) {
            return false;
        }
        int value = loopCounters.pop() - 1;
        if (value > 0) {
            loopCounters.push(value);
            return true;
        }
        return false;
    }

    public void popLoopCounter() {
        if (!loopCounters.isEmpty()) {
            loopCounters.pop();
        }
    }

    public boolean isWaiting() {
        return waitUntilTick > 0;
    }

    public void waitFor(long ticks) {
        waitUntilTick = context.getCurrentTick() + ticks;
        state = EventState.WAITING;
    }

    public boolean isWaitingComplete() {
        return waitUntilTick > 0 && context.getCurrentTick() >= waitUntilTick;
    }

    public void clearWaiting() {
        waitUntilTick = 0L;
        state = EventState.RUNNING;
    }

    public long getWaitRemaining() {
        if (waitUntilTick <= 0) {
            return 0L;
        }
        return Math.max(0L, waitUntilTick - context.getCurrentTick());
    }

    public void setWaitRemaining(long ticks) {
        if (ticks <= 0) {
            waitUntilTick = 0L;
            return;
        }
        waitUntilTick = context.getCurrentTick() + ticks;
        state = EventState.WAITING;
    }

    public boolean hasAsyncChildren() {
        return !asyncChildren.isEmpty();
    }

    public void startAsync(List<ExecutionPlan> branches) {
        asyncChildren.clear();
        for (ExecutionPlan branch : branches) {
            EventRuntime child = new EventRuntime(UUID.randomUUID(), eventId, branch, context, origin);
            child.setState(EventState.RUNNING);
            asyncChildren.add(child);
        }
    }

    public boolean areAsyncChildrenComplete() {
        for (EventRuntime child : asyncChildren) {
            if (child.getState() != EventState.FINISHED && child.getState() != EventState.CANCELLED) {
                return false;
            }
        }
        return true;
    }

    public void clearAsync() {
        asyncChildren.clear();
    }

    public void tick() {
        if (state == EventState.CANCELLED || state == EventState.FINISHED) {
            return;
        }
        if (state == EventState.CREATED) {
            state = EventState.RUNNING;
        }
        if (hasAsyncChildren()) {
            for (EventRuntime child : asyncChildren) {
                child.tick();
            }
        }
        while (instructionPointer < plan.size()) {
            Instruction instruction = plan.get(instructionPointer);
            ExecutionResult result = instruction.execute(this);
            if (result == ExecutionResult.CONTINUE) {
                instructionPointer++;
                continue;
            }
            if (result == ExecutionResult.WAIT) {
                return;
            }
            if (result == ExecutionResult.STOP) {
                state = EventState.FINISHED;
                return;
            }
        }
        state = EventState.FINISHED;
    }

    public Map<String, Object> getVariables() {
        return context.getVariables();
    }
}
