package dev.opticore.mixin.compat;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.BetterCavesColCache;
import dev.opticore.core.OptiCoreMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caches YUNG's Better Caves corner noise columns per worker thread so shared sub-cube
 * corners across a chunk are not recomputed 2x-4x via OpenSimplex2S.noise3_BCC.
 */
@Pseudo
@Mixin(targets = "com.yungnickyoung.minecraft.bettercaves.noise.NoiseGen", remap = false)
public abstract class BetterCavesNoiseGenMixin {
    @Unique
    private ThreadLocal<BetterCavesColCache> opticore$tlColCache;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void opticore$initColCache(CallbackInfo ci) {
        this.opticore$tlColCache = ThreadLocal.withInitial(BetterCavesColCache::new);
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
        BetterCavesColCache c = this.opticore$tlColCache.get();
        int slot = BetterCavesColCache.slot(x, z);
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
        BetterCavesColCache c = this.opticore$tlColCache.get();
        int slot = BetterCavesColCache.slot(x, z);
        c.xs[slot] = x;
        c.zs[slot] = z;
        c.minYs[slot] = minY;
        c.maxYs[slot] = maxY;
        c.cols[slot] = res;
    }
}
