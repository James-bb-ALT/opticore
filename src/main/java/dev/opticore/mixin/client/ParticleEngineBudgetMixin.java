package dev.opticore.mixin.client;

import dev.opticore.core.AdaptiveBudgetState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces an adaptive per-frame particle budget when client frame times exceed target budget.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineBudgetMixin {
    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void opticore$budgetParticle(Particle particle, CallbackInfo ci) {
        if (!AdaptiveBudgetState.allowClientParticle()) {
            ci.cancel();
        }
    }
}
