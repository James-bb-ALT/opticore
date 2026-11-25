package dev.opticore.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded canonical string pool for ResourceLocation namespaces and paths.
 * Avoids unbounded memory growth while eliminating duplicate short-lived Strings across 400+ mods.
 */
public final class StringInternPool {
    private static final int MAX_ENTRIES = 65536;
    private static final ConcurrentHashMap<String, String> POOL = new ConcurrentHashMap<>(8192);
    private static final AtomicInteger SIZE_APPROX = new AtomicInteger(0);

    private StringInternPool() {}

    public static String intern(String value) {
        if (value == null || value.isEmpty() || value.length() > 128) {
            return value;
        }
        String existing = POOL.get(value);
        if (existing != null) {
            OptiCoreMetrics.INTERN_HITS.increment();
            return existing;
        }
        OptiCoreMetrics.INTERN_MISSES.increment();
        if (SIZE_APPROX.get() >= MAX_ENTRIES) {
            return value;
        }
        String prev = POOL.putIfAbsent(value, value);
        if (prev == null) {
            SIZE_APPROX.incrementAndGet();
            return value;
        }
        return prev;
    }

    public static void trimIfPressured() {
        if (SIZE_APPROX.get() > MAX_ENTRIES / 2) {
            POOL.clear();
            SIZE_APPROX.set(0);
        }
    }
}
