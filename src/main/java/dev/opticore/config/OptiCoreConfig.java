package dev.opticore.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration specification for OptiCore's CPU, RAM, and thread-scheduling optimizations.
 */
public final class OptiCoreConfig {
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_POST_BAKE_CLEANUP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESOURCE_LOCATION_INTERNING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STRUCTURE_DEDUP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TERRABLENDER_CACHE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HYBRID_AQUATIC_FAST_PATH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUBIC_SPLINE_FAST_EVAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BETTER_CAVES_COLUMN_CACHE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_TICK_BUDGETER;
    public static final ForgeConfigSpec.DoubleValue MSPT_THROTTLE_THRESHOLD_MS;
    public static final ForgeConfigSpec.DoubleValue DISTANT_MOB_AI_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_PARTICLE_BUDGET;
    public static final ForgeConfigSpec.DoubleValue FRAME_BUDGET_MS;
    public static final ForgeConfigSpec.IntValue MAX_PARTICLES_UNDER_PRESSURE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BIOME_BLEND_FAST_PATH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_MEMORY_GUARD;
    public static final ForgeConfigSpec.DoubleValue HEAP_PRESSURE_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_THREAD_PRIORITY_HYGIENE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("memory");
        ENABLE_POST_BAKE_CLEANUP = b
                .comment("Clear ModelBakery intermediate caches after ModelManager finishes baking to free ~100-200 MB of heap.")
                .define("enablePostBakeModelCleanup", true);
        ENABLE_RESOURCE_LOCATION_INTERNING = b
                .comment("Intern ResourceLocation namespace/path strings in a bounded pool to cut duplicate String heap.")
                .define("enableResourceLocationInterning", true);
        ENABLE_STRUCTURE_DEDUP = b
                .comment("Deduplicate BlockPos instances inside StructureTemplate palettes to save ~60-120 MB of Old-Gen heap.")
                .define("enableStructureDeduplication", true);
        ENABLE_ADAPTIVE_MEMORY_GUARD = b
                .comment("Monitor heap pressure periodically and softly trim internal caches before Full GC pauses occur.")
                .define("enableAdaptiveMemoryGuard", true);
        HEAP_PRESSURE_THRESHOLD = b
                .comment("Heap usage fraction (0.50 - 0.95) at which OptiCore trims internal caches.")
                .defineInRange("heapPressureThreshold", 0.86, 0.50, 0.95);
        b.pop();

        b.push("worldgen");
        ENABLE_TERRABLENDER_CACHE = b
                .comment("Use a lock-free thread-local direct-mapped cache in front of TerraBlender's StampedLock Area.get().")
                .define("enableTerraBlenderThreadLocalCache", true);
        ENABLE_HYBRID_AQUATIC_FAST_PATH = b
                .comment("Cache non-replaced biomes in Hybrid Aquatic's HABiomePlacement to avoid Kotlin MapBuilder lookups.")
                .define("enableHybridAquaticFastPlacement", true);
        ENABLE_CUBIC_SPLINE_FAST_EVAL = b
                .comment("Use fast branchless/unrolled interval lookup for small CubicSpline.Multipoint evaluations.")
                .define("enableCubicSplineFastEval", true);
        ENABLE_BETTER_CAVES_COLUMN_CACHE = b
                .comment("Cache YUNG's Better Caves noise columns per worker thread to eliminate redundant OpenSimplex2S evaluations and 4D array allocations.")
                .define("enableBetterCavesColumnCache", true);
        b.pop();

        b.push("cpu_and_scheduling");
        ENABLE_ADAPTIVE_TICK_BUDGETER = b
                .comment("Dynamically throttle distant mob AI ticks when server MSPT approaches the 50ms cliff.")
                .define("enableAdaptiveTickBudgeter", true);
        MSPT_THROTTLE_THRESHOLD_MS = b
                .comment("MSPT threshold (ms) above which distant non-boss mob AI is throttled.")
                .defineInRange("msptThrottleThresholdMs", 38.0, 20.0, 50.0);
        DISTANT_MOB_AI_DISTANCE = b
                .comment("Distance (blocks) from nearest player beyond which mob AI can be throttled under load.")
                .defineInRange("distantMobAiDistance", 48.0, 24.0, 128.0);
        ENABLE_ADAPTIVE_PARTICLE_BUDGET = b
                .comment("Cap excess distant/ambient particle spawns per frame when client frame time exceeds budget.")
                .define("enableAdaptiveParticleBudget", true);
        FRAME_BUDGET_MS = b
                .comment("Target client frame time (ms) before excess distant particles are throttled.")
                .defineInRange("frameBudgetMs", 16.5, 8.0, 50.0);
        MAX_PARTICLES_UNDER_PRESSURE = b
                .comment("Maximum new particles accepted per frame when frame time is above budget.")
                .defineInRange("maxParticlesUnderPressure", 48, 8, 512);
        ENABLE_BIOME_BLEND_FAST_PATH = b
                .comment("Fast-path uniform-biome chunk regions during ClientLevel.getBlockTint to avoid O(R^2) lookups.")
                .define("enableBiomeBlendFastPath", true);
        ENABLE_THREAD_PRIORITY_HYGIENE = b
                .comment("Ensure Server thread stays prioritized over background worldgen/LOD workers.")
                .define("enableThreadPriorityHygiene", true);
        b.pop();

        COMMON_SPEC = b.build();
    }

    private OptiCoreConfig() {}
}
