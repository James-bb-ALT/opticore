package dev.opticore.mixin.server;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.AdaptiveBudgetState;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles distant, non-boss, untargeted mob AI steps only when server MSPT exceeds the configured threshold.
 */
@Mixin(Mob.class)
public abstract class MobAiThrottleMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void opticore$throttleDistantAi(CallbackInfo ci) {
        int interval = AdaptiveBudgetState.getAiThrottleInterval();
        if (interval <= 1) {
            return;
        }
        Mob self = (Mob) (Object) this;
        if ((self.tickCount % interval) == 0) {
            return;
        }
        if (self.isNoAi() || self.getTarget() != null || self.isPersistenceRequired() || self.isVehicle()) {
            return;
        }
        if (self.getType().getCategory() == MobCategory.MISC) {
            return;
        }
        double dist = OptiCoreConfig.DISTANT_MOB_AI_DISTANCE.get();
        if (self.level().getNearestPlayer(self, dist) == null) {
            OptiCoreMetrics.MOB_AI_THROTTLED.increment();
            ci.cancel();
        }
    }
}
