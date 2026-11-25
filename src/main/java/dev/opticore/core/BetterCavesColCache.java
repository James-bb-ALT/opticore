package dev.opticore.core;

import java.util.Arrays;

/**
 * Per-thread direct-mapped column cache for YUNG's Better Caves NoiseGen.
 * Kept in dev.opticore.core (outside the Mixin package) to satisfy Mixin classloader rules.
 */
public final class BetterCavesColCache {
    public static final int SLOTS = 256;
    private static final int MASK = SLOTS - 1;

    public final int[] xs = new int[SLOTS];
    public final int[] zs = new int[SLOTS];
    public final int[] minYs = new int[SLOTS];
    public final int[] maxYs = new int[SLOTS];
    public final double[][][] cols = new double[SLOTS][][];

    public BetterCavesColCache() {
        Arrays.fill(xs, Integer.MIN_VALUE);
    }

    public static int slot(int x, int z) {
        int h = (x * 0x9E3779B9) ^ (z * 0x85EBCA6B);
        return (h ^ (h >>> 16)) & MASK;
    }
}
