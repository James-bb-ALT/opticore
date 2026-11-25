package dev.opticore.mixin.server;

import dev.opticore.OptiCoreMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents IllegalStateException ("Chunk not there when requested: UNLOADED") during
 * initial spawn lookup when async chunk systems (C2ME/Ksyxis) have not pinned the chunk.
 */
@Mixin(PlayerRespawnLogic.class)
public abstract class PlayerRespawnLogicMixin {
    @Inject(method = "getOverworldRespawnPos", at = @At("HEAD"), cancellable = true)
    private static void opticore$guardUnloadedSpawnChunk(
            ServerLevel level, int x, int z, CallbackInfoReturnable<BlockPos> cir
    ) {
        int cx = SectionPos.blockToSectionCoord(x);
        int cz = SectionPos.blockToSectionCoord(z);
        if (level.getChunkSource().getChunkNow(cx, cz) != null) {
            return;
        }
        try {
            if (level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, true) == null) {
                cir.setReturnValue(null);
            }
        } catch (IllegalStateException ex) {
            OptiCoreMod.LOGGER.warn("[OptiCore] Guarded unloaded spawn chunk ({}, {}): {}", cx, cz, ex.getMessage());
            cir.setReturnValue(null);
        }
    }
}
