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

/**
 * 核心城市生成阶段（建筑/街道）
 * 包含TerrainFix地形适配功能
 */
public class CityCoreStage implements GenerationStage {
    private static final int STREET_MAX_STEP = 1;
    private static final int STREET_RETAINING_WALL_MIN_DROP = 3;
    private static final int STREET_CENTER_MIN = 4;
    private static final int STREET_CENTER_MAX = 11;
    private static final int BUILDING_APRON_INSET = 4;
    private static final int BUILDING_APRON_MAX_DROP = 5;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        if (!info.isCity) {
            return;
        }

        prepareCitySurface(context, info);

        if (info.hasBuilding) {
            generateBuilding(context);
        } else {
            generateStreet(context);
        }
    }

    private void generateBuilding(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        CompiledPalette palette = context.palette();
        
        LostCityProfile profile = getProfile(context);
        boolean terrainFixEnabled = isTerrainFixEnabled(profile);
        
        for (int floor = -info.cellars; floor < info.floors; floor++) {
            int floorY = info.getCityGroundLevel() + floor * GenerationHeightModel.FLOOR_HEIGHT;
            boolean applyFoundationFix = terrainFixEnabled && floor == -info.cellars;
            renderPart(context, info.getFloor(floor), palette, floorY, false, info.getFloorTransform(floor), applyFoundationFix, profile);
            renderPart(context, info.getFloorPart2(floor), palette, floorY, true, info.getFloorPart2Transform(floor), false, profile);
        }
    }

    private LostCityProfile getProfile(GenerationContext context) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo != null) {
            return dimInfo.getProfile();
        }
        return null;
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
                    // 建筑区采用边缘裙边过渡并仅补土不挖空，减少边缘断崖感。
                    int targetY = smoothedTargets == null ? cityGround - 1 : smoothedTargets[x][z];
                    TerrainEmbeddingEngine.embedSurfaceColumnLimited(
                            context,
                            x,
                            z,
                            terrainHeight,
                            targetY,
                            base,
                            maxRaise + GenerationHeightModel.FLOOR_HEIGHT,
                            0);
                } else {
                    int targetY = smoothedTargets == null ? cityGround : smoothedTargets[x][z];
                    TerrainEmbeddingEngine.embedSurfaceColumnLimited(
                            context,
                            x,
                            z,
                            terrainHeight,
                            targetY,
                            base,
                            maxRaise,
                            maxLower);
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

        // 中心区域保持平台，边缘再渐变，避免“中间高低不平 + 边界断层”。
        for (int x = STREET_CENTER_MIN; x <= STREET_CENTER_MAX; x++) {
            for (int z = STREET_CENTER_MIN; z <= STREET_CENTER_MAX; z++) {
                targets[x][z] = fallbackY;
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
        return Math.round(currentGround * 0.75f + terrain * 0.25f);
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
                    context.setBlock(x, y, z, ((x + z) & 1) == 0 ? road : roadVariant);
                    // 仅清理少量街面净空，避免整列挖空导致断层。
                    context.setBlock(x, y + 1, z, Material.AIR);
                    context.setBlock(x, y + 2, z, Material.AIR);
                }

                int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, y - 1);
                int foundationDepth = isEdge(x, z) ? 64 : 24;
                fillStreetFoundation(context, x, z, y - 1, terrainHeight, roadBase, foundationDepth);
                applyRetainingWall(context, x, z, y - 1, wallMaterial,
                        westHeightmap, eastHeightmap, northHeightmap, southHeightmap);
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
            int sourceMaxZ = Math.min(16, Math.min(part.getHeight(), rows.size()));
            int sourceMaxX = Math.min(16, part.getWidth());
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
                    if (worldX < 0 || worldX > 15 || worldZ < 0 || worldZ > 15) {
                        continue;
                    }
                    if (token == ' ') {
                        if (!overlay) {
                            context.setBlock(worldX, y, worldZ, Material.AIR);
                        }
                        continue;
                    }

                    CompiledPalette.Information information = palette.getInformation(token);
                    if (information != null) {
                        context.getBuildingInfo().addPalettePostTodo(worldX, y, worldZ, part.getName(), information);
                    }
                    if (information != null && information.torch()) {
                        context.setBlock(worldX, y, worldZ, Material.AIR);
                        continue;
                    }

                    String definition = resolveFromPaletteString(context, palette, token);
                    if (definition == null || definition.isBlank()) {
                        continue;
                    }
                    if (definition.contains("structure_void")) {
                        continue;
                    }
                    context.setBlock(worldX, y, worldZ, definition);
                    if (dy == 0 && !overlay) {
                        baseMaterials[worldX][worldZ] = context.resolveMaterial(definition, null);
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

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Material support = baseMaterials[x][z];
                if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                    continue;
                }

                int terrainHeight = TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, baseY);
                int foundationTop = baseY - 1;

                if (terrainHeight >= baseY) {
                    int clearFrom = Math.max(baseY, minY);
                    int clearTo = Math.min(terrainHeight, baseY + maxLower);
                    clearTo = Math.min(clearTo, maxY);
                    for (int y = clearFrom; y <= clearTo; y++) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }

                TerrainEmbeddingEngine.supportColumnToTerrain(context, x, z, foundationTop, terrainHeight, support);
            }
        }
    }

    /**
     * 从调色板解析方块定义字符串（保留完整状态）
     */
    private String resolveFromPaletteString(GenerationContext context,
                                            CompiledPalette palette,
                                            char token) {
        return palette.get(token, context.getRandom());
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
