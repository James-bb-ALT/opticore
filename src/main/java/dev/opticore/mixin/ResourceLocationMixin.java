package dev.opticore.mixin;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.StringInternPool;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Canonicalizes ResourceLocation namespace and path strings via a bounded intern pool.
 */
@Mixin(ResourceLocation.class)
public abstract class ResourceLocationMixin {
    @Mutable
    @Shadow
    @Final
    private String namespace;

    @Mutable
    @Shadow
    @Final
    private String path;

    @Inject(
            method = "<init>(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation$Dummy;)V",
            at = @At("RETURN")
    )
    private void opticore$internStrings(String ns, String p, @Coerce Object dummy, CallbackInfo ci) {
        if (OptiCoreConfig.ENABLE_RESOURCE_LOCATION_INTERNING.get()) {
            this.namespace = StringInternPool.intern(this.namespace);
            this.path = StringInternPool.intern(this.path);
        }
    }
}
