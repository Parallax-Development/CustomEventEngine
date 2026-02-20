package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.core.actions.SetVariableAction;
import com.darkbladedev.cee.core.definition.ActionDefinition;
import com.darkbladedev.cee.core.definition.ActionNodeDefinition;
import com.darkbladedev.cee.core.definition.EventDefinition;
import com.darkbladedev.cee.core.loader.EventLoader;
import com.darkbladedev.cee.core.runtime.EventContextImpl;
import com.darkbladedev.cee.core.runtime.EventRuntime;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoopNodesTest {
    @Test
    void loopRepeatsBodyTimes() throws IOException {
        EventDefinition def = loadSingleEvent("""
            event:
              id: "t"
              flow:
                nodes:
                  - action: "set_variable"
                    key: "counter"
                    value: 0
                  - loop: 3
                    nodes:
                      - action: "set_variable"
                        key: "counter"
                        value: "= (counter == null ? 0 : counter) + 1"
            """);

        EventContextImpl ctx = new EventContextImpl(serverProxy(), worldProxy());
        ExecutionPlan plan = new FlowCompiler(this::buildAction).compile(def.getFlow());
        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), def.getId(), plan, ctx, new ChunkPos(UUID.randomUUID(), 0, 0));
        runtime.tick();

        Object counter = ctx.getVariable("counter");
        assertEquals(3, ((Number) counter).intValue());
    }

    @Test
    void conditionLoopStopsWhenExpressionBecomesFalse() throws IOException {
        EventDefinition def = loadSingleEvent("""
            event:
              id: "t"
              flow:
                nodes:
                  - action: "set_variable"
                    key: "counter"
                    value: 0
                  - condition_loop:
                      times: 10
                      expression: "counter < 3"
                      nodes:
                        - action: "set_variable"
                          key: "counter"
                          value: "= (counter == null ? 0 : counter) + 1"
            """);

        EventContextImpl ctx = new EventContextImpl(serverProxy(), worldProxy());
        ExecutionPlan plan = new FlowCompiler(this::buildAction).compile(def.getFlow());
        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), def.getId(), plan, ctx, new ChunkPos(UUID.randomUUID(), 0, 0));
        runtime.tick();

        Object counter = ctx.getVariable("counter");
        assertEquals(3, ((Number) counter).intValue());
    }

    private com.darkbladedev.cee.api.Action buildAction(ActionNodeDefinition node) {
        ActionDefinition action = node.getAction();
        if (action.getType().equals("set_variable")) {
            Object key = action.getConfig().getOrDefault("key", "");
            Object value = action.getConfig().get("value");
            return new SetVariableAction(String.valueOf(key), value);
        }
        return ctx -> {};
    }

    private static EventDefinition loadSingleEvent(String yaml) throws IOException {
        Path dir = Files.createTempDirectory("cee-loop-test");
        Path file = dir.resolve("event.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        EventLoader loader = new EventLoader();
        return loader.loadFromFolder(dir.toFile()).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> {
            if (method.getName().equals("toString")) {
                return type.getSimpleName() + "Proxy";
            }
            Class<?> ret = method.getReturnType();
            if (!ret.isPrimitive()) {
                return null;
            }
            if (ret == boolean.class) {
                return false;
            }
            if (ret == byte.class) {
                return (byte) 0;
            }
            if (ret == short.class) {
                return (short) 0;
            }
            if (ret == int.class) {
                return 0;
            }
            if (ret == long.class) {
                return 0L;
            }
            if (ret == float.class) {
                return 0.0f;
            }
            if (ret == double.class) {
                return 0.0d;
            }
            if (ret == char.class) {
                return '\0';
            }
            return null;
        });
    }

    private static Server serverProxy() {
        return proxy(Server.class);
    }

    private static World worldProxy() {
        return proxy(World.class);
    }
}
