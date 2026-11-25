package dev.opticore.core;

import net.minecraft.core.BlockPos;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Lock-free canonical BlockPos pool for small non-negative structure-local coordinates (0..47).
 * Eliminates millions of duplicate BlockPos objects retained inside StructureTemplate palettes.
 */
public final class StructureDeduplicator {
    private static final int DIM = 48;
    private static final AtomicReferenceArray<BlockPos> POS_POOL = new AtomicReferenceArray<>(DIM * DIM * DIM);

    private StructureDeduplicator() {}

    public static BlockPos internLocalPos(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= DIM || y < 0 || y >= DIM || z < 0 || z >= DIM) {
            return pos;
        }
        int idx = (y * DIM + z) * DIM + x;
        BlockPos existing = POS_POOL.get(idx);
        if (existing != null) {
            OptiCoreMetrics.STRUCTURE_POS_DEDUPED.increment();
            return existing;
        }
        BlockPos immutable = pos.immutable();
        if (POS_POOL.compareAndSet(idx, null, immutable)) {
            return immutable;
        }
        BlockPos winner = POS_POOL.get(idx);
        if (winner != null) {
            OptiCoreMetrics.STRUCTURE_POS_DEDUPED.increment();
            return winner;
        }
        return immutable;
    }
}
