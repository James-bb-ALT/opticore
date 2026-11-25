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
 * Eliminates StampedLock contention in TerraBlender's Area.get(x, z) across worldgen worker threads
 * using a zero-allocation, thread-local direct-mapped 2048-entry cache.
 */
@Pseudo
@Mixin(targets = "terrablender.worldgen.noise.Area", remap = false)
public abstract class TerraBlenderAreaMixin {
    @Unique
    private static final int OPTICORE_CACHE_SIZE = 2048;
    @Unique
    private static final int OPTICORE_CACHE_MASK = OPTICORE_CACHE_SIZE - 1;

    @Unique
    private ThreadLocal<int[]> opticore$tlXs;

    @Unique
    private ThreadLocal<int[]> opticore$tlZs;

    @Unique
    private ThreadLocal<int[]> opticore$tlVals;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void opticore$initThreadLocals(CallbackInfo ci) {
        this.opticore$tlXs = ThreadLocal.withInitial(() -> {
            int[] a = new int[OPTICORE_CACHE_SIZE];
            Arrays.fill(a, Integer.MIN_VALUE);
            return a;
        });
        this.opticore$tlZs = ThreadLocal.withInitial(() -> new int[OPTICORE_CACHE_SIZE]);
        this.opticore$tlVals = ThreadLocal.withInitial(() -> new int[OPTICORE_CACHE_SIZE]);
    }

    @Unique
    private static int opticore$slot(int x, int z) {
        int h = (x * 0x9E3779B9) ^ (z * 0x85EBCA6B);
        return (h ^ (h >>> 16)) & OPTICORE_CACHE_MASK;
    }

    @Inject(method = "get(II)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void opticore$getCached(int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (!OptiCoreConfig.ENABLE_TERRABLENDER_CACHE.get() || x == Integer.MIN_VALUE || this.opticore$tlXs == null) {
            return;
        }
        int slot = opticore$slot(x, z);
        if (this.opticore$tlXs.get()[slot] == x && this.opticore$tlZs.get()[slot] == z) {
            OptiCoreMetrics.TB_CACHE_HITS.increment();
            cir.setReturnValue(this.opticore$tlVals.get()[slot]);
        } else {
            OptiCoreMetrics.TB_CACHE_MISSES.increment();
        }
    }

    @Inject(method = "get(II)I", at = @At("RETURN"), require = 0)
    private void opticore$storeCached(int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (!OptiCoreConfig.ENABLE_TERRABLENDER_CACHE.get() || x == Integer.MIN_VALUE || this.opticore$tlXs == null) {
            return;
        }
        int slot = opticore$slot(x, z);
        this.opticore$tlXs.get()[slot] = x;
        this.opticore$tlZs.get()[slot] = z;
        this.opticore$tlVals.get()[slot] = cir.getReturnValue();
    }
}
