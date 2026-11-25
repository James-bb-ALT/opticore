package dev.opticore.core;

import dev.opticore.config.OptiCoreConfig;

/**
 * Shared lock-free state for peak-aware server tick budgeting, client particle budgeting, and heap pressure monitoring.
 */
public final class AdaptiveBudgetState {
    private static volatile double msptEwma = 10.0;
    private static volatile double peakEwmaMs = 10.0;
    private static volatile double maxTickMs = 0.0;
    private static volatile long spikeTicks50Ms = 0L;
    private static volatile int aiThrottleInterval = 1;
    private static volatile long serverTickCounter = 0L;

    private static volatile double lastFrameMs = 8.0;
    private static volatile int particlesThisFrame = 0;
    private static volatile long lastParticleResetNanos = System.nanoTime();

    private AdaptiveBudgetState() {}

    public static void recordServerTickNanos(long elapsedNanos) {
        double ms = elapsedNanos * 1.0e-6;
        msptEwma = msptEwma * 0.9 + ms * 0.1;

        // Asymmetric EWMA: fast attack on spikes, slow decay on recovery
        if (ms > peakEwmaMs) {
            peakEwmaMs = peakEwmaMs * 0.4 + ms * 0.6;
        } else {
            peakEwmaMs = peakEwmaMs * 0.96 + ms * 0.04;
        }
        if (ms > maxTickMs) {
            maxTickMs = ms;
        }
        if (ms > 50.0) {
            spikeTicks50Ms++;
        }
        serverTickCounter++;

        double effectiveMspt = Math.max(ms, peakEwmaMs);
        double threshold = OptiCoreConfig.MSPT_THROTTLE_THRESHOLD_MS.get();
        if (!OptiCoreConfig.ENABLE_ADAPTIVE_TICK_BUDGETER.get() || effectiveMspt < threshold) {
            aiThrottleInterval = 1;
        } else if (effectiveMspt < threshold + 7.0) {
            aiThrottleInterval = 2;
        } else if (effectiveMspt < threshold + 15.0) {
            aiThrottleInterval = 3;
        } else {
            aiThrottleInterval = 4;
        }

        if ((serverTickCounter % 200L) == 0L && OptiCoreConfig.ENABLE_ADAPTIVE_MEMORY_GUARD.get()) {
            checkHeapPressure();
        }
    }

    private static void checkHeapPressure() {
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        if (max <= 0L) {
            return;
        }
        long used = rt.totalMemory() - rt.freeMemory();
        double ratio = (double) used / (double) max;
        if (ratio >= OptiCoreConfig.HEAP_PRESSURE_THRESHOLD.get()) {
            StringInternPool.trimIfPressured();
            OptiCoreMetrics.MEMORY_TRIMS.incrementAndGet();
        }
    }

    public static double getMsptEwma() {
        return msptEwma;
    }

    public static double getPeakEwmaMs() {
        return peakEwmaMs;
    }

    public static double getMaxTickMs() {
        return maxTickMs;
    }

    public static long getSpikeTicks50Ms() {
        return spikeTicks50Ms;
    }

    public static int getAiThrottleInterval() {
        return aiThrottleInterval;
    }

    public static long getServerTickCounter() {
        return serverTickCounter;
    }

    /**
     * Called on client particle addition to enforce a per-frame cap when frame times exceed budget.
     */
    public static boolean allowClientParticle() {
        if (!OptiCoreConfig.ENABLE_ADAPTIVE_PARTICLE_BUDGET.get()) {
            return true;
        }
        long now = System.nanoTime();
        long dt = now - lastParticleResetNanos;
        if (dt > 5_000_000L) {
            if (dt < 250_000_000L) {
                lastFrameMs = lastFrameMs * 0.8 + (dt * 1.0e-6) * 0.2;
            }
            particlesThisFrame = 0;
            lastParticleResetNanos = now;
        }
        int count = ++particlesThisFrame;
        if (lastFrameMs > OptiCoreConfig.FRAME_BUDGET_MS.get()
                && count > OptiCoreConfig.MAX_PARTICLES_UNDER_PRESSURE.get()) {
            OptiCoreMetrics.PARTICLES_THROTTLED.increment();
            return false;
        }
        return true;
    }
}
