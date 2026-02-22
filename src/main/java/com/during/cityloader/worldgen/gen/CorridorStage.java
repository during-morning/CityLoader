package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.regassets.data.RailSettings;
import org.bukkit.Material;
import org.bukkit.block.data.Rail;

import java.util.Locale;
import java.util.Map;

/**
 * 走廊生成阶段
 * 在城市建筑间生成地下走廊连接
 * 兼容LostCities Corridors系统
 */
public class CorridorStage implements GenerationStage {

    private static final int CORRIDOR_DEPTH = -6;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCityRaw()) {
            return;
        }
        
        float corridorChance = profile.getCorridorChance();
        if (corridorChance <= 0) {
            return;
        }

        boolean xCorridor = info.hasXCorridor();
        boolean zCorridor = info.hasZCorridor();

        int baseGroundY = info.groundLevel;

        if (xCorridor || zCorridor) {
            generateCorridor(context, info, profile, baseGroundY, xCorridor, zCorridor);
        }
        generateCorridorConnections(context, info, baseGroundY);
    }

    private void generateCorridor(GenerationContext context,
                                  BuildingInfo info,
                                  LostCityProfile profile,
                                  int baseY,
                                  boolean xRail,
                                  boolean zRail) {
        int height = baseY + CORRIDOR_DEPTH;
        int minY = context.getWorldInfo().getMinHeight();
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        if (height < minY || height + 5 > maxY) {
            return;
        }

        CityStyle cityStyle = info.getCityStyle();
        CompiledPalette palette = context.palette();
        Map<String, String> corridorBlocks = cityStyle == null ? Map.of() : cityStyle.getCorridorBlocks();
        RailSettings railSettings = cityStyle == null ? null : cityStyle.getRailBlocks();

        Material roof = resolvePaletteCharMaterial(context, palette, corridorBlocks.get("roof"), Material.STONE_BRICKS);
        Material glass = resolvePaletteCharMaterial(context, palette, corridorBlocks.get("glass"), Material.GLASS);
        Material glow = resolvePaletteCharMaterial(context, palette, corridorBlocks.get("glowstone"), Material.GLOWSTONE);
        Material base = profile == null
                ? Material.STONE
                : context.resolveMaterial(profile.getBaseBlock(), Material.STONE);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean inCorridor = (xRail && z >= 7 && z <= 10) || (zRail && x >= 7 && x <= 10);
                if (inCorridor) {
                    context.setBlock(x, height, z, roof);

                    if (xRail && z == 10) {
                        setRail(context, x, height + 1, z, Rail.Shape.EAST_WEST);
                    } else if (zRail && x == 10) {
                        setRail(context, x, height + 1, z, Rail.Shape.NORTH_SOUTH);
                    } else {
                        context.setBlock(x, height + 1, z, Material.AIR);
                    }

                    context.setBlock(x, height + 2, z, Material.AIR);
                    context.setBlock(x, height + 3, z, Material.AIR);

                    boolean skylight = (xRail && x == 7 && (z == 8 || z == 9))
                            || (zRail && z == 7 && (x == 8 || x == 9));
                    if (skylight) {
                        context.setBlock(x, height + 4, z, glass);
                        context.setBlock(x, height + 5, z, glow);
                    } else {
                        context.setBlock(x, height + 4, z, roof);
                        context.setBlock(x, height + 5, z, roof);
                    }
                } else {
                    int from = Math.max(baseY - 5, minY);
                    int to = Math.min(info.getCityGroundLevel(), maxY);
                    for (int y = from; y <= to; y++) {
                        context.setBlock(x, y, z, base);
                    }
                }
            }
        }
        if (railSettings != null && railSettings.getRailMain() != null) {
            // Keep reading railmain to stay data-compatible with LC configs even when specific corridor rails are vanilla-shaped.
            resolvePaletteCharMaterial(context, palette, railSettings.getRailMain(), Material.RAIL);
        }
    }

    private void generateCorridorConnections(GenerationContext context, BuildingInfo info, int baseY) {
        int minY = Math.max(baseY - 5, context.getWorldInfo().getMinHeight());
        int maxY = Math.min(baseY - 2, context.getWorldInfo().getMaxHeight() - 1);
        if (minY > maxY) {
            return;
        }

        if (info.getXmin().hasXCorridor()) {
            clearEdgeOpeningX(context, 0, minY, maxY);
        }
        if (info.getXmax().hasXCorridor()) {
            clearEdgeOpeningX(context, 15, minY, maxY);
        }
        if (info.getZmin().hasZCorridor()) {
            clearEdgeOpeningZ(context, 0, minY, maxY);
        }
        if (info.getZmax().hasZCorridor()) {
            clearEdgeOpeningZ(context, 15, minY, maxY);
        }
    }

    private void clearEdgeOpeningX(GenerationContext context, int x, int minY, int maxY) {
        for (int z = 7; z <= 10; z++) {
            for (int y = minY; y <= maxY; y++) {
                context.setBlock(x, y, z, Material.AIR);
            }
        }
    }

    private void clearEdgeOpeningZ(GenerationContext context, int z, int minY, int maxY) {
        for (int x = 7; x <= 10; x++) {
            for (int y = minY; y <= maxY; y++) {
                context.setBlock(x, y, z, Material.AIR);
            }
        }
    }

    private Material resolvePaletteCharMaterial(GenerationContext context,
                                                CompiledPalette palette,
                                                String value,
                                                Material fallback) {
        if (value == null || value.isBlank() || palette == null) {
            return fallback;
        }
        String token = value.trim();
        char c = token.charAt(0);
        String definition = palette.get(c, context.getRandom());
        if (definition == null || definition.isBlank()) {
            return fallback;
        }
        Material material = context.resolveMaterial(definition.toLowerCase(Locale.ROOT), fallback);
        return material == null ? fallback : material;
    }

    private void setRail(GenerationContext context, int x, int y, int z, Rail.Shape shape) {
        context.setRail(x, y, z, Material.RAIL, shape, false);
    }
}
