package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.City;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedStreet;
import com.during.cityloader.worldgen.lost.regassets.data.StreetSettings;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 核心城市生成阶段（建筑/街道）
 * 包含TerrainFix地形适配功能
 */
public class CityCoreStage implements GenerationStage {
    private static final int STREET_MAX_STEP = 1;
    private static final int STREET_RETAINING_WALL_MIN_DROP = 3;
    private static final int STREET_CENTER_MIN = 4;
    private static final int STREET_CENTER_MAX = 11;
    private static final int STREET_MIN_CARVE_DEPTH = 12;
    private static final int BUILDING_APRON_INSET = 4;
    private static final int BUILDING_APRON_MAX_DROP = 8;
    private static final int BUILDING_MIN_CARVE_DEPTH = 16;
    private static final int BUILDING_RETAINING_WALL_MIN_DROP = 2;
    private static final int BUILDING_SUPPORT_MAX_DEPTH = 24;
    private static final int BUILDING_EDGE_TRANSITION_BAND = 2;
    private static final Set<Material> STREET_OVERHANG_CLEAR_MATERIALS = Set.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.STONE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.DEEPSLATE,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE,
            Material.MOSS_BLOCK,
            Material.MOSS_CARPET
    );

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        if (!info.isCity) {
            return;
        }

        prepareCitySurface(context, info);

        if (info.hasBuilding) {
            generateBuilding(context);
        } else if (info.hasStreet) {
            generateStreet(context);
        } else {
            generateVacantLot(context);
        }
    }

    private void generateBuilding(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        CompiledPalette palette = context.palette();
        
        LostCityProfile profile = getProfile(context);
        boolean terrainFixEnabled = isTerrainFixEnabled(profile);
        // 建筑底层/地下层需要先整体净空，避免被原地形切入
        clearBuildingAirEnvelope(context, info);
        for (int floor = -info.cellars; floor < info.floors; floor++) {
            int floorY = info.getCityGroundLevel() + floor * GenerationHeightModel.FLOOR_HEIGHT;
            boolean applyFoundationFix = terrainFixEnabled && floor == -info.cellars;
            renderPart(context, info.getFloor(floor), palette, floorY, false, info.getFloorTransform(floor), applyFoundationFix, profile);
            renderPart(context, info.getFloorPart2(floor), palette, floorY, true, info.getFloorPart2Transform(floor), false, profile);
        }
        // 所有建筑都补齐基础承重，避免底层/地下层出现“悬空缺块”。
        if (info.hasBuilding) {
            stabilizeBuildingFoundation(context, info);
            sealExposedBuildingEdges(context, info);
        }
    }

    private LostCityProfile getProfile(GenerationContext context) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo != null) {
            return dimInfo.getProfile();
        }
        return null;
    }

    private void clearBuildingAirEnvelope(GenerationContext context, BuildingInfo info) {
        if (info == null || !info.hasBuilding) {
            return;
        }
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int cellars = Math.max(0, info.cellars);
        int extraBelowClear = cellars > 0 ? GenerationHeightModel.FLOOR_HEIGHT : 0;
        int clearFrom = Math.max(minY, info.getCityGroundLevel() - cellars * GenerationHeightModel.FLOOR_HEIGHT - extraBelowClear);
        int clearTo = maxY;
        if (clearFrom > clearTo) {
            return;
        }

        boolean preserveWestEdge = !isBuildingChunk(info.getXmin());
        boolean preserveEastEdge = !isBuildingChunk(info.getXmax());
        boolean preserveNorthEdge = !isBuildingChunk(info.getZmin());
        boolean preserveSouthEdge = !isBuildingChunk(info.getZmax());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean preserveEdgeColumn = (preserveWestEdge && x == 0)
                        || (preserveEastEdge && x == 15)
                        || (preserveNorthEdge && z == 0)
                        || (preserveSouthEdge && z == 15);
                if (preserveEdgeColumn) {
                    continue;
                }
                for (int y = clearFrom; y <= clearTo; y++) {
                    context.setBlock(x, y, z, Material.AIR);
                }
            }
        }
    }

    private void sealExposedBuildingEdges(GenerationContext context, BuildingInfo info) {
        if (info == null || !info.hasBuilding) {
            return;
        }
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int fromY = Math.max(minY, info.getCityGroundLevel() - Math.max(0, info.cellars) * GenerationHeightModel.FLOOR_HEIGHT);
        int toY = Math.min(maxY, info.getMaxHeight() + 2);

        Material edgeFill = Material.STONE_BRICKS;
        if (!isBuildingChunk(info.getXmin())) {
            for (int z = 0; z < 16; z++) {
                sealEdgeColumn(context, 0, z, 1, z, fromY, toY, edgeFill);
            }
        }
        if (!isBuildingChunk(info.getXmax())) {
            for (int z = 0; z < 16; z++) {
                sealEdgeColumn(context, 15, z, 14, z, fromY, toY, edgeFill);
            }
        }
        if (!isBuildingChunk(info.getZmin())) {
            for (int x = 0; x < 16; x++) {
                sealEdgeColumn(context, x, 0, x, 1, fromY, toY, edgeFill);
            }
        }
        if (!isBuildingChunk(info.getZmax())) {
            for (int x = 0; x < 16; x++) {
                sealEdgeColumn(context, x, 15, x, 14, fromY, toY, edgeFill);
            }
        }
    }

    private boolean isBuildingChunk(BuildingInfo info) {
        return info != null && info.isCity && info.hasBuilding;
    }

    private void sealEdgeColumn(GenerationContext context,
                                int edgeX,
                                int edgeZ,
                                int innerX,
                                int innerZ,
                                int fromY,
                                int toY,
                                Material fallback) {
        for (int y = fromY; y <= toY; y++) {
            Material edge = context.getBlockType(edgeX, y, edgeZ);
            if (!isAir(edge)) {
                continue;
            }
            Material inner = context.getBlockType(innerX, y, innerZ);
            if (isAir(inner)) {
                continue;
            }
            Material fill = isReplaceableForFacade(inner) ? fallback : inner;
            context.setBlock(edgeX, y, edgeZ, fill);
        }
    }

    private boolean isAir(Material material) {
        return material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private boolean isReplaceableForFacade(Material material) {
        if (material == null) {
            return true;
        }
        String name = material.name();
        return material == Material.WATER
                || material == Material.LAVA
                || material == Material.TALL_GRASS
                || "GRASS".equals(name)
                || "SHORT_GRASS".equals(name);
    }

    private boolean isSolidSupport(Material material) {
        return material != null
                && material != Material.AIR
                && material != Material.CAVE_AIR
                && material != Material.VOID_AIR
                && material != Material.WATER
                && material != Material.LAVA;
    }

    private boolean isTerrainFixEnabled(LostCityProfile profile) {
        if (profile == null) {
            return false;
        }
        return profile.getTerrainFixLowerMinOffset() != 0 || 
               profile.getTerrainFixLowerMaxOffset() != 0 ||
               profile.getTerrainFixUpperMinOffset() != 0 || 
               profile.getTerrainFixUpperMaxOffset() != 0;
    }

    private void prepareCitySurface(GenerationContext context, BuildingInfo info) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }

        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        LostCityProfile profile = dimInfo.getProfile();
        Material base = profile == null
                ? Material.STONE
                : context.resolveMaterial(profile.getBaseBlock(), Material.STONE);

        int cityGround = info.getCityGroundLevel();
        boolean buildingChunk = info.hasBuilding;
        int[][] smoothedTargets = buildingChunk
                ? buildBuildingSurfaceTargets(context, info, cityGround - 1)
                : buildStreetSurfaceTargets(context, info, cityGround);
        int maxRaise = 4;
        int maxLower = 2;
        if (profile != null) {
            maxRaise = Math.max(1, Math.max(
                    Math.abs(profile.getTerrainFixLowerMinOffset()),
                    Math.abs(profile.getTerrainFixLowerMaxOffset())));
            maxLower = Math.max(1, Math.max(
                    Math.abs(profile.getTerrainFixUpperMinOffset()),
                    Math.abs(profile.getTerrainFixUpperMaxOffset())));
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, cityGround);
                if (buildingChunk) {
                    // LostCities 风格：建筑区仅在边缘做轻微过渡，内部不做大规模切地形。
                    int edgeDistance = Math.min(Math.min(x, 15 - x), Math.min(z, 15 - z));
                    if (edgeDistance <= BUILDING_EDGE_TRANSITION_BAND) {
                        int targetY = smoothedTargets == null ? cityGround - 1 : smoothedTargets[x][z];
                        int carveDepth = Math.max(3, Math.min(BUILDING_APRON_MAX_DROP + 2, maxLower + 2));
                        TerrainEmbeddingEngine.embedSurfaceColumnLimited(
                                context,
                                x,
                                z,
                                terrainHeight,
                                targetY,
                                base,
                                Math.min(2, maxRaise),
                                carveDepth);
                    }
                } else {
                    int targetY = smoothedTargets == null ? cityGround : smoothedTargets[x][z];
                    int carveDepth = Math.max(STREET_MIN_CARVE_DEPTH, maxLower);
                    TerrainEmbeddingEngine.embedSurfaceColumnLimited(
                            context,
                            x,
                            z,
                            terrainHeight,
                            targetY,
                            base,
                            maxRaise,
                            carveDepth);
                }
            }
        }

    }

    /**
     * 类似 LC 的边界过渡：根据邻接区块角点高度做双线性插值，避免街区边缘“台阶断层”。
     */
    private int[][] buildStreetSurfaceTargets(GenerationContext context, BuildingInfo info, int fallbackY) {
        int[][] targets = new int[16][16];
        int west = sideAnchor(context, info, fallbackY, -1, 0, 15, 8);
        int east = sideAnchor(context, info, fallbackY, 1, 0, 0, 8);
        int north = sideAnchor(context, info, fallbackY, 0, -1, 8, 15);
        int south = sideAnchor(context, info, fallbackY, 0, 1, 8, 0);

        int nw = Math.round((west + north) / 2.0f);
        int ne = Math.round((east + north) / 2.0f);
        int sw = Math.round((west + south) / 2.0f);
        int se = Math.round((east + south) / 2.0f);

        for (int x = 0; x < 16; x++) {
            double fx = x / 15.0;
            for (int z = 0; z < 16; z++) {
                double fz = z / 15.0;
                double top = lerp(nw, ne, fx);
                double bottom = lerp(sw, se, fx);
                int y = (int) Math.round(lerp(top, bottom, fz));
                targets[x][z] = y;
            }
        }

        // 中心区域更偏平台，但保留一定插值，减轻城市/自然交界“台阶感”。
        for (int x = STREET_CENTER_MIN; x <= STREET_CENTER_MAX; x++) {
            for (int z = STREET_CENTER_MIN; z <= STREET_CENTER_MAX; z++) {
                targets[x][z] = (int) Math.round(targets[x][z] * 0.3 + fallbackY * 0.7);
            }
        }

        lockStreetEdges(targets, west, east, north, south);
        clampStreetGradientLockedEdges(targets, STREET_MAX_STEP);
        lockStreetEdges(targets, west, east, north, south);
        return targets;
    }

    /**
     * 建筑区边缘裙边：中心保持平台，靠近边界逐步贴近邻区块目标高度，避免整块硬抬升造成断层。
     */
    private int[][] buildBuildingSurfaceTargets(GenerationContext context, BuildingInfo info, int centerY) {
        int[][] targets = new int[16][16];
        int west = buildingSideAnchor(context, info, centerY, -1, 0, 15, 8);
        int east = buildingSideAnchor(context, info, centerY, 1, 0, 0, 8);
        int north = buildingSideAnchor(context, info, centerY, 0, -1, 8, 15);
        int south = buildingSideAnchor(context, info, centerY, 0, 1, 8, 0);

        int nw = Math.round((west + north) / 2.0f);
        int ne = Math.round((east + north) / 2.0f);
        int sw = Math.round((west + south) / 2.0f);
        int se = Math.round((east + south) / 2.0f);

        for (int x = 0; x < 16; x++) {
            double fx = x / 15.0;
            for (int z = 0; z < 16; z++) {
                double fz = z / 15.0;
                double top = lerp(nw, ne, fx);
                double bottom = lerp(sw, se, fx);
                int edgeY = (int) Math.round(lerp(top, bottom, fz));

                int dist = Math.min(Math.min(x, 15 - x), Math.min(z, 15 - z));
                double apronT;
                if (dist >= BUILDING_APRON_INSET) {
                    apronT = 0.0;
                } else {
                    apronT = (BUILDING_APRON_INSET - dist) / (double) BUILDING_APRON_INSET;
                }
                int y = (int) Math.round(lerp(centerY, edgeY, apronT));
                targets[x][z] = Math.max(centerY - BUILDING_APRON_MAX_DROP, Math.min(centerY, y));
            }
        }
        return targets;
    }

    private void lockStreetEdges(int[][] targets, int west, int east, int north, int south) {
        for (int i = 0; i < 16; i++) {
            targets[0][i] = west;
            targets[15][i] = east;
            targets[i][0] = north;
            targets[i][15] = south;
        }
    }

    private void applyBuildingRetainingSkirt(GenerationContext context, int[][] targets, Material wall) {
        if (targets == null) {
            return;
        }
        if (wall == null || wall == Material.AIR) {
            wall = Material.STONE_BRICKS;
        }
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }

        ChunkHeightmap westHeightmap = dimInfo.getHeightmap(context.getChunkX() - 1, context.getChunkZ());
        ChunkHeightmap eastHeightmap = dimInfo.getHeightmap(context.getChunkX() + 1, context.getChunkZ());
        ChunkHeightmap northHeightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ() - 1);
        ChunkHeightmap southHeightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ() + 1);
        int minY = context.getWorldInfo().getMinHeight();

        for (int i = 0; i < 16; i++) {
            buildRetainingWallColumn(context, 0, i, targets[0][i],
                    TerrainEmbeddingEngine.terrainHeight(westHeightmap, 15, i, targets[0][i]),
                    wall, minY);
            buildRetainingWallColumn(context, 15, i, targets[15][i],
                    TerrainEmbeddingEngine.terrainHeight(eastHeightmap, 0, i, targets[15][i]),
                    wall, minY);
            buildRetainingWallColumn(context, i, 0, targets[i][0],
                    TerrainEmbeddingEngine.terrainHeight(northHeightmap, i, 15, targets[i][0]),
                    wall, minY);
            buildRetainingWallColumn(context, i, 15, targets[i][15],
                    TerrainEmbeddingEngine.terrainHeight(southHeightmap, i, 0, targets[i][15]),
                    wall, minY);
        }
    }

    private void buildRetainingWallColumn(GenerationContext context,
                                          int x,
                                          int z,
                                          int topY,
                                          int outsideY,
                                          Material wall,
                                          int minY) {
        int drop = topY - outsideY;
        if (drop < BUILDING_RETAINING_WALL_MIN_DROP) {
            return;
        }
        int bottom = Math.max(minY, outsideY + 1);
        for (int y = topY; y >= bottom; y--) {
            context.setBlock(x, y, z, wall);
        }
    }

    private void clampStreetGradientLockedEdges(int[][] targets, int maxStep) {
        if (targets == null || maxStep <= 0) {
            return;
        }
        for (int pass = 0; pass < 10; pass++) {
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    int v = targets[x][z];
                    v = clampNear(v, targets[x - 1][z], maxStep);
                    v = clampNear(v, targets[x + 1][z], maxStep);
                    v = clampNear(v, targets[x][z - 1], maxStep);
                    v = clampNear(v, targets[x][z + 1], maxStep);
                    targets[x][z] = v;
                }
            }
        }
    }

    private void clampStreetGradient(int[][] targets, int maxStep) {
        if (targets == null || maxStep <= 0) {
            return;
        }
        for (int pass = 0; pass < 4; pass++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int v = targets[x][z];
                    if (x > 0) {
                        v = clampNear(v, targets[x - 1][z], maxStep);
                    }
                    if (z > 0) {
                        v = clampNear(v, targets[x][z - 1], maxStep);
                    }
                    targets[x][z] = v;
                }
            }
            for (int x = 15; x >= 0; x--) {
                for (int z = 15; z >= 0; z--) {
                    int v = targets[x][z];
                    if (x < 15) {
                        v = clampNear(v, targets[x + 1][z], maxStep);
                    }
                    if (z < 15) {
                        v = clampNear(v, targets[x][z + 1], maxStep);
                    }
                    targets[x][z] = v;
                }
            }
        }
    }

    private int clampNear(int value, int neighbor, int maxStep) {
        if (value > neighbor + maxStep) {
            return neighbor + maxStep;
        }
        if (value < neighbor - maxStep) {
            return neighbor - maxStep;
        }
        return value;
    }

    private int cornerBlendNW(GenerationContext context, BuildingInfo info, int fallbackY) {
        WeightedSum sum = new WeightedSum();
        sum.add(cornerAnchor(context, info, 0, 0, 0, 0, fallbackY), info.isCity ? 2 : 1);
        BuildingInfo west = info.getXmin();
        sum.add(cornerAnchor(context, west, -1, 0, 15, 0, fallbackY), west.isCity ? 2 : 1);
        BuildingInfo north = info.getZmin();
        sum.add(cornerAnchor(context, north, 0, -1, 0, 15, fallbackY), north.isCity ? 2 : 1);
        BuildingInfo nw = west.getZmin();
        sum.add(cornerAnchor(context, nw, -1, -1, 15, 15, fallbackY), nw.isCity ? 2 : 1);
        return sum.valueOr(fallbackY);
    }

    private int sideAnchor(GenerationContext context,
                           BuildingInfo current,
                           int fallbackY,
                           int dx,
                           int dz,
                           int localX,
                           int localZ) {
        if (current == null) {
            return fallbackY;
        }
        int currentGround = current.getCityGroundLevel();

        BuildingInfo neighbor = switch (dx) {
            case -1 -> current.getXmin();
            case 1 -> current.getXmax();
            default -> (dz < 0 ? current.getZmin() : current.getZmax());
        };
        if (neighbor != null && neighbor.isCity) {
            return Math.round((currentGround + neighbor.getCityGroundLevel()) / 2.0f);
        }

        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return currentGround;
        }
        ChunkHeightmap neighborHeightmap = dimInfo.getHeightmap(context.getChunkX() + dx, context.getChunkZ() + dz);
        int terrain = TerrainEmbeddingEngine.terrainHeight(neighborHeightmap, localX, localZ, currentGround);
        return Math.round(currentGround * 0.35f + terrain * 0.65f);
    }

    private int buildingSideAnchor(GenerationContext context,
                                   BuildingInfo current,
                                   int centerY,
                                   int dx,
                                   int dz,
                                   int localX,
                                   int localZ) {
        if (current == null) {
            return centerY;
        }
        BuildingInfo neighbor = switch (dx) {
            case -1 -> current.getXmin();
            case 1 -> current.getXmax();
            default -> (dz < 0 ? current.getZmin() : current.getZmax());
        };
        if (neighbor != null && neighbor.isCity) {
            return Math.min(centerY, Math.round((centerY + neighbor.getCityGroundLevel() - 1) / 2.0f));
        }

        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return centerY - 2;
        }
        ChunkHeightmap neighborHeightmap = dimInfo.getHeightmap(context.getChunkX() + dx, context.getChunkZ() + dz);
        int terrain = TerrainEmbeddingEngine.terrainHeight(neighborHeightmap, localX, localZ, centerY - 2);
        int blended = Math.round(centerY * 0.35f + terrain * 0.65f);
        return Math.max(centerY - BUILDING_APRON_MAX_DROP, Math.min(centerY, blended));
    }

    private int cornerBlendNE(GenerationContext context, BuildingInfo info, int fallbackY) {
        WeightedSum sum = new WeightedSum();
        sum.add(cornerAnchor(context, info, 0, 0, 15, 0, fallbackY), info.isCity ? 2 : 1);
        BuildingInfo east = info.getXmax();
        sum.add(cornerAnchor(context, east, 1, 0, 0, 0, fallbackY), east.isCity ? 2 : 1);
        BuildingInfo north = info.getZmin();
        sum.add(cornerAnchor(context, north, 0, -1, 15, 15, fallbackY), north.isCity ? 2 : 1);
        BuildingInfo ne = east.getZmin();
        sum.add(cornerAnchor(context, ne, 1, -1, 0, 15, fallbackY), ne.isCity ? 2 : 1);
        return sum.valueOr(fallbackY);
    }

    private int cornerBlendSW(GenerationContext context, BuildingInfo info, int fallbackY) {
        WeightedSum sum = new WeightedSum();
        sum.add(cornerAnchor(context, info, 0, 0, 0, 15, fallbackY), info.isCity ? 2 : 1);
        BuildingInfo west = info.getXmin();
        sum.add(cornerAnchor(context, west, -1, 0, 15, 15, fallbackY), west.isCity ? 2 : 1);
        BuildingInfo south = info.getZmax();
        sum.add(cornerAnchor(context, south, 0, 1, 0, 0, fallbackY), south.isCity ? 2 : 1);
        BuildingInfo sw = west.getZmax();
        sum.add(cornerAnchor(context, sw, -1, 1, 15, 0, fallbackY), sw.isCity ? 2 : 1);
        return sum.valueOr(fallbackY);
    }

    private int cornerBlendSE(GenerationContext context, BuildingInfo info, int fallbackY) {
        WeightedSum sum = new WeightedSum();
        sum.add(cornerAnchor(context, info, 0, 0, 15, 15, fallbackY), info.isCity ? 2 : 1);
        BuildingInfo east = info.getXmax();
        sum.add(cornerAnchor(context, east, 1, 0, 0, 15, fallbackY), east.isCity ? 2 : 1);
        BuildingInfo south = info.getZmax();
        sum.add(cornerAnchor(context, south, 0, 1, 15, 0, fallbackY), south.isCity ? 2 : 1);
        BuildingInfo se = east.getZmax();
        sum.add(cornerAnchor(context, se, 1, 1, 0, 0, fallbackY), se.isCity ? 2 : 1);
        return sum.valueOr(fallbackY);
    }

    private int cornerAnchor(GenerationContext context,
                             BuildingInfo chunkInfo,
                             int dx,
                             int dz,
                             int localX,
                             int localZ,
                             int fallbackY) {
        if (chunkInfo == null) {
            return fallbackY;
        }
        if (chunkInfo.isCity) {
            return chunkInfo.getCityGroundLevel();
        }
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return fallbackY;
        }
        ChunkHeightmap map = dimInfo.getHeightmap(context.getChunkX() + dx, context.getChunkZ() + dz);
        return TerrainEmbeddingEngine.terrainHeight(map, localX, localZ, fallbackY);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static final class WeightedSum {
        private int sum = 0;
        private int weight = 0;

        void add(int value, int w) {
            if (w <= 0) {
                return;
            }
            sum += value * w;
            weight += w;
        }

        int valueOr(int fallback) {
            if (weight <= 0) {
                return fallback;
            }
            return Math.round((float) sum / (float) weight);
        }
    }

    private void generateStreet(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        CompiledPalette palette = context.palette();
        char roadChar = 'S';
        char roadVariantChar = 0;
        char roadBaseChar = 0;
        char wallChar = 0;
        char borderChar = 0;

        CityStyle cityStyle = info.getCityStyle();
        StreetSettings streetSettings = cityStyle == null ? null : cityStyle.getStreetBlocks();
        if (streetSettings != null) {
            roadChar = firstChar(streetSettings.getStreet(), roadChar);
            roadVariantChar = firstChar(streetSettings.getStreetVariant(), (char) 0);
            roadBaseChar = firstChar(streetSettings.getStreetBase(), (char) 0);
            wallChar = firstChar(streetSettings.getWall(), (char) 0);
            borderChar = firstChar(streetSettings.getBorder(), (char) 0);
        }

        Material road = resolveFromPalette(context, palette, roadChar, Material.DEEPSLATE_TILES);
        Material roadVariant = roadVariantChar == 0
                ? road
                : resolveFromPalette(context, palette, roadVariantChar, road);
        Material roadBase = roadBaseChar == 0
                ? road
                : resolveFromPalette(context, palette, roadBaseChar, road);
        Material wallMaterial = wallChar == 0
                ? (borderChar == 0 ? roadBase : resolveFromPalette(context, palette, borderChar, roadBase))
                : resolveFromPalette(context, palette, wallChar, roadBase);

        int y = info.getCityGroundLevel();
        clearAboveSurface(context, y);
        StreetPartPlacement streetPartPlacement = selectStreetPartPlacement(info, streetSettings);
        boolean renderedStreetPart = streetPartPlacement != null && renderStreetPart(context, streetPartPlacement, y - 1);

        ChunkHeightmap heightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ());
        ChunkHeightmap westHeightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX() - 1, context.getChunkZ());
        ChunkHeightmap eastHeightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX() + 1, context.getChunkZ());
        ChunkHeightmap northHeightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ() - 1);
        ChunkHeightmap southHeightmap = context.getDimensionInfo() == null
                ? null
                : context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ() + 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!renderedStreetPart) {
                    context.setBlock(x, y - 1, z, roadBase);
                    context.setBlock(x, y, z, pickStreetSurface(context, x, z, road, roadVariant));
                }

                int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, y - 1);
                clearStreetHeadroom(context, x, z, y, terrainHeight);
                int foundationDepth = isEdge(x, z) ? 64 : 24;
                fillStreetFoundation(context, x, z, y - 1, terrainHeight, roadBase, foundationDepth);
                applyRetainingWall(context, x, z, y - 1, wallMaterial,
                        westHeightmap, eastHeightmap, northHeightmap, southHeightmap);
            }
        }
        cleanupStreetVegetation(context, y);
    }

    private void generateVacantLot(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        int y = info.getCityGroundLevel();
        clearAboveSurface(context, y);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                context.setBlock(x, y - 1, z, Material.STONE_BRICKS);
                context.setBlock(x, y, z, pickRuinFloorMaterial(context, x, z, y));
            }
        }
    }

    private void clearAboveSurface(GenerationContext context, int surfaceY) {
        int fromY = Math.max(context.getWorldInfo().getMinHeight(), surfaceY + 1);
        int toY = context.getWorldInfo().getMaxHeight() - 1;
        if (fromY > toY) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = fromY; y <= toY; y++) {
                    context.setBlock(x, y, z, Material.AIR);
                }
            }
        }
    }

    private void cleanupStreetVegetation(GenerationContext context, int roadY) {
        int minY = Math.max(context.getWorldInfo().getMinHeight(), roadY);
        int maxY = Math.min(context.getWorldInfo().getMaxHeight() - 1, roadY + 24);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Material current = context.getBlockType(x, y, z);
                    if (!isRoadPollutingVegetation(current)) {
                        continue;
                    }
                    Material below = y > context.getWorldInfo().getMinHeight()
                            ? context.getBlockType(x, y - 1, z)
                            : Material.AIR;
                    boolean onRoadSurface = y <= roadY + 1;
                    boolean floating = !isSolidSupport(below) && !isRoadPollutingVegetation(below);
                    if (onRoadSurface || floating) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
            }
        }
    }

    private boolean isRoadPollutingVegetation(Material material) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_LEAVES")
                || name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_SAPLING")
                || name.equals("VINE")
                || name.equals("MOSS_BLOCK")
                || name.equals("MOSS_CARPET")
                || name.contains("GRASS")
                || name.contains("FERN");
    }

    private void clearStreetOverhangs(GenerationContext context, int roadY) {
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int fromY = Math.max(minY, roadY + 2);
        int toY = Math.min(maxY, roadY + 48);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = fromY; y <= toY; y++) {
                    Material current = context.getBlockType(x, y, z);
                    if (isStreetOverhangBlock(current)) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
            }
        }
    }

    private boolean isStreetOverhangBlock(Material material) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        if (STREET_OVERHANG_CLEAR_MATERIALS.contains(material)) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_LEAVES")
                || name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_SAPLING")
                || name.contains("GRASS")
                || name.contains("FERN")
                || name.equals("VINE");
    }

    private void stabilizeBuildingFoundation(GenerationContext context, BuildingInfo info) {
        if (info == null || !info.hasBuilding) {
            return;
        }
        int minY = context.getWorldInfo().getMinHeight();
        int topY = info.getCityGroundLevel() - Math.max(0, info.cellars) * GenerationHeightModel.FLOOR_HEIGHT - 1;
        int bottomY = Math.max(minY, topY - GenerationHeightModel.FLOOR_HEIGHT * 8);
        int probeTopY = Math.min(context.getWorldInfo().getMaxHeight() - 1, info.getMaxHeight() + 2);
        Material support = Material.STONE;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int lowestSolidY = -1;
                for (int y = topY; y <= probeTopY; y++) {
                    Material cap = context.getBlockType(x, y, z);
                    if (cap == null || cap == Material.AIR || cap == Material.CAVE_AIR || cap == Material.VOID_AIR) {
                        continue;
                    }
                    lowestSolidY = y;
                    break;
                }
                if (lowestSolidY < 0) {
                    continue;
                }
                for (int y = lowestSolidY - 1; y >= bottomY; y--) {
                    Material current = context.getBlockType(x, y, z);
                    if (current == null || current == Material.AIR || current == Material.CAVE_AIR || current == Material.VOID_AIR) {
                        context.setBlock(x, y, z, support);
                        continue;
                    }
                    break;
                }
            }
        }
    }

    private StreetPartPlacement selectStreetPartPlacement(BuildingInfo info, StreetSettings streetSettings) {
        if (info == null || streetSettings == null) {
            return null;
        }
        Map<String, String> parts = streetSettings.getParts();
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        boolean n = isStreetNeighbor(info.getZmin());
        boolean s = isStreetNeighbor(info.getZmax());
        boolean w = isStreetNeighbor(info.getXmin());
        boolean e = isStreetNeighbor(info.getXmax());

        PredefinedStreet predefinedStreet = City.getPredefinedStreet(info.coord);
        if (predefinedStreet != null) {
            ConnectionOverride connectionOverride = connectionOverride(predefinedStreet);
            if (connectionOverride != null) {
                n = connectionOverride.north;
                s = connectionOverride.south;
                w = connectionOverride.west;
                e = connectionOverride.east;
            }
            String forcedType = normalizeType(predefinedStreet.getType());
            if (forcedType != null) {
                return placementFromForcedType(parts, forcedType, n, s, w, e);
            }
        }

        int count = (n ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0) + (e ? 1 : 0);
        if (count == 4) {
            String id = firstAvailable(parts, "all", "full");
            return id == null ? null : new StreetPartPlacement(id, Transform.ROTATE_NONE);
        }
        if (count == 0) {
            String id = firstAvailable(parts, "none");
            return id == null ? null : new StreetPartPlacement(id, Transform.ROTATE_NONE);
        }
        if (count == 1) {
            String id = firstAvailable(parts, "end");
            if (id == null) {
                return null;
            }
            if (w) return new StreetPartPlacement(id, Transform.ROTATE_NONE);
            if (s) return new StreetPartPlacement(id, Transform.ROTATE_90);
            if (e) return new StreetPartPlacement(id, Transform.ROTATE_180);
            return new StreetPartPlacement(id, Transform.ROTATE_270);
        }
        if (count == 2) {
            if ((w && e) || (n && s)) {
                String id = firstAvailable(parts, "straight");
                if (id == null) {
                    return null;
                }
                return n && s
                        ? new StreetPartPlacement(id, Transform.ROTATE_90)
                        : new StreetPartPlacement(id, Transform.ROTATE_NONE);
            }
            String id = firstAvailable(parts, "bend");
            if (id == null) {
                return null;
            }
            if (w && n) return new StreetPartPlacement(id, Transform.ROTATE_NONE);
            if (w && s) return new StreetPartPlacement(id, Transform.ROTATE_90);
            if (e && s) return new StreetPartPlacement(id, Transform.ROTATE_180);
            return new StreetPartPlacement(id, Transform.ROTATE_270); // e && n
        }
        if (count == 3) {
            String id = firstAvailable(parts, "t");
            if (id == null) {
                return null;
            }
            if (!s) return new StreetPartPlacement(id, Transform.ROTATE_NONE);
            if (!e) return new StreetPartPlacement(id, Transform.ROTATE_90);
            if (!n) return new StreetPartPlacement(id, Transform.ROTATE_180);
            return new StreetPartPlacement(id, Transform.ROTATE_270); // !w
        }
        return null;
    }

    private StreetPartPlacement placementFromForcedType(Map<String, String> parts,
                                                        String forcedType,
                                                        boolean n,
                                                        boolean s,
                                                        boolean w,
                                                        boolean e) {
        if (parts == null || forcedType == null) {
            return null;
        }
        return switch (forcedType) {
            case "all" -> {
                String id = firstAvailable(parts, "all", "full");
                yield id == null ? null : new StreetPartPlacement(id, Transform.ROTATE_NONE);
            }
            case "none" -> {
                String id = firstAvailable(parts, "none");
                yield id == null ? null : new StreetPartPlacement(id, Transform.ROTATE_NONE);
            }
            case "straight" -> {
                String id = firstAvailable(parts, "straight");
                if (id == null) {
                    yield null;
                }
                yield n || s
                        ? new StreetPartPlacement(id, Transform.ROTATE_90)
                        : new StreetPartPlacement(id, Transform.ROTATE_NONE);
            }
            case "end" -> {
                String id = firstAvailable(parts, "end");
                if (id == null) {
                    yield null;
                }
                if (w) yield new StreetPartPlacement(id, Transform.ROTATE_NONE);
                if (s) yield new StreetPartPlacement(id, Transform.ROTATE_90);
                if (e) yield new StreetPartPlacement(id, Transform.ROTATE_180);
                yield new StreetPartPlacement(id, Transform.ROTATE_270);
            }
            case "bend" -> {
                String id = firstAvailable(parts, "bend");
                if (id == null) {
                    yield null;
                }
                if (w && n) yield new StreetPartPlacement(id, Transform.ROTATE_NONE);
                if (w && s) yield new StreetPartPlacement(id, Transform.ROTATE_90);
                if (e && s) yield new StreetPartPlacement(id, Transform.ROTATE_180);
                yield new StreetPartPlacement(id, Transform.ROTATE_270);
            }
            case "t" -> {
                String id = firstAvailable(parts, "t");
                if (id == null) {
                    yield null;
                }
                if (!s) yield new StreetPartPlacement(id, Transform.ROTATE_NONE);
                if (!e) yield new StreetPartPlacement(id, Transform.ROTATE_90);
                if (!n) yield new StreetPartPlacement(id, Transform.ROTATE_180);
                yield new StreetPartPlacement(id, Transform.ROTATE_270);
            }
            case "full" -> {
                String id = firstAvailable(parts, "full", "all");
                yield id == null ? null : new StreetPartPlacement(id, Transform.ROTATE_NONE);
            }
            default -> null;
        };
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "cross", "x", "intersection", "all" -> "all";
            case "empty", "isolated", "none" -> "none";
            case "line", "straight" -> "straight";
            case "cap", "deadend", "dead_end", "end" -> "end";
            case "corner", "turn", "bend", "elbow" -> "bend";
            case "tee", "tjunction", "t_junction", "t" -> "t";
            case "full" -> "full";
            default -> null;
        };
    }

    private ConnectionOverride connectionOverride(PredefinedStreet street) {
        if (street == null) {
            return null;
        }
        String raw = street.getConnections();
        boolean hasString = raw != null && !raw.isBlank();
        boolean hasBooleans = street.getNorth() != null
                || street.getSouth() != null
                || street.getWest() != null
                || street.getEast() != null;
        if (!hasString && !hasBooleans) {
            return null;
        }

        boolean n = hasString && containsConnection(raw, 'n');
        boolean s = hasString && containsConnection(raw, 's');
        boolean w = hasString && containsConnection(raw, 'w');
        boolean e = hasString && containsConnection(raw, 'e');

        if (street.getNorth() != null) n = street.getNorth();
        if (street.getSouth() != null) s = street.getSouth();
        if (street.getWest() != null) w = street.getWest();
        if (street.getEast() != null) e = street.getEast();

        return new ConnectionOverride(n, s, w, e);
    }

    private boolean containsConnection(String raw, char key) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        return normalized.indexOf(key) >= 0;
    }

    private boolean renderStreetPart(GenerationContext context, StreetPartPlacement placement, int baseY) {
        if (placement == null || placement.partId == null || placement.partId.isBlank()) {
            return false;
        }
        CityStyle cityStyle = context.getBuildingInfo().getCityStyle();
        if (cityStyle == null) {
            return false;
        }
        BuildingPart part = AssetRegistries.PARTS.get(
                context.getDimensionInfo().getWorld(),
                resolveLocation(cityStyle.getId(), placement.partId));
        if (part == null) {
            return false;
        }
        CompiledPalette palette = composePalette(context, part);
        renderPart(context, part, palette, baseY, false, placement.transform, false, null);
        return true;
    }

    private String firstAvailable(Map<String, String> parts, String... keys) {
        if (parts == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String value = parts.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isStreetNeighbor(BuildingInfo info) {
        if (info == null) {
            return false;
        }
        if (City.getPredefinedStreet(info.coord) != null) {
            return true;
        }
        if (!info.isCity) {
            return false;
        }
        if (info.hasBuilding) {
            return false;
        }
        return info.highwayXLevel <= 0 && info.highwayZLevel <= 0;
    }

    private Material pickStreetSurface(GenerationContext context, int x, int z, Material primary, Material variant) {
        Material safePrimary = primary == null ? Material.STONE_BRICKS : primary;
        Material safeVariant = variant == null ? safePrimary : variant;

        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) context.getChunkX() * 341873128712L;
        hash ^= (long) context.getChunkZ() * 132897987541L;
        hash ^= (long) x * 0x517cc1b727220a95L;
        hash ^= (long) z * 0x94d049bb133111ebL;
        int roll = (int) Math.floorMod(hash, 100);

        Material accentA = chooseStreetAccentA(safePrimary, safeVariant);
        Material accentB = chooseStreetAccentB(safePrimary, safeVariant);

        if (roll < 56) {
            return safePrimary;
        }
        if (roll < 84) {
            return safeVariant;
        }
        if (roll < 95) {
            return accentA;
        }
        return accentB;
    }

    private Material chooseStreetAccentA(Material primary, Material variant) {
        if (variant != primary) {
            return variant;
        }
        return primary;
    }

    private Material chooseStreetAccentB(Material primary, Material variant) {
        return variant != primary ? primary : Material.CRACKED_STONE_BRICKS;
    }

    private CompiledPalette composePalette(GenerationContext context, BuildingPart part) {
        List<Palette> additions = new ArrayList<>();
        if (part.getLocalPalette() != null) {
            additions.add(part.getLocalPalette());
        }
        if (part.getPalette() != null) {
            Palette ref = AssetRegistries.PALETTES.get(
                    context.getDimensionInfo().getWorld(),
                    resolveLocation(part.getId(), part.getPalette()));
            if (ref != null) {
                additions.add(ref);
            }
        }
        if (additions.isEmpty()) {
            return context.palette();
        }
        return new CompiledPalette(context.palette(), additions.toArray(Palette[]::new));
    }

    private com.during.cityloader.util.ResourceLocation resolveLocation(com.during.cityloader.util.ResourceLocation owner, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return new com.during.cityloader.util.ResourceLocation(normalized);
        }
        return new com.during.cityloader.util.ResourceLocation(owner.getNamespace(), normalized);
    }

    private record StreetPartPlacement(String partId, Transform transform) {
    }

    private record ConnectionOverride(boolean north, boolean south, boolean west, boolean east) {
    }

    private void fillStreetFoundation(GenerationContext context,
                                      int x,
                                      int z,
                                      int fromY,
                                      int terrainHeight,
                                      Material base,
                                      int maxDepth) {
        if (base == null || base == Material.AIR) {
            base = Material.STONE;
        }
        int minY = context.getWorldInfo().getMinHeight();
        int bottom = Math.max(minY, fromY - Math.max(1, maxDepth));
        int targetBottom = Math.max(bottom, terrainHeight);
        for (int y = fromY; y >= targetBottom; y--) {
            context.setBlock(x, y, z, base);
        }
    }

    private void clearStreetHeadroom(GenerationContext context, int x, int z, int roadY, int terrainHeight) {
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int clearTo = Math.min(maxY, Math.max(roadY + 3, terrainHeight));
        for (int y = roadY + 1; y <= clearTo; y++) {
            context.setBlock(x, y, z, Material.AIR);
        }
    }

    private boolean isEdge(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }

    private void applyRetainingWall(GenerationContext context,
                                    int x,
                                    int z,
                                    int topY,
                                    Material wallMaterial,
                                    ChunkHeightmap west,
                                    ChunkHeightmap east,
                                    ChunkHeightmap north,
                                    ChunkHeightmap south) {
        if (wallMaterial == null || wallMaterial == Material.AIR) {
            wallMaterial = Material.STONE_BRICKS;
        }

        int outside = Integer.MIN_VALUE;
        if (x == 0) {
            outside = Math.max(outside, TerrainEmbeddingEngine.terrainHeight(west, 15, z, topY));
        }
        if (x == 15) {
            outside = Math.max(outside, TerrainEmbeddingEngine.terrainHeight(east, 0, z, topY));
        }
        if (z == 0) {
            outside = Math.max(outside, TerrainEmbeddingEngine.terrainHeight(north, x, 15, topY));
        }
        if (z == 15) {
            outside = Math.max(outside, TerrainEmbeddingEngine.terrainHeight(south, x, 0, topY));
        }
        if (outside == Integer.MIN_VALUE) {
            return;
        }

        int drop = topY - outside;
        if (drop < STREET_RETAINING_WALL_MIN_DROP) {
            return;
        }

        int minY = context.getWorldInfo().getMinHeight();
        int bottom = Math.max(minY, outside + 1);
        for (int y = topY; y >= bottom; y--) {
            context.setBlock(x, y, z, wallMaterial);
        }
    }

    private void renderPart(GenerationContext context,
                            BuildingPart part,
                            CompiledPalette palette,
                            int baseY,
                            boolean overlay,
                            Transform transform,
                            boolean terrainFixEnabled,
                            LostCityProfile profile) {
        if (part == null) {
            return;
        }
        if (transform == null) {
            transform = Transform.ROTATE_NONE;
        }

        List<List<String>> layers = part.getSliceLayers();
        if (layers == null || layers.isEmpty()) {
            return;
        }

        Material[][] baseMaterials = new Material[16][16];
        int maxLayers = Math.min(part.getDepth(), layers.size());
        for (int dy = 0; dy < maxLayers; dy++) {
            int y = baseY + dy;
            List<String> rows = layers.get(dy);
            int sourceMaxZ = Math.min(part.getHeight(), rows.size());
            int sourceMaxX = part.getWidth();
            int boundX = sourceMaxX - 1;
            int boundZ = sourceMaxZ - 1;
            if (boundX < 0 || boundZ < 0) {
                continue;
            }
            for (int z = 0; z < sourceMaxZ; z++) {
                String row = rows.get(z);
                int maxX = Math.min(sourceMaxX, row.length());
                for (int x = 0; x < maxX; x++) {
                    char token = row.charAt(x);
                    int worldX = transform.mapX(x, z, boundX, boundZ);
                    int worldZ = transform.mapZ(x, z, boundX, boundZ);
                    if (token == ' ') {
                        if (!overlay) {
                            context.setBlock(worldX, y, worldZ, Material.AIR);
                        }
                        continue;
                    }
                    if (token == '.') {
                        if (!overlay && dy == 0) {
                            Material fallbackFloor = pickRuinFloorMaterial(context, worldX, worldZ, y);
                            context.setBlock(worldX, y, worldZ, fallbackFloor);
                            baseMaterials[worldX][worldZ] = fallbackFloor;
                        } else if (!overlay) {
                            context.setBlock(worldX, y, worldZ, Material.AIR);
                        }
                        continue;
                    }

                    CompiledPalette.Information information = palette.getInformation(token);
                    if (information != null && worldX >= 0 && worldX < 16 && worldZ >= 0 && worldZ < 16) {
                        context.getBuildingInfo().addPalettePostTodo(worldX, y, worldZ, part.getName(), information);
                    }
                    if (information != null && information.torch()) {
                        context.setBlock(worldX, y, worldZ, Material.AIR);
                        continue;
                    }

                    String definition = resolveFromPaletteString(context, palette, token);
                    if (definition == null || definition.isBlank()) {
                        if (!overlay && dy == 0) {
                            Material fallbackFloor = pickRuinFloorMaterial(context, worldX, worldZ, y);
                            context.setBlock(worldX, y, worldZ, fallbackFloor);
                            baseMaterials[worldX][worldZ] = fallbackFloor;
                        }
                        continue;
                    }
                    if (isAirDefinition(definition)) {
                        if (!overlay && dy == 0) {
                            Material fallbackFloor = pickRuinFloorMaterial(context, worldX, worldZ, y);
                            context.setBlock(worldX, y, worldZ, fallbackFloor);
                            baseMaterials[worldX][worldZ] = fallbackFloor;
                        } else {
                            context.setBlock(worldX, y, worldZ, Material.AIR);
                        }
                        continue;
                    }
                    if (definition.contains("structure_void")) {
                        continue;
                    }
                    context.setBlock(worldX, y, worldZ, definition);
                    if (dy == 0 && !overlay) {
                        Material resolved = context.resolveMaterial(definition, null);
                        if (resolved == null || resolved == Material.AIR) {
                            resolved = pickRuinFloorMaterial(context, worldX, worldZ, y);
                            context.setBlock(worldX, y, worldZ, resolved);
                        }
                        baseMaterials[worldX][worldZ] = resolved;
                    }
                }
            }
        }

        if (!overlay && terrainFixEnabled && profile != null) {
            applyTerrainFix(context, baseMaterials, baseY, profile);
        }
    }

    private void applyTerrainFix(GenerationContext context, Material[][] baseMaterials, int baseY, LostCityProfile profile) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }

        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        int maxLower = Math.max(1, Math.max(
                Math.abs(profile.getTerrainFixUpperMinOffset()),
                Math.abs(profile.getTerrainFixUpperMaxOffset())));
        int clearCeiling = Math.max(baseY + maxLower, context.getBuildingInfo().getMaxHeight() + 1);

        boolean[][] supportMask = buildSupportMask(baseMaterials);
        Material defaultSupport = Material.STONE;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Material support = baseMaterials[x][z];
                if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                    support = defaultSupport;
                }
                if (!supportMask[x][z]) {
                    continue;
                }

                int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, baseY);
                int foundationTop = baseY - 1;

                if (terrainHeight >= baseY) {
                    int clearFrom = Math.max(baseY, minY);
                    int clearTo = Math.min(terrainHeight, clearCeiling);
                    clearTo = Math.min(clearTo, maxY);
                    for (int y = clearFrom; y <= clearTo; y++) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }

                int supportBottom = Math.min(terrainHeight, foundationTop - BUILDING_SUPPORT_MAX_DEPTH);
                TerrainEmbeddingEngine.supportColumnToTerrain(context, x, z, foundationTop, supportBottom, support);
            }
        }
    }

    private boolean[][] buildSupportMask(Material[][] baseMaterials) {
        boolean[][] mask = new boolean[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Material m = baseMaterials[x][z];
                mask[x][z] = m != null && m != Material.AIR && m != Material.STRUCTURE_VOID;
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean[][] expanded = new boolean[16][16];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    boolean filled = mask[x][z];
                    if (!filled && x > 0) filled = mask[x - 1][z];
                    if (!filled && x < 15) filled = mask[x + 1][z];
                    if (!filled && z > 0) filled = mask[x][z - 1];
                    if (!filled && z < 15) filled = mask[x][z + 1];
                    expanded[x][z] = filled;
                }
            }
            mask = expanded;
        }
        return mask;
    }

    private Material pickRuinFloorMaterial(GenerationContext context, int localX, int localZ, int y) {
        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) context.getChunkX() * 341873128712L;
        hash ^= (long) context.getChunkZ() * 132897987541L;
        hash ^= (long) localX * 0x517cc1b727220a95L;
        hash ^= (long) localZ * 0x94d049bb133111ebL;
        hash ^= (long) y * 0xD6E8FEB86659FD93L;
        int roll = (int) Math.floorMod(hash, 100);
        if (roll < 42) {
            return Material.STONE_BRICKS;
        }
        if (roll < 68) {
            return Material.COBBLESTONE;
        }
        if (roll < 86) {
            return Material.ANDESITE;
        }
        if (roll < 95) {
            return Material.DEEPSLATE_BRICKS;
        }
        if (roll < 99) {
            return Material.STONE;
        }
        return Material.DEEPSLATE;
    }

    /**
     * 从调色板解析方块定义字符串（保留完整状态）
     */
    private String resolveFromPaletteString(GenerationContext context,
                                            CompiledPalette palette,
                                            char token) {
        return palette.get(token, context.getRandom());
    }

    private boolean isAirDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            return false;
        }
        String normalized = definition.toLowerCase(Locale.ROOT);
        int stateStart = normalized.indexOf('[');
        if (stateStart >= 0) {
            normalized = normalized.substring(0, stateStart);
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized.equals("air")
                || normalized.equals("cave_air")
                || normalized.equals("void_air");
    }

    /**
     * 从调色板解析方块材料（向后兼容，丢失状态）
     */
    private Material resolveFromPalette(GenerationContext context,
                                        CompiledPalette palette,
                                        char token,
                                        Material fallback) {
        String definition = palette.get(token, context.getRandom());
        if (definition == null || definition.isBlank()) {
            return fallback;
        }
        return context.resolveMaterial(definition, fallback);
    }

    private char firstChar(String value, char fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.charAt(0);
    }
}
