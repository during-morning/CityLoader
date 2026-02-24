package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.ChunkHeightmap;
import org.bukkit.Material;

/**
 * 统一地形嵌合工具，集中处理清挖/填充逻辑。
 */
public final class TerrainEmbeddingEngine {

    private TerrainEmbeddingEngine() {
    }

    public static int terrainHeight(ChunkHeightmap heightmap, int x, int z, int fallback) {
        if (heightmap == null) {
            return fallback;
        }
        if (x < 0 || x >= 16 || z < 0 || z >= 16) {
            return fallback;
        }
        return heightmap.getHeight(x, z);
    }

    public static void embedSurfaceColumn(GenerationContext context,
                                          int x,
                                          int z,
                                          int terrainHeight,
                                          int targetY,
                                          Material fillMaterial) {
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;

        if (terrainHeight > targetY) {
            int from = Math.max(targetY + 1, minY);
            int to = Math.min(terrainHeight, maxY);
            for (int y = from; y <= to; y++) {
                context.setBlock(x, y, z, Material.AIR);
            }
            return;
        }

        if (terrainHeight < targetY) {
            int from = Math.max(terrainHeight + 1, minY);
            int to = Math.min(targetY, maxY);
            for (int y = from; y <= to; y++) {
                context.setBlock(x, y, z, fillMaterial);
            }
        }
    }

    /**
     * 受限地形修正：最多只做有限高度的抬升/挖低，避免形成大断层。
     */
    public static void embedSurfaceColumnLimited(GenerationContext context,
                                                 int x,
                                                 int z,
                                                 int terrainHeight,
                                                 int targetY,
                                                 Material fillMaterial,
                                                 int maxRaise,
                                                 int maxLower) {
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int safeRaise = Math.max(0, maxRaise);
        int safeLower = Math.max(0, maxLower);

        if (terrainHeight > targetY) {
            if (safeLower <= 0) {
                return;
            }
            int from = Math.max(targetY + 1, minY);
            int to = Math.min(terrainHeight, targetY + safeLower);
            to = Math.min(to, maxY);
            for (int y = from; y <= to; y++) {
                context.setBlock(x, y, z, Material.AIR);
            }
            return;
        }

        if (terrainHeight < targetY) {
            if (safeRaise <= 0) {
                return;
            }
            int from = Math.max(terrainHeight + 1, minY);
            int to = Math.min(targetY, terrainHeight + safeRaise);
            to = Math.min(to, maxY);
            for (int y = from; y <= to; y++) {
                context.setBlock(x, y, z, fillMaterial);
            }
        }
    }

    /**
     * 对称受限地形修正：抬升与挖低均按上限执行，并确保目标地坪不会被挖空。
     */
    public static void embedSurfaceColumnSymmetricLimited(GenerationContext context,
                                                          int x,
                                                          int z,
                                                          int terrainHeight,
                                                          int targetY,
                                                          Material fillMaterial,
                                                          int maxRaise,
                                                          int maxLower) {
        embedSurfaceColumnLimited(context, x, z, terrainHeight, targetY, fillMaterial, maxRaise, maxLower);
        Material surface = context.getBlockType(x, targetY, z);
        if (isAirLike(surface) || surface == Material.WATER || surface == Material.LAVA || surface == Material.BUBBLE_COLUMN) {
            context.setBlock(x, targetY, z, fillMaterial == null ? Material.STONE : fillMaterial);
        }
    }

    private static boolean isAirLike(Material material) {
        return material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    public static void supportColumnToTerrain(GenerationContext context,
                                              int x,
                                              int z,
                                              int supportTopY,
                                              int terrainHeight,
                                              Material support) {
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int start = Math.min(supportTopY, maxY);
        int end = Math.max(terrainHeight, minY);

        for (int y = start; y >= end; y--) {
            context.setBlock(x, y, z, support);
        }
    }
}
