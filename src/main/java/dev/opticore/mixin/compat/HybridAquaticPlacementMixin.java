package dev.opticore.mixin.compat;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fast-paths Hybrid Aquatic's HABiomePlacement$Placement.place() for biomes that have no
 * replacement or sub-biome rules, bypassing Kotlin MapBuilder.get() + Intrinsics.areEqual().
 */
@Pseudo
@Mixin(targets = "dev.hybridlabs.aquatic.world.gen.biome.HABiomePlacement$Placement", remap = false)
public abstract class HybridAquaticPlacementMixin {
    @Shadow
    private Map<Holder<Biome>, ?> replacements;

    @Shadow
    private Map<Holder<Biome>, ?> subBiomes;

    @Unique
    private Set<Holder<Biome>> opticore$handledBiomes;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void opticore$indexHandledBiomes(CallbackInfo ci) {
        HashSet<Holder<Biome>> set = new HashSet<>();
        if (this.replacements != null) {
            set.addAll(this.replacements.keySet());
        }
        if (this.subBiomes != null) {
            set.addAll(this.subBiomes.keySet());
        }
        this.opticore$handledBiomes = set;
    }

    @Inject(
            method = "place(Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void opticore$fastSkipUnhandled(
            Holder<Biome> biome,
            int x,
            int y,
            int z,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        if (!OptiCoreConfig.ENABLE_HYBRID_AQUATIC_FAST_PATH.get() || this.opticore$handledBiomes == null) {
            return;
        }
        OptiCoreMetrics.HA_TOTAL_CALLS.increment();
        if (!this.opticore$handledBiomes.contains(biome)) {
            OptiCoreMetrics.HA_FAST_SKIPS.increment();
            cir.setReturnValue(biome);
        }
    }
}
