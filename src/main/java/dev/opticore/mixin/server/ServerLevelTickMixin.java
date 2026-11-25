package dev.opticore.mixin.server;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.AdaptiveBudgetState;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Measures Overworld ServerLevel tick duration and throttles completely empty, unforced
 * non-Overworld dimensions to 5 TPS (1 out of 4 ticks) after initial world startup.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelTickMixin {
    @Unique
    private long opticore$tickStartNs;

    @Unique
    private long opticore$dimTickCount;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void opticore$onTickStart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.dimension() == Level.OVERWORLD) {
            this.opticore$tickStartNs = System.nanoTime();
            return;
        }
        long c = ++this.opticore$dimTickCount;
        if (OptiCoreConfig.ENABLE_ADAPTIVE_TICK_BUDGETER.get()
                && c > 200L
                && (c % 4L) != 0L
                && self.players().isEmpty()
                && self.getForcedChunks().isEmpty()) {
            OptiCoreMetrics.IDLE_DIM_TICKS_SKIPPED.increment();
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void opticore$onTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.dimension() == Level.OVERWORLD && this.opticore$tickStartNs != 0L) {
            AdaptiveBudgetState.recordServerTickNanos(System.nanoTime() - this.opticore$tickStartNs);
            this.opticore$tickStartNs = 0L;
        }
    }
}
