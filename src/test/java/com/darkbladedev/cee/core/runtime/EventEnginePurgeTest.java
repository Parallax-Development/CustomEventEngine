package com.darkbladedev.cee.core.runtime;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.core.flow.ExecutionPlan;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventEnginePurgeTest {
    @Test
    void purgeWorldCancelsRuntimesAndFreesChunks() {
        UUID worldA = UUID.randomUUID();
        UUID worldB = UUID.randomUUID();

        Server server = serverProxy();
        Plugin plugin = pluginProxy(server);
        EventEngine engine = new EventEngine(plugin);

        World a = worldProxy(worldA, "worldA");
        World b = worldProxy(worldB, "worldB");

        EventRuntime rtA1 = runtime(server, a, "e1", new ChunkPos(worldA, 0, 0));
        EventRuntime rtA2 = runtime(server, a, "e2", new ChunkPos(worldA, 10, 10));
        EventRuntime rtB = runtime(server, b, "e3", new ChunkPos(worldB, 0, 0));

        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldA, 0, 0), new ChunkPos(worldA, 1, 0)), rtA1));
        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldA, 10, 10)), rtA2));
        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldB, 0, 0)), rtB));

        engine.getScheduler().addRuntime(rtA1);
        engine.getScheduler().addRuntime(rtA2);
        engine.getScheduler().addRuntime(rtB);

        EventEngine.PurgeResult result = engine.purgeWorld(worldA);
        assertEquals(2, result.runtimesPurged());
        assertEquals(3, result.chunksFreed());
        assertEquals(0, result.schedulersDisabled());

        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 0, 0)));
        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 1, 0)));
        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 10, 10)));
        assertTrue(engine.getLockManager().isOccupied(new ChunkPos(worldB, 0, 0)));

        assertFalse(engine.getScheduler().getRuntimes().containsKey(rtA1.getRuntimeId()));
        assertFalse(engine.getScheduler().getRuntimes().containsKey(rtA2.getRuntimeId()));
        assertTrue(engine.getScheduler().getRuntimes().containsKey(rtB.getRuntimeId()));
    }

    @Test
    void purgeChunkCancelsOneRuntimeAndReleasesAllItsLocks() {
        UUID worldA = UUID.randomUUID();

        Server server = serverProxy();
        Plugin plugin = pluginProxy(server);
        EventEngine engine = new EventEngine(plugin);

        World a = worldProxy(worldA, "worldA");
        EventRuntime rt = runtime(server, a, "e1", new ChunkPos(worldA, 0, 0));

        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldA, 0, 0), new ChunkPos(worldA, 0, 1)), rt));
        engine.getScheduler().addRuntime(rt);

        EventEngine.PurgeResult result = engine.purgeChunk(new ChunkPos(worldA, 0, 1));
        assertEquals(1, result.runtimesPurged());
        assertEquals(2, result.chunksFreed());
        assertEquals(0, result.schedulersDisabled());

        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 0, 0)));
        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 0, 1)));
        assertFalse(engine.getScheduler().getRuntimes().containsKey(rt.getRuntimeId()));
    }

    @Test
    void purgeRegionChunksPurgesOnlyChunksInRange() {
        UUID worldA = UUID.randomUUID();

        Server server = serverProxy();
        Plugin plugin = pluginProxy(server);
        EventEngine engine = new EventEngine(plugin);

        World a = worldProxy(worldA, "worldA");
        EventRuntime in = runtime(server, a, "in", new ChunkPos(worldA, 5, 5));
        EventRuntime out = runtime(server, a, "out", new ChunkPos(worldA, 50, 50));

        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldA, 5, 5), new ChunkPos(worldA, 6, 5)), in));
        assertTrue(engine.getLockManager().tryLock(Set.of(new ChunkPos(worldA, 50, 50)), out));

        engine.getScheduler().addRuntime(in);
        engine.getScheduler().addRuntime(out);

        EventEngine.PurgeResult result = engine.purgeRegionChunks(worldA, 4, 4, 10, 10);
        assertEquals(1, result.runtimesPurged());
        assertEquals(2, result.chunksFreed());
        assertEquals(0, result.schedulersDisabled());

        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 5, 5)));
        assertFalse(engine.getLockManager().isOccupied(new ChunkPos(worldA, 6, 5)));
        assertTrue(engine.getLockManager().isOccupied(new ChunkPos(worldA, 50, 50)));
    }

    private static EventRuntime runtime(Server server, World world, String eventId, ChunkPos origin) {
        EventContextImpl ctx = new EventContextImpl(server, world);
        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), eventId, new ExecutionPlan(List.of()), ctx, origin);
        runtime.setState(com.darkbladedev.cee.api.EventState.RUNNING);
        return runtime;
    }

    private static Plugin pluginProxy(Server server) {
        Logger logger = Logger.getLogger("CEE-Test");
        return (Plugin) Proxy.newProxyInstance(
            EventEnginePurgeTest.class.getClassLoader(),
            new Class[]{Plugin.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getServer" -> server;
                case "getLogger" -> logger;
                case "getName" -> "CEE-Test";
                case "toString" -> "CEE-Test";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Server serverProxy() {
        return (Server) Proxy.newProxyInstance(
            EventEnginePurgeTest.class.getClassLoader(),
            new Class[]{Server.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "toString" -> "ServerProxy";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static World worldProxy(UUID uid, String name) {
        return (World) Proxy.newProxyInstance(
            EventEnginePurgeTest.class.getClassLoader(),
            new Class[]{World.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUID" -> uid;
                case "getName" -> name;
                case "toString" -> "WorldProxy(" + name + ")";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
