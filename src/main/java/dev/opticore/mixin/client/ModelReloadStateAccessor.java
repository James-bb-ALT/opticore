package dev.opticore.mixin.client;

import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for package-private ModelManager.ReloadState#modelBakery.
 */
@Mixin(targets = "net.minecraft.client.resources.model.ModelManager$ReloadState")
public interface ModelReloadStateAccessor {
    @Accessor("modelBakery")
    ModelBakery opticore$getModelBakery();
}
