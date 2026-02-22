package com.darkbladedev.cee.core.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.EventState;
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import com.darkbladedev.cee.core.flow.ExecutionResult;
import com.darkbladedev.cee.core.flow.FlowCompiler;
import com.darkbladedev.cee.core.flow.Instruction;

public final class EventRuntime {
    private final UUID runtimeId;
    private final String eventId;
    private final FlowCompiler compiler;
    private final EventContextImpl context;
    private final ChunkPos origin;
    private final Map<String, TaskDefinition> tasks;
    private final Set<UUID> chunkLocks;
    private final Deque<Integer> loopCounters;
    private final List<EventRuntime> asyncChildren;
    private final Deque<ExecutionFrame> frames;
    private final Deque<TaskCallFrame> taskFrames;
    private final Map<String, ExecutionPlan> taskPlanCache;
    private volatile EventState state;
    private long waitUntilTick;
    private String lastError;

    public EventRuntime(UUID runtimeId,
                        String eventId,
                        ExecutionPlan plan,
                        FlowCompiler compiler,
                        EventContextImpl context,
                        ChunkPos origin,
                        Map<String, TaskDefinition> tasks) {
        this.runtimeId = Objects.requireNonNull(runtimeId, "runtimeId");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.context = Objects.requireNonNull(context, "context");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.chunkLocks = ConcurrentHashMap.newKeySet();
        this.loopCounters = new ArrayDeque<>();
        this.asyncChildren = new ArrayList<>();
        this.frames = new ArrayDeque<>();
        this.frames.push(new ExecutionFrame(Objects.requireNonNull(plan, "plan")));
        this.taskFrames = new ArrayDeque<>();
        this.taskPlanCache = new HashMap<>();
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
        ExecutionFrame frame = frames.peek();
        return frame == null ? 0 : frame.instructionPointer;
    }

    public void setInstructionPointer(int instructionPointer) {
        ExecutionFrame frame = frames.peek();
        if (frame != null) {
            frame.instructionPointer = instructionPointer;
        }
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

    public void trimLoopCountersToSize(int size) {
        while (loopCounters.size() > size) {
            loopCounters.pop();
        }
    }

    public int getLoopCounterDepth() {
        return loopCounters.size();
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
            EventRuntime child = new EventRuntime(UUID.randomUUID(), eventId, branch, compiler, context, origin, tasks);
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
        while (true) {
            ExecutionFrame frame = frames.peek();
            if (frame == null) {
                state = EventState.FINISHED;
                return;
            }
            if (frame.instructionPointer >= frame.plan.size()) {
                if (!taskFrames.isEmpty()) {
                    completeCurrentTask(Map.of());
                    continue;
                }
                state = EventState.FINISHED;
                return;
            }
            Instruction instruction = frame.plan.get(frame.instructionPointer);
            ExecutionResult result = instruction.execute(this);
            if (result == ExecutionResult.CONTINUE) {
                frame.instructionPointer++;
                continue;
            }
            if (result == ExecutionResult.WAIT) {
                return;
            }
            if (result == ExecutionResult.STOP) {
                if (state != EventState.CANCELLED) {
                    state = EventState.FINISHED;
                }
                return;
            }
        }
    }

    public Map<String, Object> getVariables() {
        return context.getVariables();
    }

    public FlowCompiler getCompiler() {
        return compiler;
    }

    public Map<String, TaskDefinition> getTasks() {
        return tasks;
    }

    public TaskDefinition getTaskDefinition(String name) {
        if (name == null) {
            return null;
        }
        return tasks.get(name);
    }

    public int getTaskDepth() {
        return taskFrames.size();
    }

    public ExecutionPlan getOrCompileTaskPlan(TaskDefinition definition) {
        String key = definition.getSource() + "|" + definition.getName();
        return taskPlanCache.computeIfAbsent(key, k -> compiler.compile(definition.getFlow()));
    }

    public void beginTask(TaskCallFrame frame, ExecutionPlan plan) {
        taskFrames.push(frame);
        frames.push(new ExecutionFrame(plan));
    }

    public boolean completeCurrentTask(Map<String, Object> returns) {
        if (taskFrames.isEmpty()) {
            return false;
        }
        frames.pop();
        TaskCallFrame frame = taskFrames.pop();
        restoreVariableState(frame);
        trimLoopCountersToSize(frame.loopCounterDepth);
        applyReturnMapping(frame, returns);
        return true;
    }

    public void fail(String message) {
        this.lastError = message;
        this.state = EventState.CANCELLED;
    }

    public String getLastError() {
        return lastError;
    }

    private void restoreVariableState(TaskCallFrame frame) {
        Map<String, Object> local = context.getLocalVariables();
        Map<String, Object> global = context.getGlobalVariables();

        for (String key : frame.localMissing) {
            local.remove(key);
        }
        for (Map.Entry<String, Object> entry : frame.localBackup.entrySet()) {
            local.put(entry.getKey(), entry.getValue());
        }

        for (String key : frame.globalMissing) {
            global.remove(key);
        }
        for (Map.Entry<String, Object> entry : frame.globalBackup.entrySet()) {
            global.put(entry.getKey(), entry.getValue());
        }
    }

    private void applyReturnMapping(TaskCallFrame frame, Map<String, Object> returns) {
        if (frame.into.isEmpty() || returns == null || returns.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : frame.into.entrySet()) {
            String returnName = entry.getKey();
            String targetVar = entry.getValue();
            if (targetVar == null || targetVar.isBlank()) {
                continue;
            }
            if (!returns.containsKey(returnName)) {
                continue;
            }
            context.setVariable(targetVar, returns.get(returnName));
        }
    }

    public static final class TaskCallFrame {
        private final Map<String, Object> localBackup;
        private final Set<String> localMissing;
        private final Map<String, Object> globalBackup;
        private final Set<String> globalMissing;
        private final Map<String, String> into;
        private final int loopCounterDepth;

        public TaskCallFrame(Map<String, Object> localBackup,
                             Set<String> localMissing,
                             Map<String, Object> globalBackup,
                             Set<String> globalMissing,
                             Map<String, String> into,
                             int loopCounterDepth) {
            this.localBackup = localBackup;
            this.localMissing = localMissing;
            this.globalBackup = globalBackup;
            this.globalMissing = globalMissing;
            this.into = into;
            this.loopCounterDepth = loopCounterDepth;
        }
    }

    private static final class ExecutionFrame {
        private final ExecutionPlan plan;
        private int instructionPointer;

        private ExecutionFrame(ExecutionPlan plan) {
            this.plan = plan;
            this.instructionPointer = 0;
        }
    }
}
