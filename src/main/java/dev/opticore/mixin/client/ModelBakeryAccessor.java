package dev.opticore.mixin.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor for ModelBakery intermediate caches so OptiCore can release them after baking completes.
 */
@Mixin(ModelBakery.class)
public interface ModelBakeryAccessor {
    @Accessor("unbakedCache")
    Map<ResourceLocation, UnbakedModel> opticore$getUnbakedCache();

    @Accessor("topLevelModels")
    Map<ResourceLocation, UnbakedModel> opticore$getTopLevelModels();

    @Accessor("bakedCache")
    Map<?, ?> opticore$getBakedCache();
}
