package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Railway;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.data.WorldPartSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 基础设施阶段（Highway / Railway / Corridor / Bridge）
 * 包含地形适配功能
 */
public class InfrastructureStage implements GenerationStage {
    private static final int HIGHWAY_CLEAR_HEIGHT = 15;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        WorldStyle worldStyle = context.getDimensionInfo().getWorldStyle();
        CityStyle cityStyle = info.getCityStyle();
        WorldPartSettings partSettings = worldStyle == null ? null : worldStyle.getPartSettings();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        boolean highwaysEnabled = profile.isHighwaysEnabled();
        boolean railwaysEnabled = profile.isRailwaysEnabled();
        
        if (worldStyle != null && worldStyle.getSettings() != null) {
            if (worldStyle.getSettings().getHighways() != null) {
                highwaysEnabled = worldStyle.getSettings().getHighways();
            }
            if (worldStyle.getSettings().getRailways() != null) {
                railwaysEnabled = worldStyle.getSettings().getRailways();
            }
        }

        if (highwaysEnabled && info.isCityRaw() && info.highwayXLevel > 0) {
            drawHighwayX(context, info.highwayXLevel, info.xBridge, cityStyle, worldStyle, partSettings, profile);
        }
        if (highwaysEnabled && info.isCityRaw() && info.highwayZLevel > 0) {
            drawHighwayZ(context, info.highwayZLevel, info.zBridge, cityStyle, worldStyle, partSettings, profile);
        }

        if (railwaysEnabled) {
            generateRailNetwork(context, info, cityStyle, worldStyle, partSettings, profile);
        }
    }

    private void generateRailNetwork(GenerationContext context,
                                     BuildingInfo info,
                                     CityStyle cityStyle,
                                     WorldStyle worldStyle,
                                     WorldPartSettings partSettings,
                                     LostCityProfile profile) {
        ChunkCoord coord = new ChunkCoord(context.getWorldInfo().getName(), context.getChunkX(), context.getChunkZ());
        Railway.RailChunkInfo railInfo = Railway.getRailChunkType(coord, context.getDimensionInfo(), profile);
        Railway.RailChunkType type = railInfo.getType();
        if (type == Railway.RailChunkType.NONE) {
            return;
        }

        int railY = GenerationHeightModel.railY(info, railInfo.getLevel(), worldStyle);

        switch (type) {
            case STATION_SURFACE, STATION_EXTENSION_SURFACE, STATION_UNDERGROUND, STATION_EXTENSION_UNDERGROUND -> {
                generateRailwayStation(context, railY, true, type, cityStyle, worldStyle, partSettings, profile);
            }
            case RAILS_END_HERE -> drawRailXEnd(context, railY, railInfo.getDirection(), cityStyle, worldStyle, partSettings, profile);
            case VERTICAL -> drawRailZ(context, railY, cityStyle, worldStyle, partSettings, profile, false);
            case THREE_SPLIT -> {
                drawRailX(context, railY, cityStyle, worldStyle, partSettings, profile, true);
                drawRailZ(context, railY, cityStyle, worldStyle, partSettings, profile, true);
            }
            case DOUBLE_BEND -> drawRailBend(context, railY, cityStyle, worldStyle, partSettings, profile);
            case GOING_DOWN_ONE_FROM_SURFACE ->
                    drawRailDown(context, railY, GenerationHeightModel.FLOOR_HEIGHT, railInfo.getDirection(), cityStyle, worldStyle, partSettings, profile, true);
            case GOING_DOWN_TWO_FROM_SURFACE, GOING_DOWN_FURTHER ->
                    drawRailDown(context, railY, GenerationHeightModel.FLOOR_HEIGHT * 2, railInfo.getDirection(), cityStyle, worldStyle, partSettings, profile, false);
            default -> drawRailX(context, railY, cityStyle, worldStyle, partSettings, profile, false);
        }
    }

    private void generateRailwayStation(GenerationContext context,
                                        int stationY,
                                        boolean xAxis,
                                        Railway.RailChunkType type,
                                        CityStyle cityStyle,
                                        WorldStyle worldStyle,
                                        WorldPartSettings partSettings,
                                        LostCityProfile profile) {
        if (partSettings != null) {
            String partName = (type == Railway.RailChunkType.STATION_EXTENSION_UNDERGROUND)
                    ? partSettings.getStationUndergroundStairs()
                    : partSettings.getStationUnderground();
            if (renderRailNamedPart(context, stationY, partName, cityStyle, worldStyle, profile)) {
                if (profile.isBridgeSupports()) {
                    applyRailTerrainFix(context, stationY, Material.STONE_BRICKS, xAxis, profile);
                }
                return;
            }
        }

        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                for (int z = 5; z <= 10; z++) {
                    context.setBlock(x, stationY, z, Material.SMOOTH_STONE);
                }
            }
            
            for (int x = 0; x < 16; x++) {
                setRailBlock(context, x, stationY + 1, 7, Material.POWERED_RAIL, Rail.Shape.EAST_WEST);
                setRailBlock(context, x, stationY + 1, 8, Material.POWERED_RAIL, Rail.Shape.EAST_WEST);
            }
        } else {
            for (int z = 0; z < 16; z++) {
                for (int x = 5; x <= 10; x++) {
                    context.setBlock(x, stationY, z, Material.SMOOTH_STONE);
                }
            }
            
            for (int z = 0; z < 16; z++) {
                setRailBlock(context, 7, stationY + 1, z, Material.POWERED_RAIL, Rail.Shape.NORTH_SOUTH);
                setRailBlock(context, 8, stationY + 1, z, Material.POWERED_RAIL, Rail.Shape.NORTH_SOUTH);
            }
        }
    }

    private void drawRailXEnd(GenerationContext context,
                              int y,
                              Railway.RailDirection direction,
                              CityStyle cityStyle,
                              WorldStyle worldStyle,
                              WorldPartSettings partSettings,
                              LostCityProfile profile) {
        if (partSettings != null && renderRailNamedPart(context, y, partSettings.getRailsHorizontalEnd(), cityStyle, worldStyle, profile)) {
            if (profile.isBridgeSupports()) {
                applyRailTerrainFix(context, y, Material.STONE_BRICKS, true, profile);
            }
            return;
        }

        drawRailX(context, y, cityStyle, worldStyle, partSettings, profile, false);
        int stopX = direction == Railway.RailDirection.WEST ? 0 : 15;
        context.setRail(stopX, y, 8, Material.POWERED_RAIL, Rail.Shape.EAST_WEST, true);
        context.setBlock(stopX, y + 1, 8, Material.REDSTONE_BLOCK);
        context.setBlock(stopX, y, 7, Material.STONE_BRICK_WALL);
        context.setBlock(stopX, y, 9, Material.STONE_BRICK_WALL);
    }

    private void drawRailBend(GenerationContext context,
                              int y,
                              CityStyle cityStyle,
                              WorldStyle worldStyle,
                              WorldPartSettings partSettings,
                              LostCityProfile profile) {
        if (partSettings != null && renderRailNamedPart(context, y, partSettings.getRailsBend(), cityStyle, worldStyle, profile)) {
            if (profile.isBridgeSupports()) {
                applyRailTerrainFix(context, y, Material.STONE_BRICKS, true, profile);
                applyRailTerrainFix(context, y, Material.STONE_BRICKS, false, profile);
            }
            return;
        }

        drawRailX(context, y, cityStyle, worldStyle, partSettings, profile, false);
        drawRailZ(context, y, cityStyle, worldStyle, partSettings, profile, false);
    }

    private void drawRailDown(GenerationContext context,
                              int y,
                              int drop,
                              Railway.RailDirection direction,
                              CityStyle cityStyle,
                              WorldStyle worldStyle,
                              WorldPartSettings partSettings,
                              LostCityProfile profile,
                              boolean oneLevelDrop) {
        if (partSettings != null) {
            String partName = oneLevelDrop ? partSettings.getRailsDown1() : partSettings.getRailsDown2();
            if (renderRailNamedPart(context, y, partName, cityStyle, worldStyle, profile)) {
                if (profile.isBridgeSupports()) {
                    applyRailTerrainFix(context, y, Material.STONE_BRICKS, true, profile);
                }
                return;
            }
        }
        drawRailXSlope(context, y, drop, direction, profile);
    }

    private void drawHighwayX(GenerationContext context,
                              int y,
                              boolean bridgeStyle,
                              CityStyle cityStyle,
                              WorldStyle worldStyle,
                              WorldPartSettings partSettings,
                              LostCityProfile profile) {
        boolean effectiveBridge = bridgeStyle || isWaterCrossing(context, y, true);
        boolean tunnelStyle = isTunnelRequired(context, y, true);
        BuildingPart part = pickHighwayPart(context, effectiveBridge, tunnelStyle, cityStyle, worldStyle, partSettings);
        if (part != null) {
            renderPart(context, part, y, false, Transform.ROTATE_NONE, profile);
        } else {
            Material deck = effectiveBridge ? Material.SMOOTH_STONE : Material.POLISHED_ANDESITE;
            if (tunnelStyle) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 6; z <= 9; z++) {
                        context.setBlock(x, y, z, deck);
                        for (int airY = y + 1; airY <= y + 3; airY++) {
                            context.setBlock(x, airY, z, Material.AIR);
                        }
                        context.setBlock(x, y + 4, z, Material.STONE_BRICKS);
                    }
                    for (int wallY = y; wallY <= y + 3; wallY++) {
                        context.setBlock(x, wallY, 5, Material.STONE_BRICKS);
                        context.setBlock(x, wallY, 10, Material.STONE_BRICKS);
                    }
                }
            } else {
                for (int x = 0; x < 16; x++) {
                    for (int z = 6; z <= 9; z++) {
                        context.setBlock(x, y, z, deck);
                    }
                }
            }
        }

        if (!tunnelStyle && shouldClearAboveHighway(profile)) {
            int clearFrom = y + (part == null ? 1 : Math.max(1, part.getDepth()));
            clearAboveHighway(context, clearFrom, HIGHWAY_CLEAR_HEIGHT);
        }

        if (profile.isHighwaySupports() && !tunnelStyle) {
            Material support = effectiveBridge ? Material.STONE_BRICKS : Material.COBBLED_DEEPSLATE;
            applyHighwayTerrainFix(context, y, support, true, profile);
        }
    }

    private void drawHighwayZ(GenerationContext context,
                              int y,
                              boolean bridgeStyle,
                              CityStyle cityStyle,
                              WorldStyle worldStyle,
                              WorldPartSettings partSettings,
                              LostCityProfile profile) {
        boolean effectiveBridge = bridgeStyle || isWaterCrossing(context, y, false);
        boolean tunnelStyle = isTunnelRequired(context, y, false);
        BuildingPart part = pickHighwayPart(context, effectiveBridge, tunnelStyle, cityStyle, worldStyle, partSettings);
        if (part != null) {
            // 历史 highway-Z 语义等价于当前 Transform 坐标系下的 ROTATE_270
            renderPart(context, part, y, false, Transform.ROTATE_270, profile);
        } else {
            Material deck = effectiveBridge ? Material.SMOOTH_STONE : Material.POLISHED_ANDESITE;
            if (tunnelStyle) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 6; x <= 9; x++) {
                        context.setBlock(x, y, z, deck);
                        for (int airY = y + 1; airY <= y + 3; airY++) {
                            context.setBlock(x, airY, z, Material.AIR);
                        }
                        context.setBlock(x, y + 4, z, Material.STONE_BRICKS);
                    }
                    for (int wallY = y; wallY <= y + 3; wallY++) {
                        context.setBlock(5, wallY, z, Material.STONE_BRICKS);
                        context.setBlock(10, wallY, z, Material.STONE_BRICKS);
                    }
                }
            } else {
                for (int z = 0; z < 16; z++) {
                    for (int x = 6; x <= 9; x++) {
                        context.setBlock(x, y, z, deck);
                    }
                }
            }
        }

        if (!tunnelStyle && shouldClearAboveHighway(profile)) {
            int clearFrom = y + (part == null ? 1 : Math.max(1, part.getDepth()));
            clearAboveHighway(context, clearFrom, HIGHWAY_CLEAR_HEIGHT);
        }

        if (profile.isHighwaySupports() && !tunnelStyle) {
            Material support = effectiveBridge ? Material.STONE_BRICKS : Material.COBBLED_DEEPSLATE;
            applyHighwayTerrainFix(context, y, support, false, profile);
        }
    }

    private void clearAboveHighway(GenerationContext context, int startY, int clearHeight) {
        if (clearHeight <= 0) {
            return;
        }
        int minY = minBuildHeight(context);
        int maxY = maxBuildHeightExclusive(context) - 1;
        int fromY = Math.max(startY, minY);
        int toY = Math.min(startY + clearHeight - 1, maxY);
        if (fromY > toY) {
            return;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = fromY; y <= toY; y++) {
                    Material current = context.getBlockType(x, y, z);
                    if (!isClearableAboveHighway(current)) {
                        continue;
                    }
                    context.setBlock(x, y, z, Material.AIR);
                }
            }
        }
    }

    private boolean shouldClearAboveHighway(LostCityProfile profile) {
        if (profile == null) {
            return true;
        }
        String landscape = profile.getLandscapeType();
        if (landscape == null || landscape.isBlank()) {
            return true;
        }
        String normalized = landscape.toLowerCase(Locale.ROOT);
        return !normalized.equals("cavern") && !normalized.equals("cavernspheres");
    }

    private boolean isClearableAboveHighway(Material material) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        String name = material.name();
        if (name.endsWith("_LEAVES") || name.endsWith("_LOG") || name.endsWith("_WOOD")) {
            return false;
        }
        return true;
    }

    private void applyHighwayTerrainFix(GenerationContext context, int y, Material support, boolean xAxis, LostCityProfile profile) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }

        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());

        int groundLevel = context.getBuildingInfo().groundLevel;
        int minY = minBuildHeight(context);
        int maxYExclusive = maxBuildHeightExclusive(context);
        int topY = Math.min(y - 1, maxYExclusive - 1);
        if (topY < minY) {
            return;
        }

        if (xAxis) {
            for (int x = 0; x < 16; x += 3) {
                int terrainHeight = getTerrainHeight(heightmap, x, 7, groundLevel);
                int bottomY = Math.max(terrainHeight, minY);
                for (int fillY = topY; fillY >= bottomY; fillY--) {
                    context.setBlock(x, fillY, 7, support);
                    context.setBlock(x, fillY, 8, support);
                }
            }
        } else {
            for (int z = 0; z < 16; z += 3) {
                int terrainHeight = getTerrainHeight(heightmap, 7, z, groundLevel);
                int bottomY = Math.max(terrainHeight, minY);
                for (int fillY = topY; fillY >= bottomY; fillY--) {
                    context.setBlock(7, fillY, z, support);
                    context.setBlock(8, fillY, z, support);
                }
            }
        }
    }

    private void drawRailX(GenerationContext context,
                           int y,
                           CityStyle cityStyle,
                           WorldStyle worldStyle,
                           WorldPartSettings partSettings,
                           LostCityProfile profile,
                           boolean crossing) {
        BuildingPart part = pickRailPart(context, true, cityStyle, worldStyle, partSettings, crossing);
        if (part != null) {
            renderPart(context, part, y, false, Transform.ROTATE_NONE, profile);
        } else {
            for (int x = 0; x < 16; x++) {
                setRailBlock(context, x, y, 8, Material.RAIL, Rail.Shape.EAST_WEST);
                context.setBlock(x, y - 1, 8, Material.STONE_BRICKS);
            }
        }

        if (profile.isBridgeSupports()) {
            applyRailTerrainFix(context, y, Material.STONE_BRICKS, true, profile);
        }
    }

    private void drawRailZ(GenerationContext context,
                           int y,
                           CityStyle cityStyle,
                           WorldStyle worldStyle,
                           WorldPartSettings partSettings,
                           LostCityProfile profile,
                           boolean crossing) {
        BuildingPart part = pickRailPart(context, false, cityStyle, worldStyle, partSettings, crossing);
        if (part != null) {
            renderPart(context, part, y, false, Transform.ROTATE_NONE, profile);
        } else {
            for (int z = 0; z < 16; z++) {
                setRailBlock(context, 8, y, z, Material.RAIL, Rail.Shape.NORTH_SOUTH);
                context.setBlock(8, y - 1, z, Material.STONE_BRICKS);
            }
        }

        if (profile.isBridgeSupports()) {
            applyRailTerrainFix(context, y, Material.STONE_BRICKS, false, profile);
        }
    }

    private void drawRailXSlope(GenerationContext context,
                                int lowY,
                                int drop,
                                Railway.RailDirection direction,
                                LostCityProfile profile) {
        int[] heights = new int[16];
        boolean lowOnWest = direction != Railway.RailDirection.EAST;

        for (int x = 0; x < 16; x++) {
            double progress = x / 15.0;
            int y = lowOnWest
                    ? lowY + (int) Math.round(drop * progress)
                    : lowY + (int) Math.round(drop * (1.0 - progress));
            heights[x] = y;
        }

        for (int x = 0; x < 16; x++) {
            Rail.Shape shape = Rail.Shape.EAST_WEST;
            if (x < 15 && heights[x + 1] > heights[x]) {
                shape = Rail.Shape.ASCENDING_EAST;
            } else if (x > 0 && heights[x - 1] > heights[x]) {
                shape = Rail.Shape.ASCENDING_WEST;
            }
            setRailBlock(context, x, heights[x], 8, Material.RAIL, shape);
            context.setBlock(x, heights[x] - 1, 8, Material.STONE_BRICKS);
        }

        if (profile.isBridgeSupports()) {
            applyRailSlopeTerrainFix(context, heights, Material.STONE_BRICKS);
        }
    }

    private void applyRailTerrainFix(GenerationContext context, int y, Material support, boolean xAxis, LostCityProfile profile) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }

        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        int groundLevel = context.getBuildingInfo().groundLevel;
        int minY = minBuildHeight(context);
        int maxYExclusive = maxBuildHeightExclusive(context);
        int topY = Math.min(y - 2, maxYExclusive - 1);
        if (topY < minY) {
            return;
        }

        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                int terrainHeight = getTerrainHeight(heightmap, x, 8, groundLevel);
                int bottomY = Math.max(terrainHeight, minY);
                for (int fillY = topY; fillY >= bottomY; fillY--) {
                    context.setBlock(x, fillY, 8, support);
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                int terrainHeight = getTerrainHeight(heightmap, 8, z, groundLevel);
                int bottomY = Math.max(terrainHeight, minY);
                for (int fillY = topY; fillY >= bottomY; fillY--) {
                    context.setBlock(8, fillY, z, support);
                }
            }
        }
    }

    private void applyRailSlopeTerrainFix(GenerationContext context, int[] heights, Material support) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return;
        }
        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        int groundLevel = context.getBuildingInfo().groundLevel;
        int minY = minBuildHeight(context);
        int maxYExclusive = maxBuildHeightExclusive(context);

        for (int x = 0; x < 16; x++) {
            int terrainHeight = getTerrainHeight(heightmap, x, 8, groundLevel);
            int bottomY = Math.max(terrainHeight, minY);
            int topY = Math.min(heights[x] - 2, maxYExclusive - 1);
            for (int fillY = topY; fillY >= bottomY; fillY--) {
                context.setBlock(x, fillY, 8, support);
            }
        }
    }

    private int getTerrainHeight(ChunkHeightmap heightmap, int x, int z, int fallback) {
        return TerrainEmbeddingEngine.terrainHeight(heightmap, x, z, fallback);
    }

    private int minBuildHeight(GenerationContext context) {
        if (context.getWorldInfo() == null) {
            return 0;
        }
        return context.getWorldInfo().getMinHeight();
    }

    private int maxBuildHeightExclusive(GenerationContext context) {
        if (context.getWorldInfo() == null) {
            return 256;
        }
        return context.getWorldInfo().getMaxHeight();
    }

    private BuildingPart pickHighwayPart(GenerationContext context,
                                         boolean bridgeStyle,
                                         boolean tunnelStyle,
                                         CityStyle cityStyle,
                                         WorldStyle worldStyle,
                                         WorldPartSettings partSettings) {
        BuildingInfo info = context.getBuildingInfo();
        boolean bi = info.highwayXLevel > 0 && info.highwayZLevel > 0;

        List<String> candidates = new ArrayList<>();
        if (partSettings != null) {
            if (tunnelStyle) {
                if (bi) {
                    candidates.add(partSettings.getTunnelBi());
                }
                candidates.add(partSettings.getTunnel());
            } else if (bridgeStyle) {
                if (bi) {
                    candidates.add(partSettings.getBridgeBi());
                }
                candidates.add(partSettings.getBridge());
            } else {
                if (bi) {
                    candidates.add(partSettings.getOpenBi());
                }
                candidates.add(partSettings.getOpen());
            }
        }

        if (bridgeStyle && !tunnelStyle && cityStyle != null) {
            String bridgeSelector = cityStyle.pickSelectorValue("bridges", context.getRandom());
            candidates.add(bridgeSelector);
        }

        if (tunnelStyle) {
            if (bi) {
                candidates.add("highway_tunnel_bi");
            }
            candidates.add("highway_tunnel");
        } else if (bridgeStyle) {
            if (bi) {
                candidates.add("highway_bridge_bi");
            }
            candidates.add("highway_bridge");
            candidates.add("bridge_open");
            candidates.add("bridge_covered");
        } else {
            if (bi) {
                candidates.add("highway_open_bi");
            }
            candidates.add("highway_open");
        }

        return findPart(context, cityStyle, worldStyle, candidates);
    }

    private boolean isWaterCrossing(GenerationContext context, int y, boolean xAxis) {
        if (isWaterBiome(context)) {
            return true;
        }
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return false;
        }
        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        if (heightmap == null) {
            return false;
        }

        int waterLevel = context.getBuildingInfo().waterLevel;
        int waterishColumns = 0;
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                int h1 = getTerrainHeight(heightmap, x, 7, y);
                int h2 = getTerrainHeight(heightmap, x, 8, y);
                if (h1 <= waterLevel + 1 || h2 <= waterLevel + 1) {
                    waterishColumns++;
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                int h1 = getTerrainHeight(heightmap, 7, z, y);
                int h2 = getTerrainHeight(heightmap, 8, z, y);
                if (h1 <= waterLevel + 1 || h2 <= waterLevel + 1) {
                    waterishColumns++;
                }
            }
        }
        return waterishColumns >= 5;
    }

    private boolean isTunnelRequired(GenerationContext context, int y, boolean xAxis) {
        IDimensionInfo dimInfo = context.getDimensionInfo();
        if (dimInfo == null) {
            return false;
        }
        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        if (heightmap == null) {
            return false;
        }

        int coveredColumns = 0;
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                int h1 = getTerrainHeight(heightmap, x, 7, y);
                int h2 = getTerrainHeight(heightmap, x, 8, y);
                if (Math.max(h1, h2) >= y + 4) {
                    coveredColumns++;
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                int h1 = getTerrainHeight(heightmap, 7, z, y);
                int h2 = getTerrainHeight(heightmap, 8, z, y);
                if (Math.max(h1, h2) >= y + 4) {
                    coveredColumns++;
                }
            }
        }
        return coveredColumns >= 10;
    }

    private BuildingPart pickRailPart(GenerationContext context,
                                      boolean xAxis,
                                      CityStyle cityStyle,
                                      WorldStyle worldStyle,
                                      WorldPartSettings partSettings,
                                      boolean crossing) {
        boolean waterBiome = isWaterBiome(context);

        List<String> candidates = new ArrayList<>();
        if (partSettings != null) {
            if (crossing) {
                candidates.add(partSettings.getRails3Split());
            }
            if (xAxis) {
                if (waterBiome) {
                    candidates.add(partSettings.getRailsHorizontalWater());
                    candidates.add(partSettings.getRailsHorizontal());
                } else {
                    candidates.add(partSettings.getRailsHorizontal());
                    candidates.add(partSettings.getRailsHorizontalWater());
                }
                candidates.add(partSettings.getRailsFlat());
            } else {
                if (waterBiome) {
                    candidates.add(partSettings.getRailsVerticalWater());
                    candidates.add(partSettings.getRailsVertical());
                } else {
                    candidates.add(partSettings.getRailsVertical());
                    candidates.add(partSettings.getRailsVerticalWater());
                }
                candidates.add(partSettings.getRailsFlat());
            }
        }

        if (crossing) {
            candidates.add("rails_3split");
        }
        if (xAxis) {
            if (waterBiome) {
                candidates.add("rails_horizontal_water");
                candidates.add("rails_horizontal");
            } else {
                candidates.add("rails_horizontal");
                candidates.add("rails_horizontal_water");
            }
            candidates.add("rails_flat");
        } else {
            if (waterBiome) {
                candidates.add("rails_vertical_water");
                candidates.add("rails_vertical");
            } else {
                candidates.add("rails_vertical");
                candidates.add("rails_vertical_water");
            }
            candidates.add("rails_flat");
        }

        return findPart(context, cityStyle, worldStyle, candidates);
    }
    
    private boolean isWaterBiome(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        int groundY = info.getCityGroundLevel();
        return groundY <= info.waterLevel;
    }

    private BuildingPart findPart(GenerationContext context,
                                  CityStyle cityStyle,
                                  WorldStyle worldStyle,
                                  List<String> candidates) {
        World world = context.getDimensionInfo().getWorld();
        for (String raw : candidates) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            BuildingPart part = null;
            if (worldStyle != null) {
                part = AssetRegistries.PARTS.get(world, resolveLocation(worldStyle.getId(), raw));
            }
            if (part == null && cityStyle != null) {
                part = AssetRegistries.PARTS.get(world, resolveLocation(cityStyle.getId(), raw));
            }
            if (part == null) {
                part = AssetRegistries.PARTS.get(world, new ResourceLocation(raw.toLowerCase(Locale.ROOT)));
            }
            if (part != null) {
                return part;
            }
        }
        return null;
    }

    private boolean renderRailNamedPart(GenerationContext context,
                                        int y,
                                        String partName,
                                        CityStyle cityStyle,
                                        WorldStyle worldStyle,
                                        LostCityProfile profile) {
        if (partName == null || partName.isBlank()) {
            return false;
        }
        BuildingPart part = findPart(context, cityStyle, worldStyle, List.of(partName));
        if (part == null) {
            return false;
        }
        renderPart(context, part, y, false, Transform.ROTATE_NONE, profile);
        return true;
    }

    private void renderPart(GenerationContext context,
                            BuildingPart part,
                            int baseY,
                            boolean overlay,
                            Transform transform,
                            LostCityProfile profile) {
        if (part == null) {
            return;
        }
        if (transform == null) {
            transform = Transform.ROTATE_NONE;
        }

        CompiledPalette palette = composePalette(context, part);
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

    private ResourceLocation resolveLocation(ResourceLocation owner, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return new ResourceLocation(normalized);
        }
        return new ResourceLocation(owner.getNamespace(), normalized);
    }

    private void setRailBlock(GenerationContext context,
                              int x,
                              int y,
                              int z,
                              Material material,
                              Rail.Shape shape) {
        context.setRail(x, y, z, material, shape, false);
    }
}
