package dev.opticore.mixin.compat;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/**
 * Caches YUNG's Better Caves corner noise columns per worker thread so shared sub-cube
 * corners across a chunk are not recomputed 2x-4x via OpenSimplex2S.noise3_BCC.
 */
@Pseudo
@Mixin(targets = "com.yungnickyoung.minecraft.bettercaves.noise.NoiseGen", remap = false)
public abstract class BetterCavesNoiseGenMixin {
    @Unique
    private static final int OPTICORE_BC_SLOTS = 256;
    @Unique
    private static final int OPTICORE_BC_MASK = OPTICORE_BC_SLOTS - 1;

    @Unique
    private ThreadLocal<ColCache> opticore$tlColCache;

    @Unique
    private static final class ColCache {
        final int[] xs = new int[OPTICORE_BC_SLOTS];
        final int[] zs = new int[OPTICORE_BC_SLOTS];
        final int[] minYs = new int[OPTICORE_BC_SLOTS];
        final int[] maxYs = new int[OPTICORE_BC_SLOTS];
        final double[][][] cols = new double[OPTICORE_BC_SLOTS][][];

        ColCache() {
            Arrays.fill(xs, Integer.MIN_VALUE);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void opticore$initColCache(CallbackInfo ci) {
        this.opticore$tlColCache = ThreadLocal.withInitial(ColCache::new);
    }

    @Unique
    private static int opticore$slot(int x, int z) {
        int h = (x * 0x9E3779B9) ^ (z * 0x85EBCA6B);
        return (h ^ (h >>> 16)) & OPTICORE_BC_MASK;
    }

    @Inject(method = "generateNoiseColumn(IIII)[[D", at = @At("HEAD"), cancellable = true, require = 0)
    private void opticore$getCachedColumn(
            int x, int z, int minY, int maxY,
            CallbackInfoReturnable<double[][]> cir
    ) {
        if (!OptiCoreConfig.ENABLE_BETTER_CAVES_COLUMN_CACHE.get()
                || x == Integer.MIN_VALUE
                || this.opticore$tlColCache == null) {
            return;
        }
        ColCache c = this.opticore$tlColCache.get();
        int slot = opticore$slot(x, z);
        if (c.xs[slot] == x && c.zs[slot] == z && c.minYs[slot] == minY && c.maxYs[slot] == maxY && c.cols[slot] != null) {
            OptiCoreMetrics.BETTER_CAVES_COL_HITS.increment();
            cir.setReturnValue(c.cols[slot]);
        } else {
            OptiCoreMetrics.BETTER_CAVES_COL_MISSES.increment();
        }
    }

    @Inject(method = "generateNoiseColumn(IIII)[[D", at = @At("RETURN"), require = 0)
    private void opticore$storeCachedColumn(
            int x, int z, int minY, int maxY,
            CallbackInfoReturnable<double[][]> cir
    ) {
        if (!OptiCoreConfig.ENABLE_BETTER_CAVES_COLUMN_CACHE.get()
                || x == Integer.MIN_VALUE
                || this.opticore$tlColCache == null) {
            return;
        }
        double[][] res = cir.getReturnValue();
        if (res == null) {
            return;
        }
        ColCache c = this.opticore$tlColCache.get();
        int slot = opticore$slot(x, z);
        c.xs[slot] = x;
        c.zs[slot] = z;
        c.minYs[slot] = minY;
        c.maxYs[slot] = maxY;
        c.cols[slot] = res;
    }
}
