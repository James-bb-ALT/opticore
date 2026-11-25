package dev.opticore.mixin.client;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.OptiCoreMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fast-paths ClientLevel.calculateBlockTint in uniform-biome neighborhoods by sampling
 * corners and midpoints before falling back to the full (2R+1)^2 biome blend loop.
 */
@Mixin(ClientLevel.class)
public abstract class BiomeBlendCacheMixin {
    @Inject(method = "calculateBlockTint", at = @At("HEAD"), cancellable = true)
    private void opticore$fastUniformTint(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> cir) {
        if (!OptiCoreConfig.ENABLE_BIOME_BLEND_FAST_PATH.get()) {
            return;
        }
        int r = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (r < 2 || r > 7) {
            return;
        }
        ClientLevel self = (ClientLevel) (Object) this;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Holder<Biome> center = self.getBiome(pos);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        if (self.getBiome(cursor.set(x - r, y, z - r)) != center) return;
        if (self.getBiome(cursor.set(x + r, y, z - r)) != center) return;
        if (self.getBiome(cursor.set(x - r, y, z + r)) != center) return;
        if (self.getBiome(cursor.set(x + r, y, z + r)) != center) return;
        if (self.getBiome(cursor.set(x - r, y, z)) != center) return;
        if (self.getBiome(cursor.set(x + r, y, z)) != center) return;
        if (self.getBiome(cursor.set(x, y, z - r)) != center) return;
        if (self.getBiome(cursor.set(x, y, z + r)) != center) return;

        OptiCoreMetrics.BIOME_BLEND_FAST_HITS.increment();
        cir.setReturnValue(resolver.getColor(center.value(), x, z));
    }
}
