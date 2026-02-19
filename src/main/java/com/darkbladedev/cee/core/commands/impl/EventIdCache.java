package com.darkbladedev.cee.core.commands.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class EventIdCache {
    private final Set<String> values = new HashSet<>();

    public synchronized void refresh(Collection<String> ids) {
        values.clear();
        values.addAll(ids);
    }

    public synchronized Set<String> get() {
        return new HashSet<>(values);
    }
}
