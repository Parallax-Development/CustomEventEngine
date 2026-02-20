package com.darkbladedev.cee.core.dsl;

import com.darkbladedev.cee.api.EventContext;
import com.darkbladedev.cee.core.actions.ExecuteConsoleCommandAction;
import com.darkbladedev.cee.core.actions.ExecuteParticipantsCommandAction;
import com.darkbladedev.cee.core.actions.LogAction;
import com.darkbladedev.cee.core.conditions.AnyParticipantHasPermissionCondition;
import com.darkbladedev.cee.core.conditions.IsDayCondition;
import com.darkbladedev.cee.core.conditions.IsNightCondition;
import com.darkbladedev.cee.core.conditions.IsRainingCondition;
import com.darkbladedev.cee.core.conditions.IsThunderingCondition;
import com.darkbladedev.cee.core.conditions.ParticipantsCountGreaterCondition;
import com.darkbladedev.cee.core.conditions.ParticipantsCountLessCondition;
import com.darkbladedev.cee.core.conditions.WorldDifficultyIsCondition;
import org.bukkit.Difficulty;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompatiblePackTest {
    @Test
    void timeConditionsWork() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        world.setTime(6000);
        assertTrue(new IsDayCondition().evaluate(ctx));
        assertFalse(new IsNightCondition().evaluate(ctx));

        world.setTime(14000);
        assertFalse(new IsDayCondition().evaluate(ctx));
        assertTrue(new IsNightCondition().evaluate(ctx));
    }

    @Test
    void weatherConditionsWork() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        world.setStorm(true);
        world.setThundering(false);
        assertTrue(new IsRainingCondition().evaluate(ctx));
        assertFalse(new IsThunderingCondition().evaluate(ctx));

        world.setThundering(true);
        assertTrue(new IsThunderingCondition().evaluate(ctx));
    }

    @Test
    void difficultyConditionWorks() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        world.setDifficulty(Difficulty.HARD);
        assertTrue(new WorldDifficultyIsCondition(Difficulty.HARD).evaluate(ctx));
        assertFalse(new WorldDifficultyIsCondition(Difficulty.EASY).evaluate(ctx));
    }

    @Test
    void participantCountConditionsWork() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        ctx.getParticipants().add(UUID.randomUUID());
        ctx.getParticipants().add(UUID.randomUUID());

        assertTrue(new ParticipantsCountGreaterCondition(1).evaluate(ctx));
        assertFalse(new ParticipantsCountGreaterCondition(2).evaluate(ctx));
        assertTrue(new ParticipantsCountLessCondition(3).evaluate(ctx));
        assertFalse(new ParticipantsCountLessCondition(2).evaluate(ctx));
    }

    @Test
    void anyParticipantHasPermissionConditionWorks() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        server.players.put(p1, server.playerProxy(Set.of()));
        server.players.put(p2, server.playerProxy(Set.of("cee.admin")));

        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());
        ctx.getParticipants().add(p1);
        ctx.getParticipants().add(p2);

        assertTrue(new AnyParticipantHasPermissionCondition("cee.admin").evaluate(ctx));
        assertFalse(new AnyParticipantHasPermissionCondition("other.perm").evaluate(ctx));
    }

    @Test
    void executeConsoleCommandActionDispatchesCommand() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        new ExecuteConsoleCommandAction("/say hola").execute(ctx);
        assertTrue(server.dispatched.contains("say hola"));
    }

    @Test
    void executeParticipantsCommandActionRunsOnPlayers() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        AtomicReference<String> p1Cmd = new AtomicReference<>();
        AtomicReference<String> p2Cmd = new AtomicReference<>();
        server.players.put(p1, server.playerProxy(Set.of(), p1Cmd));
        server.players.put(p2, server.playerProxy(Set.of(), p2Cmd));

        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());
        ctx.getParticipants().add(p1);
        ctx.getParticipants().add(p2);

        new ExecuteParticipantsCommandAction("msg @s hi").execute(ctx);
        assertTrue("msg @s hi".equals(p1Cmd.get()));
        assertTrue("msg @s hi".equals(p2Cmd.get()));
    }

    @Test
    void logActionUsesServerLogger() {
        TestWorld world = new TestWorld();
        TestServer server = new TestServer();
        TestContext ctx = new TestContext(server.asServerProxy(), world.asWorldProxy());

        new LogAction("hola").execute(ctx);
        assertTrue("hola".equals(server.logged.get()));
    }

    private static final class TestContext implements EventContext {
        private final Server server;
        private final World world;
        private final Set<UUID> participants = new HashSet<>();
        private final Map<String, Object> vars = new HashMap<>();

        private TestContext(Server server, World world) {
            this.server = server;
            this.world = world;
        }

        @Override
        public Server getServer() {
            return server;
        }

        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public Set<UUID> getParticipants() {
            return participants;
        }

        @Override
        public Map<String, Object> getVariables() {
            return vars;
        }

        @Override
        public Object getVariable(String key) {
            return vars.get(key);
        }

        @Override
        public void setVariable(String key, Object value) {
            vars.put(key, value);
        }
    }

    private static final class TestWorld {
        private long time;
        private boolean storm;
        private boolean thundering;
        private Difficulty difficulty = Difficulty.NORMAL;

        private World asWorldProxy() {
            return proxy(World.class, (proxy, method, args) -> switch (method.getName()) {
                case "getTime" -> time;
                case "hasStorm" -> storm;
                case "isThundering" -> thundering;
                case "getDifficulty" -> difficulty;
                default -> defaultValue(method.getReturnType());
            });
        }

        private void setTime(long time) {
            this.time = time;
        }

        private void setStorm(boolean storm) {
            this.storm = storm;
        }

        private void setThundering(boolean thundering) {
            this.thundering = thundering;
        }

        private void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }
    }

    private static final class TestServer {
        private final List<String> dispatched = new ArrayList<>();
        private final Map<UUID, Player> players = new HashMap<>();
        private final AtomicReference<String> logged = new AtomicReference<>();

        private Server asServerProxy() {
            Logger logger = Logger.getLogger("test");
            logger.setUseParentHandlers(false);

            return proxy(Server.class, (proxy, method, args) -> switch (method.getName()) {
                case "getLogger" -> new Logger("capture", null) {
                    @Override
                    public void info(String msg) {
                        logged.set(msg);
                    }
                };
                case "getConsoleSender" -> proxy(ConsoleCommandSender.class, (p, m, a) -> defaultValue(m.getReturnType()));
                case "dispatchCommand" -> {
                    if (args != null && args.length >= 2 && args[1] instanceof String cmd) {
                        dispatched.add(cmd);
                    }
                    yield true;
                }
                case "getPlayer" -> {
                    if (args != null && args.length >= 1 && args[0] instanceof UUID uuid) {
                        yield players.get(uuid);
                    }
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private Player playerProxy(Set<String> permissions) {
            return playerProxy(permissions, new AtomicReference<>());
        }

        private Player playerProxy(Set<String> permissions, AtomicReference<String> lastCommand) {
            return proxy(Player.class, (proxy, method, args) -> switch (method.getName()) {
                case "hasPermission" -> {
                    if (args != null && args.length >= 1 && args[0] instanceof String perm) {
                        yield permissions.contains(perm);
                    }
                    yield false;
                }
                case "performCommand" -> {
                    if (args != null && args.length >= 1 && args[0] instanceof String cmd) {
                        lastCommand.set(cmd);
                    }
                    yield true;
                }
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
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
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
