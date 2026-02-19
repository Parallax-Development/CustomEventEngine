package com.darkbladedev.cee.core.runtime;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntervalScheduleRegistryTest {
    @Test
    void triggersAtExpectedTicks() {
        IntervalScheduleRegistry registry = new IntervalScheduleRegistry();
        AtomicInteger calls = new AtomicInteger();
        registry.register("e1", 20, 0, calls::incrementAndGet);

        assertEquals(0, calls.get());
        assertEquals(20L, registry.statuses(0).getFirst().remainingTicks());

        registry.tick(19);
        assertEquals(0, calls.get());
        assertEquals(1L, registry.statuses(19).getFirst().remainingTicks());

        registry.tick(20);
        assertEquals(1, calls.get());
        assertEquals(20L, registry.statuses(20).getFirst().remainingTicks());

        registry.tick(39);
        assertEquals(1, calls.get());
        assertEquals(1L, registry.statuses(39).getFirst().remainingTicks());

        registry.tick(40);
        assertEquals(2, calls.get());
    }

    @Test
    void catchesUpWhenTickSkipsOverMultipleIntervals() {
        IntervalScheduleRegistry registry = new IntervalScheduleRegistry();
        AtomicInteger calls = new AtomicInteger();
        registry.register("e1", 10, 0, calls::incrementAndGet);

        registry.tick(35);
        assertEquals(3, calls.get());
        assertEquals(5L, registry.statuses(35).getFirst().remainingTicks());
    }

    @Test
    void unregisterStopsFutureTriggers() {
        IntervalScheduleRegistry registry = new IntervalScheduleRegistry();
        AtomicInteger calls = new AtomicInteger();
        registry.register("e1", 5, 0, calls::incrementAndGet);

        registry.tick(5);
        assertEquals(1, calls.get());

        registry.unregister("e1");
        assertTrue(registry.isEmpty());

        registry.tick(100);
        assertEquals(1, calls.get());
    }

    @Test
    void stopSemanticsUnregistersSchedule() {
        IntervalScheduleRegistry registry = new IntervalScheduleRegistry();
        AtomicInteger calls = new AtomicInteger();
        registry.register("evento_interval", 20, 0, calls::incrementAndGet);

        registry.tick(20);
        assertEquals(1, calls.get());

        registry.unregister("evento_interval");
        registry.tick(40);
        assertEquals(1, calls.get());
    }
}
