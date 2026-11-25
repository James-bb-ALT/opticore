package dev.opticore.mixin.client;

import dev.opticore.OptiCoreMod;
import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Releases ModelBakery's temporary unbaked/baked caches once ModelManager finishes applying
 * the baked registry, freeing ~100-200 MB of retained heap on heavy 400+ modpacks.
 */
@Mixin(ModelManager.class)
public abstract class ModelManagerCleanupMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void opticore$cleanupBakeryCaches(@Coerce Object reloadState, ProfilerFiller profiler, CallbackInfo ci) {
        if (!OptiCoreConfig.ENABLE_POST_BAKE_CLEANUP.get() || !(reloadState instanceof ModelReloadStateAccessor stateAcc)) {
            return;
        }
        ModelBakery bakery = stateAcc.opticore$getModelBakery();
        if (!(bakery instanceof ModelBakeryAccessor accessor)) {
            return;
        }
        int cleared = 0;
        try {
            if (accessor.opticore$getBakedCache() != null) {
                cleared += accessor.opticore$getBakedCache().size();
                accessor.opticore$getBakedCache().clear();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            if (accessor.opticore$getUnbakedCache() != null) {
                cleared += accessor.opticore$getUnbakedCache().size();
                accessor.opticore$getUnbakedCache().clear();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            if (accessor.opticore$getTopLevelModels() != null) {
                cleared += accessor.opticore$getTopLevelModels().size();
                accessor.opticore$getTopLevelModels().clear();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        if (cleared > 0) {
            OptiCoreMetrics.MODELS_CACHE_CLEARED.incrementAndGet();
            OptiCoreMod.LOGGER.info("[OptiCore] Post-bake cleanup released {} cached model entries.", cleared);
        }
    }
}
