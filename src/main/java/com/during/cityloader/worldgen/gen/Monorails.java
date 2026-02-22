package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.regassets.data.MonorailParts;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 单轨列车生成器
 * 负责在城市球体之间生成单轨连接
 */
public class Monorails {

    /**
     * 检测指定坐标是否有水平单轨
     * 当此chunk在网格中心且左右城市都需要单轨时生成
     */
    public static boolean hasHorizontalMonorail(ChunkCoord coord, IDimensionInfo provider) {
        LostCityProfile profile = provider.getProfile();
        
        // 检查是否是网格中心
        if (!isGridCenter(coord.chunkX()) || !isGridCenter(coord.chunkZ())) {
            return false;
        }
        
        // 检查左右是否有城市需要单轨
        boolean leftWants = cityWantsMonorail(coord.west(), provider);
        boolean rightWants = cityWantsMonorail(coord.east(), provider);
        
        return leftWants && rightWants;
    }

    /**
     * 检测指定坐标是否有垂直单轨
     */
    public static boolean hasVerticalMonorail(ChunkCoord coord, IDimensionInfo provider) {
        LostCityProfile profile = provider.getProfile();
        
        if (!isGridCenter(coord.chunkX()) || !isGridCenter(coord.chunkZ())) {
            return false;
        }
        
        boolean northWants = cityWantsMonorail(coord.north(), provider);
        boolean southWants = cityWantsMonorail(coord.south(), provider);
        
        return northWants && southWants;
    }

    /**
     * 检测指定坐标是否有单轨站
     */
    public static boolean hasMonorailStation(ChunkCoord coord, IDimensionInfo provider) {
        LostCityProfile profile = provider.getProfile();
        
        // 检查附近是否有非封闭的单轨
        return hasHorizontalMonorail(coord.south(), provider) ||
               hasHorizontalMonorail(coord.north(), provider) ||
               hasVerticalMonorail(coord.east(), provider) ||
               hasVerticalMonorail(coord.west(), provider);
    }

    private static boolean isGridCenter(int chunkCoord) {
        return chunkCoord % 16 == 0;
    }

    private static boolean cityWantsMonorail(ChunkCoord coord, IDimensionInfo provider) {
        // 检查该位置是否是城市球体中心
        // 需要检测是否是球体中心以及是否有单轨连接意愿
        // 这里简化为检查是否是城市
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        return info.isCity;
    }

    /**
     * 生成单轨列车
     */
    public static void generateMonorails(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        IDimensionInfo provider = context.getDimensionInfo();
        LostCityProfile profile = provider.getProfile();
        
        // 检查是否在城市球体模式
        if (!profile.getLandscapeType().equals("space") && 
            !profile.getLandscapeType().equals("spheres") &&
            !profile.getLandscapeType().equals("cavernspheres")) {
            return;
        }
        
        // 检查是否是城市
        if (!info.isCity) {
            return;
        }
        
        // 完全在球体内的chunk不生成单轨（单轨在球体边缘）
        if (fullyInsideCitySphere(info.coord, provider)) {
            return;
        }
        
        boolean horizontal = info.hasHorizontalMonorail();
        boolean vertical = info.hasVerticalMonorail();
        
        if (!horizontal && !vertical) {
            return;
        }
        
        int heightOffset = profile.getCitySphereMonorailHeightOffset();
        int baseHeight = profile.getGroundLevel() + heightOffset;
        
        if (horizontal) {
            generateHorizontalMonorail(context, baseHeight);
        }
        
        if (vertical) {
            generateVerticalMonorail(context, baseHeight);
        }
        
        // 生成单轨站
        if (hasMonorailStation(info.coord, provider)) {
            generateMonorailStation(context, baseHeight);
        }
    }

    private static void generateHorizontalMonorail(GenerationContext context, int baseHeight) {
        // 默认实现：简单的轨道
        for (int x = 0; x < 16; x++) {
            context.setRail(x, baseHeight, 7, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setRail(x, baseHeight, 8, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setBlock(x, baseHeight - 1, 7, Material.IRON_BLOCK);
            context.setBlock(x, baseHeight - 1, 8, Material.IRON_BLOCK);
        }
    }

    private static void generateVerticalMonorail(GenerationContext context, int baseHeight) {
        // 默认实现
        for (int z = 0; z < 16; z++) {
            context.setRail(7, baseHeight, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setRail(8, baseHeight, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setBlock(7, baseHeight - 1, z, Material.IRON_BLOCK);
            context.setBlock(8, baseHeight - 1, z, Material.IRON_BLOCK);
        }
    }

    private static void generateMonorailStation(GenerationContext context, int baseHeight) {
        // 默认实现：创建一个简单的车站平台
        for (int x = 4; x < 12; x++) {
            for (int z = 4; z < 12; z++) {
                context.setBlock(x, baseHeight - 1, z, Material.SMOOTH_STONE);
                context.setBlock(x, baseHeight, z, Material.AIR);
            }
        }
        
        // 添加栏杆
        for (int x = 4; x < 12; x++) {
            context.setBlock(x, baseHeight + 1, 4, Material.IRON_BARS);
            context.setBlock(x, baseHeight + 1, 11, Material.IRON_BARS);
        }
        for (int z = 4; z < 12; z++) {
            context.setBlock(4, baseHeight + 1, z, Material.IRON_BARS);
            context.setBlock(11, baseHeight + 1, z, Material.IRON_BARS);
        }
    }

    private static boolean fullyInsideCitySphere(ChunkCoord coord, IDimensionInfo provider) {
        // 检查是否完全在球体内
        // 需要通过球体算法确定
        LostCityProfile profile = provider.getProfile();
        
        int gridSize = profile.isCitySphere32Grid() ? 32 : 16;
        int gridX = Math.floorDiv(coord.chunkX(), gridSize);
        int gridZ = Math.floorDiv(coord.chunkZ(), gridSize);
        
        Random gridRandom = gridRandom(provider.getSeed(), gridX, gridZ);
        float chance = profile.getCitySphereChance();
        
        if (gridRandom.nextFloat() >= chance) {
            return false;
        }
        
        int sphereCenterX = gridX * gridSize + gridSize / 2;
        int sphereCenterZ = gridZ * gridSize + gridSize / 2;
        int sphereRadius = 8 + gridRandom.nextInt(8);
        
        double distanceFromCenter = Math.sqrt(
            Math.pow(coord.chunkX() - sphereCenterX, 2) + 
            Math.pow(coord.chunkZ() - sphereCenterZ, 2)
        );
        
        // 距离中心小于半径的80%认为是完全在球体内
        return distanceFromCenter < sphereRadius * 0.8;
    }

    private static Random gridRandom(long worldSeed, int gridX, int gridZ) {
        long seed = worldSeed;
        seed ^= (long) gridX * 341873128712L;
        seed ^= (long) gridZ * 132897987541L;
        seed ^= 0x7FFFFFFFFFFFFFFFL;
        return new Random(seed);
    }

    private static void renderPart(GenerationContext context,
                                    BuildingPart part,
                                    int baseY,
                                    boolean overlay,
                                    Transform transform) {
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
                }
            }
        }
    }

    private static CompiledPalette composePalette(GenerationContext context, BuildingPart part) {
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

    private static String resolveFromPaletteString(GenerationContext context,
                                                    CompiledPalette palette,
                                                    char token) {
        return palette.get(token, context.getRandom());
    }

    private static ResourceLocation resolveLocation(ResourceLocation owner, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return new ResourceLocation(normalized);
        }
        return new ResourceLocation(owner.getNamespace(), normalized);
    }
}
