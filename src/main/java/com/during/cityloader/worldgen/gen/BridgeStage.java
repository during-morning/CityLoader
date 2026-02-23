package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;

import java.util.Random;

/**
 * 桥梁生成阶段
 * 在城市间跨越峡谷生成桥梁
 * 兼容LostCities Bridges系统
 */
public class BridgeStage implements GenerationStage {
    private static final int BRIDGE_SCAN_DISTANCE = 8;
    private static final Material BRIDGE_DECK_PRIMARY = Material.POLISHED_BLACKSTONE_BRICKS;
    private static final Material BRIDGE_DECK_VARIANT = Material.POLISHED_BLACKSTONE;
    private static final Material BRIDGE_SUPPORT = Material.BLACKSTONE;
    private static final Material BRIDGE_RAILING = Material.POLISHED_BLACKSTONE_BRICK_WALL;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();

        boolean xBridge = shouldGenerateBridgeAxis(context, info, true);
        boolean zBridge = shouldGenerateBridgeAxis(context, info, false);
        if (!info.isCity && !xBridge && !zBridge) {
            return;
        }
        if (!xBridge && !zBridge) {
            return;
        }

        Random random = context.getRandom();
        
        if (xBridge) {
            int xLevel = resolveBridgeLevel(context, info, true);
            generateBridge(context, xLevel, true, random);
        }
        
        if (zBridge) {
            int zLevel = resolveBridgeLevel(context, info, false);
            generateBridge(context, zLevel, false, random);
        }
    }

    private boolean shouldGenerateBridgeAxis(GenerationContext context, BuildingInfo info, boolean xAxis) {
        if (info == null) {
            return false;
        }
        if (xAxis && info.xBridge) {
            return true;
        }
        if (!xAxis && info.zBridge) {
            return true;
        }
        if (hasBridgeNeighbor(info, xAxis)) {
            return true;
        }
        int nearbyLevel = findNearbyBridgeLevel(context, xAxis, BRIDGE_SCAN_DISTANCE);
        if (nearbyLevel <= 0) {
            return false;
        }
        return needsBridgeSpan(context, nearbyLevel, xAxis);
    }

    private boolean hasBridgeNeighbor(BuildingInfo info, boolean xAxis) {
        if (info == null) {
            return false;
        }
        if (xAxis) {
            BuildingInfo west = info.getXmin();
            BuildingInfo east = info.getXmax();
            return (west != null && west.xBridge) || (east != null && east.xBridge);
        }
        BuildingInfo north = info.getZmin();
        BuildingInfo south = info.getZmax();
        return (north != null && north.zBridge) || (south != null && south.zBridge);
    }

    private int resolveBridgeLevel(GenerationContext context, BuildingInfo info, boolean xAxis) {
        if (info == null) {
            return 0;
        }
        int own = xAxis ? info.highwayXLevel : info.highwayZLevel;
        if (own > 0) {
            return own;
        }
        if (xAxis) {
            BuildingInfo west = info.getXmin();
            BuildingInfo east = info.getXmax();
            if (west != null && west.highwayXLevel > 0) {
                return west.highwayXLevel;
            }
            if (east != null && east.highwayXLevel > 0) {
                return east.highwayXLevel;
            }
            return 0;
        }
        BuildingInfo north = info.getZmin();
        BuildingInfo south = info.getZmax();
        if (north != null && north.highwayZLevel > 0) {
            return north.highwayZLevel;
        }
        if (south != null && south.highwayZLevel > 0) {
            return south.highwayZLevel;
        }
        return findNearbyBridgeLevel(context, xAxis, BRIDGE_SCAN_DISTANCE);
    }

    private int findNearbyBridgeLevel(GenerationContext context, boolean xAxis, int maxDistance) {
        if (context == null || context.getDimensionInfo() == null) {
            return 0;
        }
        IDimensionInfo dimInfo = context.getDimensionInfo();
        String dimension = dimInfo.dimension() != null ? dimInfo.dimension() : context.getWorldInfo().getName();
        for (int distance = 1; distance <= Math.max(1, maxDistance); distance++) {
            int dx = xAxis ? distance : 0;
            int dz = xAxis ? 0 : distance;

            int positive = bridgeLevelAt(dimInfo, dimension, context.getChunkX() + dx, context.getChunkZ() + dz, xAxis);
            if (positive > 0) {
                return positive;
            }
            int negative = bridgeLevelAt(dimInfo, dimension, context.getChunkX() - dx, context.getChunkZ() - dz, xAxis);
            if (negative > 0) {
                return negative;
            }
        }
        return 0;
    }

    private int bridgeLevelAt(IDimensionInfo dimInfo, String dimension, int chunkX, int chunkZ, boolean xAxis) {
        BuildingInfo near = BuildingInfo.getBuildingInfo(new ChunkCoord(dimension, chunkX, chunkZ), dimInfo);
        if (near == null) {
            return 0;
        }
        if (xAxis) {
            if (near.highwayXLevel > 0 && (near.xBridge || near.isCity)) {
                return near.highwayXLevel;
            }
            return 0;
        }
        if (near.highwayZLevel > 0 && (near.zBridge || near.isCity)) {
            return near.highwayZLevel;
        }
        return 0;
    }

    private boolean needsBridgeSpan(GenerationContext context, int bridgeY, boolean xAxis) {
        if (bridgeY <= 0 || context == null || context.getDimensionInfo() == null) {
            return false;
        }
        ChunkHeightmap heightmap = context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ());
        if (heightmap == null) {
            return false;
        }
        int waterLevel = context.getBuildingInfo() == null ? 63 : context.getBuildingInfo().waterLevel;
        int lowColumns = 0;
        int deepColumns = 0;

        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                int h1 = TerrainEmbeddingEngine.terrainHeight(heightmap, x, 7, bridgeY);
                int h2 = TerrainEmbeddingEngine.terrainHeight(heightmap, x, 8, bridgeY);
                int min = Math.min(h1, h2);
                if (min <= waterLevel + 1) {
                    lowColumns++;
                }
                if (bridgeY - min >= 8) {
                    deepColumns++;
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                int h1 = TerrainEmbeddingEngine.terrainHeight(heightmap, 7, z, bridgeY);
                int h2 = TerrainEmbeddingEngine.terrainHeight(heightmap, 8, z, bridgeY);
                int min = Math.min(h1, h2);
                if (min <= waterLevel + 1) {
                    lowColumns++;
                }
                if (bridgeY - min >= 8) {
                    deepColumns++;
                }
            }
        }
        return lowColumns >= 3 || deepColumns >= 4;
    }

    private void generateBridge(GenerationContext context, int baseY, boolean xAxis, Random random) {
        if (baseY <= 0) {
            return;
        }

        int bridgeY = baseY;
        clearBridgeCorridor(context, bridgeY, xAxis);
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                for (int z = 5; z <= 10; z++) {
                    context.setBlock(x, bridgeY, z, pickDeckMaterial(x, z));
                    context.setBlock(x, bridgeY - 1, z, BRIDGE_DECK_PRIMARY);
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                for (int x = 5; x <= 10; x++) {
                    context.setBlock(x, bridgeY, z, pickDeckMaterial(x, z));
                    context.setBlock(x, bridgeY - 1, z, BRIDGE_DECK_PRIMARY);
                }
            }
        }
        generateBridgeRailings(context, bridgeY, xAxis, random);
        generateBridgeSupports(context, bridgeY, xAxis, random);
    }

    private Material pickDeckMaterial(int x, int z) {
        int roll = Math.floorMod((x * 31) ^ (z * 17), 100);
        return roll < 78 ? BRIDGE_DECK_PRIMARY : BRIDGE_DECK_VARIANT;
    }

    private boolean isAirOrFluid(Material material) {
        if (material == null) {
            return true;
        }
        return material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.WATER
                || material == Material.LAVA
                || material == Material.BUBBLE_COLUMN;
    }

    private void generateBridgeSupports(GenerationContext context, int bridgeY, boolean xAxis, Random random) {
        int supportInterval = 3;
        
        if (xAxis) {
            for (int x = 0; x < 16; x += supportInterval) {
                for (int z = 6; z <= 9; z++) {
                    for (int y = bridgeY - 1; y >= context.getWorldInfo().getMinHeight(); y--) {
                        if (isAirOrFluid(context.getBlockType(x, y, z))) {
                            context.setBlock(x, y, z, BRIDGE_SUPPORT);
                        } else {
                            break;
                        }
                    }
                }
            }
        } else {
            for (int z = 0; z < 16; z += supportInterval) {
                for (int x = 6; x <= 9; x++) {
                    for (int y = bridgeY - 1; y >= context.getWorldInfo().getMinHeight(); y--) {
                        if (isAirOrFluid(context.getBlockType(x, y, z))) {
                            context.setBlock(x, y, z, BRIDGE_SUPPORT);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void clearBridgeCorridor(GenerationContext context, int bridgeY, boolean xAxis) {
        int minY = Math.max(context.getWorldInfo().getMinHeight(), bridgeY + 1);
        int maxYBound = context.getWorldInfo().getMaxHeight() - 1;

        ChunkHeightmap heightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ());

        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                for (int z = 5; z <= 10; z++) {
                    int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, bridgeY);
                    int maxY = Math.min(maxYBound, Math.max(bridgeY + 6, terrainHeight + 1));
                    if (minY > maxY) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                for (int x = 5; x <= 10; x++) {
                    int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, bridgeY);
                    int maxY = Math.min(maxYBound, Math.max(bridgeY + 6, terrainHeight + 1));
                    if (minY > maxY) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
            }
        }
    }

    private void generateBridgeRailings(GenerationContext context, int bridgeY, boolean xAxis, Random random) {
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                context.setBlock(x, bridgeY + 1, 5, BRIDGE_RAILING);
                context.setBlock(x, bridgeY + 1, 10, BRIDGE_RAILING);
                
                if (x % 2 == 0) {
                    context.setBlock(x, bridgeY + 2, 5, BRIDGE_RAILING);
                    context.setBlock(x, bridgeY + 2, 10, BRIDGE_RAILING);
                }
            }
            
            context.setBlock(0, bridgeY + 1, 5, Material.GILDED_BLACKSTONE);
            context.setBlock(0, bridgeY + 1, 10, Material.GILDED_BLACKSTONE);
        } else {
            for (int z = 0; z < 16; z++) {
                context.setBlock(5, bridgeY + 1, z, BRIDGE_RAILING);
                context.setBlock(10, bridgeY + 1, z, BRIDGE_RAILING);
                
                if (z % 2 == 0) {
                    context.setBlock(5, bridgeY + 2, z, BRIDGE_RAILING);
                    context.setBlock(10, bridgeY + 2, z, BRIDGE_RAILING);
                }
            }
            
            context.setBlock(5, bridgeY + 1, 0, Material.GILDED_BLACKSTONE);
            context.setBlock(10, bridgeY + 1, 0, Material.GILDED_BLACKSTONE);
        }
    }
}
