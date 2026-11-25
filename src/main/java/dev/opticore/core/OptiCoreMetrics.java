package dev.opticore.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lock-free runtime telemetry counters for OptiCore subsystems.
 */
public final class OptiCoreMetrics {
    public static final LongAdder INTERN_HITS = new LongAdder();
    public static final LongAdder INTERN_MISSES = new LongAdder();
    public static final LongAdder TB_CACHE_HITS = new LongAdder();
    public static final LongAdder TB_CACHE_MISSES = new LongAdder();
    public static final LongAdder HA_FAST_SKIPS = new LongAdder();
    public static final LongAdder HA_TOTAL_CALLS = new LongAdder();
    public static final LongAdder SPLINE_FAST_EVALS = new LongAdder();
    public static final AtomicInteger MODELS_CACHE_CLEARED = new AtomicInteger();
    public static final LongAdder MOB_AI_THROTTLED = new LongAdder();
    public static final LongAdder IDLE_DIM_TICKS_SKIPPED = new LongAdder();
    public static final LongAdder PARTICLES_THROTTLED = new LongAdder();
    public static final LongAdder BIOME_BLEND_FAST_HITS = new LongAdder();
    public static final LongAdder STRUCTURE_POS_DEDUPED = new LongAdder();
    public static final LongAdder BETTER_CAVES_COL_HITS = new LongAdder();
    public static final LongAdder BETTER_CAVES_COL_MISSES = new LongAdder();
    public static final AtomicInteger MEMORY_TRIMS = new AtomicInteger();

    private OptiCoreMetrics() {}

    public static String formatSummary() {
        long internTotal = INTERN_HITS.sum() + INTERN_MISSES.sum();
        double internPct = internTotal > 0 ? (100.0 * INTERN_HITS.sum() / internTotal) : 0.0;

        long tbTotal = TB_CACHE_HITS.sum() + TB_CACHE_MISSES.sum();
        double tbPct = tbTotal > 0 ? (100.0 * TB_CACHE_HITS.sum() / tbTotal) : 0.0;

        long haTotal = HA_TOTAL_CALLS.sum();
        double haPct = haTotal > 0 ? (100.0 * HA_FAST_SKIPS.sum() / haTotal) : 0.0;

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) >> 20;
        long maxMb = rt.maxMemory() >> 20;

        return String.format(
                "[OptiCore] Heap=%d/%d MB | MSPT_EWMA=%.2f ms (PeakEWMA=%.2f ms, Max=%.1f ms, Spikes>50ms=%d, AI_step=%dx) | "
                        + "Intern=%.1f%% (%d/%d) | TerraBlenderCache=%.1f%% (%d/%d) | HybridAquaticFastSkip=%.1f%% (%d/%d) | "
                        + "SplineFast=%d | IdleDimSkipped=%d | MobAIThrottled=%d | ParticlesThrottled=%d | "
                        + "StructPosDedup=%d | BetterCavesColHits=%d/%d | PostBakeClears=%d | MemTrims=%d",
                usedMb, maxMb,
                AdaptiveBudgetState.getMsptEwma(),
                AdaptiveBudgetState.getPeakEwmaMs(),
                AdaptiveBudgetState.getMaxTickMs(),
                AdaptiveBudgetState.getSpikeTicks50Ms(),
                AdaptiveBudgetState.getAiThrottleInterval(),
                internPct, INTERN_HITS.sum(), internTotal,
                tbPct, TB_CACHE_HITS.sum(), tbTotal,
                haPct, HA_FAST_SKIPS.sum(), haTotal,
                SPLINE_FAST_EVALS.sum(),
                IDLE_DIM_TICKS_SKIPPED.sum(),
                MOB_AI_THROTTLED.sum(),
                PARTICLES_THROTTLED.sum(),
                STRUCTURE_POS_DEDUPED.sum(),
                BETTER_CAVES_COL_HITS.sum(),
                BETTER_CAVES_COL_HITS.sum() + BETTER_CAVES_COL_MISSES.sum(),
                MODELS_CACHE_CLEARED.get(),
                MEMORY_TRIMS.get()
        );
    }
}
