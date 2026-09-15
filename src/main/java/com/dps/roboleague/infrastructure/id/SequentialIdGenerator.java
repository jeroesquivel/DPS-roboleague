package com.dps.roboleague.infrastructure.id;

import com.dps.roboleague.application.port.out.IdGenerator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialIdGenerator implements IdGenerator {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public String nextId(String prefix) {
        int next = counters.computeIfAbsent(prefix, key -> new AtomicInteger()).incrementAndGet();
        return prefix + "-" + next;
    }
}
