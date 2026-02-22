package com.darkbladedev.cee.core.flow;

import com.darkbladedev.cee.api.ChunkPos;
import com.darkbladedev.cee.api.EventState;
import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.loader.EventLoader;
import com.darkbladedev.cee.core.loader.TaskValidator;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskNodesTest {
    @Test
    void taskCallPassesArgsAndCapturesReturn() throws IOException {
        var def = loadSingleEvent("""
            event:
              id: "t"
              tasks:
                add:
                  params:
                    a: number
                    b: number
                  returns:
                    sum: number
                  flow:
                    nodes:
                      - return:
                          sum: "= a + b"
              flow:
                nodes:
                  - task:
                      name: "add"
                      with:
                        a: 2
                        b: 3
                      into:
                        sum: "result"
            """);

        EventContextImpl ctx = new EventContextImpl(serverProxy(), worldProxy());
        FlowCompiler compiler = new FlowCompiler(node -> c -> {});
        ExecutionPlan plan = compiler.compile(def.getFlow());
        Map<String, TaskDefinition> tasks = def.getTasks();

        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), def.getId(), plan, compiler, ctx, new ChunkPos(UUID.randomUUID(), 0, 0), tasks);
        runtime.tick();

        assertEquals(5, ((Number) ctx.getVariable("result")).intValue());
    }

    @Test
    void taskOverrideRedefinesBehaviorForCallSite() throws IOException {
        var def = loadSingleEvent("""
            event:
              id: "t"
              tasks:
                base:
                  params:
                    x: number
                  returns:
                    out: number
                  flow:
                    nodes:
                      - return:
                          out: "= x + 1"
              flow:
                nodes:
                  - task:
                      name: "base"
                      with:
                        x: 1
                      into:
                        out: "a"
                  - task:
                      name: "base"
                      with:
                        x: 1
                      into:
                        out: "b"
                      override:
                        params:
                          x: number
                        returns:
                          out: number
                        nodes:
                          - return:
                              out: "= x + 5"
            """);

        EventContextImpl ctx = new EventContextImpl(serverProxy(), worldProxy());
        FlowCompiler compiler = new FlowCompiler(node -> c -> {});
        ExecutionPlan plan = compiler.compile(def.getFlow());
        Map<String, TaskDefinition> tasks = def.getTasks();

        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), def.getId(), plan, compiler, ctx, new ChunkPos(UUID.randomUUID(), 0, 0), tasks);
        runtime.tick();

        assertEquals(2, ((Number) ctx.getVariable("a")).intValue());
        assertEquals(6, ((Number) ctx.getVariable("b")).intValue());
    }

    @Test
    void taskValidatorDetectsMissingRequiredParam() throws IOException {
        var def = loadSingleEvent("""
            event:
              id: "t"
              tasks:
                needs:
                  params:
                    x:
                      type: number
                      required: true
                  returns:
                    out: number
                  flow:
                    nodes:
                      - return:
                          out: "= x"
              flow:
                nodes:
                  - task:
                      name: "needs"
                      into:
                        out: "a"
            """);

        List<String> errors = new TaskValidator().validate(def, Map.of());
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(line -> line.contains("falta parámetro requerido 'x'")));
    }

    @Test
    void taskRecursionStopsAtMaxDepth() throws IOException {
        var def = loadSingleEvent("""
            event:
              id: "t"
              tasks:
                recurse:
                  flow:
                    nodes:
                      - task:
                          name: "recurse"
                          max_depth: 3
              flow:
                nodes:
                  - task:
                      name: "recurse"
                      max_depth: 3
            """);

        EventContextImpl ctx = new EventContextImpl(serverProxy(), worldProxy());
        FlowCompiler compiler = new FlowCompiler(node -> c -> {});
        ExecutionPlan plan = compiler.compile(def.getFlow());
        Map<String, TaskDefinition> tasks = def.getTasks();

        EventRuntime runtime = new EventRuntime(UUID.randomUUID(), def.getId(), plan, compiler, ctx, new ChunkPos(UUID.randomUUID(), 0, 0), tasks);
        runtime.tick();

        assertEquals(EventState.CANCELLED, runtime.getState());
        assertNotNull(runtime.getLastError());
        assertTrue(runtime.getLastError().contains("max_depth"));
    }

    @Test
    void taskValidationScalesToManyDefinitions() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Map<String, TaskDefinition> tasks = new LinkedHashMap<>();
            for (int i = 0; i < 2000; i++) {
                String name = "t" + i;
                tasks.put(name, new TaskDefinition(name, "", Map.of(), Map.of(), new FlowDefinition(List.of()), "test"));
            }
            TaskValidator validator = new TaskValidator();
            for (TaskDefinition task : tasks.values()) {
                List<String> errors = validator.validate(task, tasks);
                if (!errors.isEmpty()) {
                    throw new IllegalStateException(errors.toString());
                }
            }
        });
    }

    private static com.darkbladedev.cee.core.definition.EventDefinition loadSingleEvent(String yaml) throws IOException {
        Path dir = Files.createTempDirectory("cee-task-test");
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

