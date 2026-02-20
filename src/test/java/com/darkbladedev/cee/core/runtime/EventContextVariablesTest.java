package com.darkbladedev.cee.core.runtime;

import com.darkbladedev.cee.core.definition.VariableDefinition;
import com.darkbladedev.cee.core.definition.VariableScope;
import com.darkbladedev.cee.core.definition.VariableType;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EventContextVariablesTest {
    @Test
    void writesToGlobalRequireLocalDefinitionWithGlobalScope() {
        Map<String, Object> global = new HashMap<>();
        Map<String, VariableDefinition> globalDefs = Map.of(
            "g", new VariableDefinition("g", VariableType.NUMBER, VariableScope.GLOBAL, 0, "")
        );

        EventContextImpl ctxWithoutDef = new EventContextImpl(serverProxy(), worldProxy(), global, Map.of(), globalDefs);
        ctxWithoutDef.setVariable("g", 5);

        assertNull(global.get("g"));
        assertEquals(5, ((Number) ctxWithoutDef.getVariable("g")).intValue());

        Map<String, VariableDefinition> localDefs = Map.of(
            "g", new VariableDefinition("g", VariableType.NUMBER, VariableScope.GLOBAL, 0, "")
        );
        EventContextImpl ctxWithDef = new EventContextImpl(serverProxy(), worldProxy(), global, localDefs, globalDefs);
        ctxWithDef.setVariable("g", 7);

        assertEquals(7, ((Number) global.get("g")).intValue());
    }

    @Test
    void localVariablesDoNotPersistBetweenContexts() {
        Map<String, Object> global = new HashMap<>();
        Map<String, VariableDefinition> defs = Map.of(
            "x", new VariableDefinition("x", VariableType.NUMBER, VariableScope.LOCAL, 0, "")
        );

        EventContextImpl ctx1 = new EventContextImpl(serverProxy(), worldProxy(), global, defs, Map.of());
        ctx1.setVariable("x", 123);

        EventContextImpl ctx2 = new EventContextImpl(serverProxy(), worldProxy(), global, defs, Map.of());
        assertNull(ctx2.getVariable("x"));
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
        UUID uid = UUID.randomUUID();
        return (World) Proxy.newProxyInstance(
            EventContextVariablesTest.class.getClassLoader(),
            new Class<?>[]{World.class},
            (p, method, args) -> {
                if (method.getName().equals("getUID")) {
                    return uid;
                }
                if (method.getName().equals("toString")) {
                    return "WorldProxy(" + uid + ")";
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
            }
        );
    }
}
