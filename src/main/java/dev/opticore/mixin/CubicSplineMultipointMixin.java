package dev.opticore.mixin;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fast-path evaluation for CubicSpline.Multipoint with small control-point arrays (<= 16),
 * avoiding Mth.binarySearch lambda/bounds overhead on worldgen worker threads.
 */
@Mixin(CubicSpline.Multipoint.class)
public abstract class CubicSplineMultipointMixin<C, I extends ToFloatFunction<C>> {
    @Shadow
    @Final
    private float[] locations;

    @Shadow
    @Final
    private List<CubicSpline<C, I>> values;

    @Shadow
    @Final
    private float[] derivatives;

    @Shadow
    public abstract net.minecraft.util.ToFloatFunction<C> coordinate();

    @Inject(method = "apply(Ljava/lang/Object;)F", at = @At("HEAD"), cancellable = true)
    private void opticore$fastApply(C context, CallbackInfoReturnable<Float> cir) {
        if (!OptiCoreConfig.ENABLE_CUBIC_SPLINE_FAST_EVAL.get()) {
            return;
        }
        float[] locs = this.locations;
        int len = locs.length;
        if (len > 16) {
            return;
        }

        OptiCoreMetrics.SPLINE_FAST_EVALS.increment();
        float x = this.coordinate().apply(context);

        if (x < locs[0]) {
            float v0 = this.values.get(0).apply(context);
            cir.setReturnValue(v0 + this.derivatives[0] * (x - locs[0]));
            return;
        }
        int last = len - 1;
        if (x >= locs[last]) {
            float vLast = this.values.get(last).apply(context);
            cir.setReturnValue(vLast + this.derivatives[last] * (x - locs[last]));
            return;
        }

        int i = 0;
        while (i + 1 < len && x >= locs[i + 1]) {
            i++;
        }

        float loc0 = locs[i];
        float loc1 = locs[i + 1];
        float span = loc1 - loc0;
        float t = (x - loc0) / span;
        float v0 = this.values.get(i).apply(context);
        float v1 = this.values.get(i + 1).apply(context);
        float d0 = this.derivatives[i] * span - (v1 - v0);
        float d1 = -this.derivatives[i + 1] * span + (v1 - v0);
        float invT = 1.0F - t;
        float result = invT * v0 + t * v1 + t * invT * (invT * d0 + t * d1);
        cir.setReturnValue(result);
    }
}
