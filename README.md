# OptiCore (Forge 1.20.1)

**OptiCore** is a targeted CPU, RAM, GC, and thread-scheduling optimization mod for heavy **Minecraft Forge 1.20.1** modpacks (400+ mods), designed to eliminate real-world bottlenecks without removing or altering any gameplay or visual features.

---

## Architecture & Optimizations

OptiCore targets six specific bottlenecks profiled on large modpacks (`445` active mods) using `jstat`, `jcmd GC.class_histogram`, and `jstack`:

### 1. Memory & Cache Lifecycle
- **Post-Bake Model Cache Eviction** (`ModelManagerCleanupMixin`, `ModelBakeryAccessor`, `ModelReloadStateAccessor`):
  Clears `ModelBakery.bakedCache`, `unbakedCache`, and `topLevelModels` right after `ModelManager.apply()` finishes building the baked registry, releasing ~2.78M temporary entries (`ModelBakery$BakedCacheKey`, `ModelResourceLocation`, `HashMap$Node`).
- **Bounded `ResourceLocation` String Interning** (`ResourceLocationMixin`, `StringInternPool`):
  Deduplicates `namespace` and `path` strings across 400+ mods using a bounded (`65,536`-entry) `ConcurrentHashMap` pool (`~85%` hit rate in runtime telemetry).
- **`StructureTemplate` Local `BlockPos` Deduplication** (`StructurePaletteMixin`, `StructureDeduplicator`):
  Canonicalizes structure-local `BlockPos` coordinates (`0..47`) inside `StructureTemplate.Palette` via a lock-free `AtomicReferenceArray` and compacts backing `ArrayList`s with `trimToSize()`.
- **Adaptive Heap Pressure Guard** (`AdaptiveBudgetState`):
  Monitors heap utilization every 200 server ticks and trims internal pools when usage exceeds `heapPressureThreshold` (default `0.86`).

### 2. Lock-Free Worldgen & Mod-Specific Hot-Path Caches
- **TerraBlender `StampedLock` Bypass** (`TerraBlenderAreaMixin`, `@Pseudo`):
  Adds a zero-allocation `ThreadLocal` direct-mapped 2048-slot cache in front of `terrablender.worldgen.noise.Area.get(x, z)`, eliminating cross-worker `StampedLock` contention (`~78%–85%` hit rate).
- **Hybrid Aquatic Fast-Skip** (`HybridAquaticPlacementMixin`, `@Pseudo`):
  Indexes replaced/sub-biome keys at `HABiomePlacement$Placement` construction time to skip unaffected biomes in O(1) without calling Kotlin `MapBuilder.get()` (`~93%` fast-skip rate).
- **YUNG's Better Caves Column Cache** (`BetterCavesNoiseGenMixin`, `@Pseudo`):
  Adds a `ThreadLocal` 256-slot column cache for `NoiseGen.generateNoiseColumn(x, z, minY, maxY)` so shared sub-cube corners across a chunk are not recomputed 2×–4× via `OpenSimplex2S.noise3_BCC`.
- **Fast `CubicSpline.Multipoint` Evaluation** (`CubicSplineMultipointMixin`):
  Replaces `Mth.binarySearch` lambda overhead with a direct linear interval scan for splines with `<= 16` control points.

### 3. Peak-Aware CPU & Tick Budgeting
- **Asymmetric Peak-Aware MSPT Budgeter + Distant Mob AI Throttler** (`ServerLevelTickMixin`, `MobAiThrottleMixin`, `AdaptiveBudgetState`):
  Tracks both instantaneous tick time and a fast-attack/slow-decay `PeakEWMA`. When effective MSPT exceeds `msptThrottleThresholdMs` (default `38.0 ms`), `Mob.serverAiStep()` is throttled (`2x–4x`) only for distant (`> 48` blocks), idle, untargeted, non-persistent, non-vehicle mobs.
- **Idle-Dimension Tick Gate** (`ServerLevelTickMixin`):
  Throttles completely empty (`players().isEmpty()`), unforced (`getForcedChunks().isEmpty()`) non-Overworld `ServerLevel` instances to 1 out of 4 ticks after startup (`tick > 200`).
- **Adaptive Client Particle Budget** (`ParticleEngineBudgetMixin`):
  Caps excess particle additions per frame (`maxParticlesUnderPressure = 48`) only when client frame time exceeds `frameBudgetMs` (`16.5 ms`).
- **Uniform-Biome `ClientLevel.calculateBlockTint` Fast-Path** (`BiomeBlendCacheMixin`):
  Samples corners and midpoints before falling back to the full `(2R+1)^2` blend loop.
- **Server Thread Priority Hygiene** (`OptiCoreMod`):
  Elevates the `Server thread` priority above background worldgen/LOD workers on server start.

---

## Measured Telemetry (445-Mod Forge 1.20.1 Modpack)

Observed on `Optimized fps` (`12,352 MB` heap) via `jstat`, `jcmd GC.class_histogram`, `jstack`, and `OptiCoreMetrics`:

| Metric | Baseline (No OptiCore, Java 17 G1GC) | With OptiCore (Java 17 G1GC) | With OptiCore + Java 21 Generational ZGC |
|---|---:|---:|---:|
| **Post-bake model entries released** | `0` | `2,779,028` | `2,779,029` |
| **`ModelBakery$BakedCacheKey` retained** | `947,597` (`22.74 MB`) | Evicted from Top 30 | Evicted |
| **`ModelResourceLocation` retained** | `2,678,150` (`85.70 MB`) | `1,801,522` (`57.65 MB`, `-32.7%`) | — |
| **`HashMap$Node` retained** | `7,207,847` (`230.65 MB`) | `5,486,665` (`175.57 MB`, `-23.9%`) | — |
| **TerraBlender `StampedLock` contention** | Blocked worker threads | `0` blocked (`78.8%–84.6%` cache hit) | `0` blocked |
| **Hybrid Aquatic fast-skip** | N/A | `92.8%–93.4%` skipped | Active |
| **`ResourceLocation` intern hit rate** | N/A | `84.9%–85.3%` | Active |
| **Total GC stop-the-world pause** | `4.41s`–`5.92s` (boot) | `10.57s`–`13.47s` (5-min run, 12–13 Full GCs) | **`0.005s` (`5 ms`)** in `282.6s` |

---

## Commands

- `/opticore stats` — Displays live heap usage, MSPT EWMA / PeakEWMA / max tick / spikes >50ms, and hit/skip counters for every subsystem.
- `/opticore trim` *(OP level 2)* — Manually trims OptiCore's bounded intern pool.

---

## Configuration (`config/opticore-common.toml`)

All subsystems are individually toggleable:

- `[memory]`
  - `enablePostBakeModelCleanup` (`true`)
  - `enableResourceLocationInterning` (`true`)
  - `enableStructureDeduplication` (`true`)
  - `enableAdaptiveMemoryGuard` (`true`)
  - `heapPressureThreshold` (`0.86`)
- `[worldgen]`
  - `enableTerraBlenderThreadLocalCache` (`true`)
  - `enableHybridAquaticFastPlacement` (`true`)
  - `enableCubicSplineFastEval` (`true`)
  - `enableBetterCavesColumnCache` (`true`)
- `[cpu_and_scheduling]`
  - `enableAdaptiveTickBudgeter` (`true`)
  - `msptThrottleThresholdMs` (`38.0`)
  - `distantMobAiDistance` (`48.0`)
  - `enableAdaptiveParticleBudget` (`true`)
  - `frameBudgetMs` (`16.5`)
  - `maxParticlesUnderPressure` (`48`)
  - `enableBiomeBlendFastPath` (`true`)
  - `enableThreadPriorityHygiene` (`true`)

---

## Recommended JVM Flags (Java 21+ Generational ZGC)

For heavy 400+ modpacks on Java 21:
```text
-Xms4096M -Xmx12352M -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+PerfDisableSharedMem
```

---

## Building from Source

Requires **JDK 17** (`JAVA_HOME` pointing to JDK 17 for ForgeGradle 6):

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
.\gradlew.bat test build --no-daemon
```

Output JARs are written to `build/libs/opticore-1.0.0-1.20.1.jar`.

## License

MIT License — see [LICENSE](LICENSE).
