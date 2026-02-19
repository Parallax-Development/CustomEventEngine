package com.darkbladedev.cee.core.persistence;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import com.darkbladedev.cee.core.runtime.EventContextImpl;
import com.darkbladedev.cee.core.runtime.EventEngine;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import com.darkbladedev.cee.util.ChunkUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PersistenceManager {
    private final Plugin plugin;
    private final EventEngine engine;
    private final Gson gson;
    private final File file;

    public PersistenceManager(Plugin plugin, EventEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
        this.gson = new GsonBuilder().create();
        this.file = new File(plugin.getDataFolder(), "runtime.json");
    }

    public void save() {
        List<RuntimeSnapshot> snapshots = new ArrayList<>();
        for (EventRuntime runtime : engine.getScheduler().getRuntimes().values()) {
            RuntimeSnapshot snapshot = new RuntimeSnapshot();
            snapshot.setEventId(runtime.getEventId());
            snapshot.setWorldId(runtime.getContext().getWorld().getUID());
            snapshot.setOriginX(runtime.getOrigin().getX());
            snapshot.setOriginZ(runtime.getOrigin().getZ());
            List<ChunkSnapshot> chunks = new ArrayList<>();
            for (ChunkPos pos : engine.getLockManager().getOccupiedChunks(runtime)) {
                chunks.add(new ChunkSnapshot(pos.getX(), pos.getZ()));
            }
            snapshot.setChunks(chunks);
            snapshot.setInstructionPointer(runtime.getInstructionPointer());
            snapshot.setWaitRemaining(runtime.getWaitRemaining());
            snapshot.setVariables(runtime.getVariables());
            snapshots.add(snapshot);
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(snapshots, writer);
        } catch (Exception ignored) {
        }
    }

    public void load(Map<String, EventDefinition> definitions) {
        if (!file.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            RuntimeSnapshot[] snapshots = gson.fromJson(reader, RuntimeSnapshot[].class);
            if (snapshots == null) {
                return;
            }
            for (RuntimeSnapshot snapshot : snapshots) {
                EventDefinition definition = definitions.get(snapshot.getEventId());
                if (definition == null) {
                    continue;
                }
                World world = plugin.getServer().getWorld(snapshot.getWorldId());
                if (world == null) {
                    continue;
                }
                EventContextImpl context = new EventContextImpl(plugin.getServer(), world);
                if (snapshot.getVariables() != null) {
                    context.getVariables().putAll(snapshot.getVariables());
                }
                ExecutionPlan plan = engine.createExecutionPlan(definition);
                ChunkPos origin = ChunkUtil.fromWorld(world, snapshot.getOriginX(), snapshot.getOriginZ());
                EventRuntime runtime = new EventRuntime(UUID.randomUUID(), definition.getId(), plan, context, origin);
                runtime.setInstructionPointer(snapshot.getInstructionPointer());
                runtime.setWaitRemaining(snapshot.getWaitRemaining());
                List<ChunkPos> chunks = new ArrayList<>();
                if (snapshot.getChunks() != null) {
                    for (ChunkSnapshot chunk : snapshot.getChunks()) {
                        chunks.add(ChunkUtil.fromWorld(world, chunk.getX(), chunk.getZ()));
                    }
                }
                if (engine.getLockManager().tryLock(new java.util.HashSet<>(chunks), runtime)) {
                    engine.addRuntime(runtime, definition);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
