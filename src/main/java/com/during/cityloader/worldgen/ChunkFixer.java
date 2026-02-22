package com.during.cityloader.worldgen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.util.Locale;
import java.util.Random;

/**
 * 生成后修复器：处理跨区块边界细节与延迟任务。
 */
public final class ChunkFixer {

    private ChunkFixer() {
    }

    public static void fix(IDimensionInfo info, int chunkX, int chunkZ) {
        if (info == null || info.getWorld() == null) {
            return;
        }
        ChunkCoord coord = new ChunkCoord(info.dimension(), chunkX, chunkZ);
        executePostTodo(coord, info);
        generateBoundaryVines(coord, info);
    }

    private static void executePostTodo(ChunkCoord coord, IDimensionInfo provider) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        for (Runnable task : info.drainPostTodo()) {
            try {
                task.run();
            } catch (Exception ignored) {
            }
        }
    }

    private static void generateBoundaryVines(ChunkCoord coord, IDimensionInfo provider) {
        if (provider.getProfile() == null || provider.getProfile().getVineChance() <= 0.000001f) {
            return;
        }

        World world = provider.getWorld();
        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        if (!world.isChunkLoaded(chunkX + 1, chunkZ) || !world.isChunkLoaded(chunkX, chunkZ + 1)) {
            return;
        }

        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        int thisTop = info.getMaxHeight();
        if (thisTop <= info.getCityGroundLevel() + 3) {
            return;
        }

        Random random = new Random(provider.getSeed() ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L));
        float chance = provider.getProfile().getVineChance();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        BuildingInfo east = info.getXmax();
        int eastBottom = Math.max(east.getCityGroundLevel() + 3, east.hasBuilding ? east.getMaxHeight() : east.getCityGroundLevel() + 3);
        createVinePlane(world, baseX + 15, baseZ, 0, 15, eastBottom, thisTop, BlockFace.EAST, chance, random);

        BuildingInfo south = info.getZmax();
        int southBottom = Math.max(south.getCityGroundLevel() + 3, south.hasBuilding ? south.getMaxHeight() : south.getCityGroundLevel() + 3);
        createVinePlane(world, baseZ + 15, baseX, 0, 15, southBottom, thisTop, BlockFace.SOUTH, chance, random);
    }

    private static void createVinePlane(World world,
                                        int fixed,
                                        int movingBase,
                                        int start,
                                        int endExclusive,
                                        int bottom,
                                        int topExclusive,
                                        BlockFace supportFace,
                                        float chance,
                                        Random random) {
        int minY = Math.max(bottom, world.getMinHeight());
        int maxY = Math.min(topExclusive, world.getMaxHeight());
        if (minY >= maxY) {
            return;
        }
        for (int i = start; i < endExclusive; i++) {
            for (int y = minY; y < maxY; y++) {
                if (random.nextFloat() >= chance) {
                    continue;
                }
                int x = supportFace == BlockFace.SOUTH ? movingBase + i : fixed;
                int z = supportFace == BlockFace.SOUTH ? fixed : movingBase + i;
                createVineStrip(world, x, y, z, minY, supportFace, random);
            }
        }
    }

    private static void createVineStrip(World world, int x, int y, int z, int bottom, BlockFace supportFace, Random random) {
        if (!canPlaceVineAt(world, x, y, z, supportFace)) {
            return;
        }
        if (!setVine(world, x, y, z, supportFace)) {
            return;
        }
        int currentY = y - 1;
        while (currentY >= bottom && random.nextFloat() < 0.8f) {
            if (!canPlaceVineAt(world, x, currentY, z, supportFace) || !setVine(world, x, currentY, z, supportFace)) {
                break;
            }
            currentY--;
        }
    }

    private static boolean canPlaceVineAt(World world, int x, int y, int z, BlockFace supportFace) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return false;
        }
        Block target = world.getBlockAt(x, y, z);
        if (!isAirLike(target.getType())) {
            return false;
        }
        Block support = world.getBlockAt(x + supportFace.getModX(), y, z + supportFace.getModZ());
        return isSolidSupport(support.getType());
    }

    private static boolean setVine(World world, int x, int y, int z, BlockFace supportFace) {
        String key = horizontalFaceKey(supportFace);
        if (key == null) {
            return false;
        }
        try {
            BlockData data = Bukkit.createBlockData("minecraft:vine[" + key + "=true]");
            world.getBlockAt(x, y, z).setBlockData(data, false);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String horizontalFaceKey(BlockFace face) {
        if (face == null) {
            return null;
        }
        return switch (face) {
            case NORTH -> "north";
            case SOUTH -> "south";
            case EAST -> "east";
            case WEST -> "west";
            default -> null;
        };
    }

    private static boolean isAirLike(Material material) {
        return material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private static boolean isSolidSupport(Material material) {
        if (material == null || isAirLike(material) || material == Material.BARRIER) {
            return false;
        }
        try {
            return material.isSolid();
        } catch (Throwable ignored) {
            return !material.name().toLowerCase(Locale.ROOT).contains("air");
        }
    }
}
