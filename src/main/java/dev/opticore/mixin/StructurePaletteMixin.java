package dev.opticore.mixin;

import dev.opticore.config.OptiCoreConfig;
import dev.opticore.core.StructureDeduplicator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Deduplicates local BlockPos instances inside StructureTemplate.Palette upon construction.
 */
@Mixin(StructureTemplate.Palette.class)
public abstract class StructurePaletteMixin {
    @ModifyVariable(method = "<init>(Ljava/util/List;)V", at = @At("HEAD"), argsOnly = true)
    private static List<StructureTemplate.StructureBlockInfo> opticore$dedupPalette(
            List<StructureTemplate.StructureBlockInfo> blocks
    ) {
        if (!OptiCoreConfig.ENABLE_STRUCTURE_DEDUP.get() || blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        if (blocks instanceof ArrayList<StructureTemplate.StructureBlockInfo> arr) {
            for (int i = 0, n = arr.size(); i < n; i++) {
                StructureTemplate.StructureBlockInfo info = arr.get(i);
                BlockPos orig = info.pos();
                BlockPos canon = StructureDeduplicator.internLocalPos(orig);
                if (canon != orig) {
                    arr.set(i, new StructureTemplate.StructureBlockInfo(canon, info.state(), info.nbt()));
                }
            }
            arr.trimToSize();
            return arr;
        }
        return blocks;
    }
}
