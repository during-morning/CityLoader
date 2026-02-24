package com.during.cityloader.worldgen.gen;

import com.during.cityloader.season.Season;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.Condition;
import com.during.cityloader.worldgen.lost.cityassets.ConditionContext;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;

import java.util.Locale;
import java.util.Random;

/**
 * 后处理阶段（照明、藤蔓、随机树叶等）
 */
public class PostProcessStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (info.hasBuilding) {
            generateLighting(context, info);
            
            if (profile != null && profile.getVineChance() > 0) {
                generateVines(context, profile);
            }
            
            if (profile != null && profile.getChanceOfRandomLeafBlocks() > 0) {
                generateRandomLeafBlocks(context, profile);
            }
        }
        executePalettePostTodo(context, info);
        executePostTodo(info);
        removeAllSpawners(context, info);
        repairPaneConnectivity(context, info);
        cleanupGroundVegetation(context, info);
    }

    private void executePalettePostTodo(GenerationContext context, BuildingInfo info) {
        for (BuildingInfo.PalettePostTodo todo : info.drainPalettePostTodo()) {
            try {
                if (todo.torch()) {
                    context.setTorchWithSupport(todo.x(), todo.y(), todo.z());
                }
                if (todo.loot() != null) {
                    applyLootTodo(context, todo);
                }
                if (todo.mob() != null) {
                    applyMobTodo(context, todo);
                }
                if (!todo.tag().isEmpty()) {
                    applyTagTodo(context, todo);
                }
            } catch (Exception ignored) {
                // Keep chunk generation resilient: one failing palette todo should not abort stage execution.
            }
        }
    }

    private void applyLootTodo(GenerationContext context, BuildingInfo.PalettePostTodo todo) {
        Material current = context.getBlockType(todo.x(), todo.y(), todo.z());
        if (current == Material.CHEST || current == Material.TRAPPED_CHEST || current == Material.BARREL) {
            LostCityProfile profile = context.getDimensionInfo().getProfile();
            if (profile != null) {
                if (!profile.isGenerateLoot()) {
                    return;
                }
            }
            String lootTable = resolveConditionValue(context, todo.loot(), todo);
            if (lootTable != null && !lootTable.isBlank()) {
                context.queueLootTable(todo.x(), todo.y(), todo.z(), lootTable);
            }
        }
    }

    private void applyMobTodo(GenerationContext context, BuildingInfo.PalettePostTodo todo) {
        // 明确禁用刷怪笼配置注入
        return;
        /*
        Material current = context.getBlockType(todo.x(), todo.y(), todo.z());
        if (current == Material.SPAWNER) {
            LostCityProfile profile = context.getDimensionInfo().getProfile();
            if (profile != null && !profile.isGenerateSpawners()) {
                return;
            }
            String mobId = resolveConditionValue(context, todo.mob(), todo);
            if (mobId != null && !mobId.isBlank()) {
                context.queueSpawnerMob(todo.x(), todo.y(), todo.z(), mobId);
            }
        }
        */
    }

    private void removeAllSpawners(GenerationContext context, BuildingInfo info) {
        if (info == null || !info.isCity) {
            return;
        }
        int minY = Math.max(context.getWorldInfo().getMinHeight(), info.getCityGroundLevel() - 16);
        int maxY = Math.min(context.getWorldInfo().getMaxHeight() - 1, info.getMaxHeight() + 16);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (context.getBlockType(x, y, z) == Material.SPAWNER) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
            }
        }
    }

    private void repairPaneConnectivity(GenerationContext context, BuildingInfo info) {
        if (info == null || !info.isCity) {
            return;
        }
        int minY = Math.max(context.getWorldInfo().getMinHeight(), info.getCityGroundLevel() - 20);
        int maxY = Math.min(context.getWorldInfo().getMaxHeight() - 1, info.getMaxHeight() + 24);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Material current = context.getBlockType(x, y, z);
                    if (!isPaneLike(current)) {
                        continue;
                    }
                    boolean north = shouldConnectPane(context, x, y, z - 1);
                    boolean south = shouldConnectPane(context, x, y, z + 1);
                    boolean west = shouldConnectPane(context, x - 1, y, z);
                    boolean east = shouldConnectPane(context, x + 1, y, z);
                    String id = current.name().toLowerCase(Locale.ROOT);
                    String definition = "minecraft:" + id
                            + "[north=" + north
                            + ",south=" + south
                            + ",west=" + west
                            + ",east=" + east
                            + ",waterlogged=false]";
                    context.setBlock(x, y, z, definition);
                }
            }
        }
    }

    private boolean isPaneLike(Material material) {
        if (material == null) {
            return false;
        }
        return material == Material.IRON_BARS || material.name().endsWith("_PANE");
    }

    private boolean shouldConnectPane(GenerationContext context, int x, int y, int z) {
        Material neighbor = context.getBlockType(x, y, z);
        if (neighbor == null || isAirLike(neighbor)) {
            return false;
        }
        if (isPaneLike(neighbor)) {
            return true;
        }
        String name = neighbor.name();
        if (name.contains("GLASS")) {
            return true;
        }
        return neighbor.isOccluding() || name.endsWith("_WALL") || name.endsWith("_FENCE");
    }

    private void applyTagTodo(GenerationContext context, BuildingInfo.PalettePostTodo todo) {
        context.queueBlockEntityTag(todo.x(), todo.y(), todo.z(), todo.tag());
    }

    private String resolveConditionValue(GenerationContext context,
                                         BuildingInfo.ConditionTodo conditionTodo,
                                         BuildingInfo.PalettePostTodo todo) {
        if (conditionTodo == null || conditionTodo.condition() == null || conditionTodo.condition().isBlank()) {
            return null;
        }

        String raw = conditionTodo.condition().trim();
        Condition condition = AssetRegistries.CONDITIONS.get(context.getDimensionInfo().getWorld(), raw);
        if (condition == null) {
            return raw;
        }

        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        int level = profile == null ? 0 : (todo.y() - profile.getGroundLevel()) / FLOOR_HEIGHT;
        int floor = (todo.y() - info.getCityGroundLevel()) / FLOOR_HEIGHT;
        String part = conditionTodo.part() == null || conditionTodo.part().isBlank() ? "<none>" : conditionTodo.part();
        String building = conditionTodo.building() == null || conditionTodo.building().isBlank()
                ? "<none>"
                : conditionTodo.building();

        ConditionContext conditionContext = new PaletteConditionContext(
                level,
                floor,
                info.cellars,
                info.floors,
                part,
                "<none>",
                building,
                info.coord,
                resolveBiomeId(context, todo));

        return condition.getRandomValue(context.getRandom(), conditionContext);
    }

    private String resolveBiomeId(GenerationContext context, BuildingInfo.PalettePostTodo todo) {
        Biome biome = context.getDimensionInfo().getBiome(
                context.worldX(todo.x()),
                todo.y(),
                context.worldZ(todo.z()));
        if (biome == null) {
            return "minecraft:plains";
        }
        return "minecraft:" + biome.name().toLowerCase(Locale.ROOT);
    }

    private void executePostTodo(BuildingInfo info) {
        for (Runnable task : info.drainPostTodo()) {
            try {
                task.run();
            } catch (Exception ignored) {
                // Keep chunk generation resilient: one failing todo should not abort the entire stage.
            }
        }
    }

    private void generateLighting(GenerationContext context, BuildingInfo info) {
        for (int i = 0; i < info.floors; i++) {
            int y = info.getCityGroundLevel() + i * FLOOR_HEIGHT + 2;
            placeTorches(context, y);
        }
        
        for (int i = 1; i <= info.cellars; i++) {
            int y = info.getCityGroundLevel() - i * FLOOR_HEIGHT + 2;
            placeTorches(context, y);
        }
    }

    private void generateVines(GenerationContext context, LostCityProfile profile) {
        Random random = context.getRandom();
        BuildingInfo info = context.getBuildingInfo();
        float vineChance = seasonAdjustedVineChance(profile.getVineChance(), context.getSeason());
        if (vineChance <= 0.0f) {
            return;
        }

        int cityGroundY = info.getCityGroundLevel();
        int maxY = cityGroundY + info.floors * FLOOR_HEIGHT;

        generateBoundaryVineStrips(context, info, vineChance, random);
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = cityGroundY; y < maxY; y++) {
                    if (random.nextFloat() > vineChance) {
                        continue;
                    }
                    
                    Material current = context.getBlockType(x, y, z);
                    if (current != Material.AIR && current != Material.CAVE_AIR) {
                        continue;
                    }
                    
                    if (tryPlaceVine(context, x, y, z)) {
                        break;
                    }
                }
            }
        }
    }

    private void generateBoundaryVineStrips(GenerationContext context, BuildingInfo info, float vineChance, Random random) {
        if (!info.hasBuilding) {
            return;
        }
        int thisTop = info.getMaxHeight();
        if (thisTop <= info.getCityGroundLevel() + 3) {
            return;
        }

        createEastSideVines(context, info, thisTop, vineChance, random);
        createWestSideVines(context, info, thisTop, vineChance, random);
        createSouthSideVines(context, info, thisTop, vineChance, random);
        createNorthSideVines(context, info, thisTop, vineChance, random);
    }

    private void createEastSideVines(GenerationContext context, BuildingInfo info, int thisTop, float vineChance, Random random) {
        BuildingInfo east = info.getXmax();
        int neighborBottom = east.getCityGroundLevel() + 3;
        int neighborTop = east.hasBuilding ? east.getMaxHeight() : neighborBottom;

        int bottomFromThis = Math.max(neighborBottom, neighborTop);
        int bottomFromNeighbor = Math.max(info.getCityGroundLevel() + 3, thisTop);

        createVineStripPlaneX(context, 14, 0, 15, bottomFromThis, thisTop, BlockFace.EAST, vineChance, random);
        if (east.hasBuilding) {
            createVineStripPlaneX(context, 15, 0, 15, bottomFromNeighbor, neighborTop, BlockFace.WEST, vineChance, random);
        }
    }

    private void createWestSideVines(GenerationContext context, BuildingInfo info, int thisTop, float vineChance, Random random) {
        BuildingInfo west = info.getXmin();
        int neighborBottom = west.getCityGroundLevel() + 3;
        int neighborTop = west.hasBuilding ? west.getMaxHeight() : neighborBottom;

        int bottomFromThis = Math.max(neighborBottom, neighborTop);
        int bottomFromNeighbor = Math.max(info.getCityGroundLevel() + 3, thisTop);

        createVineStripPlaneX(context, 1, 0, 15, bottomFromThis, thisTop, BlockFace.WEST, vineChance, random);
        if (west.hasBuilding) {
            createVineStripPlaneX(context, 0, 0, 15, bottomFromNeighbor, neighborTop, BlockFace.EAST, vineChance, random);
        }
    }

    private void createSouthSideVines(GenerationContext context, BuildingInfo info, int thisTop, float vineChance, Random random) {
        BuildingInfo south = info.getZmax();
        int neighborBottom = south.getCityGroundLevel() + 3;
        int neighborTop = south.hasBuilding ? south.getMaxHeight() : neighborBottom;

        int bottomFromThis = Math.max(neighborBottom, neighborTop);
        int bottomFromNeighbor = Math.max(info.getCityGroundLevel() + 3, thisTop);

        createVineStripPlaneZ(context, 14, 0, 15, bottomFromThis, thisTop, BlockFace.SOUTH, vineChance, random);
        if (south.hasBuilding) {
            createVineStripPlaneZ(context, 15, 0, 15, bottomFromNeighbor, neighborTop, BlockFace.NORTH, vineChance, random);
        }
    }

    private void createNorthSideVines(GenerationContext context, BuildingInfo info, int thisTop, float vineChance, Random random) {
        BuildingInfo north = info.getZmin();
        int neighborBottom = north.getCityGroundLevel() + 3;
        int neighborTop = north.hasBuilding ? north.getMaxHeight() : neighborBottom;

        int bottomFromThis = Math.max(neighborBottom, neighborTop);
        int bottomFromNeighbor = Math.max(info.getCityGroundLevel() + 3, thisTop);

        createVineStripPlaneZ(context, 1, 0, 15, bottomFromThis, thisTop, BlockFace.NORTH, vineChance, random);
        if (north.hasBuilding) {
            createVineStripPlaneZ(context, 0, 0, 15, bottomFromNeighbor, neighborTop, BlockFace.SOUTH, vineChance, random);
        }
    }

    private void createVineStripPlaneX(GenerationContext context,
                                       int x,
                                       int minZ,
                                       int maxZExclusive,
                                       int bottom,
                                       int topExclusive,
                                       BlockFace supportFace,
                                       float chance,
                                       Random random) {
        if (x < 0 || x > 15 || bottom >= topExclusive) {
            return;
        }
        int maxY = Math.min(topExclusive, context.getWorldInfo().getMaxHeight());
        int minY = Math.max(bottom, context.getWorldInfo().getMinHeight());
        for (int z = minZ; z < maxZExclusive; z++) {
            for (int y = minY; y < maxY; y++) {
                if (random.nextFloat() < chance) {
                    createVineStrip(context, x, y, z, minY, supportFace, random);
                }
            }
        }
    }

    private void createVineStripPlaneZ(GenerationContext context,
                                       int z,
                                       int minX,
                                       int maxXExclusive,
                                       int bottom,
                                       int topExclusive,
                                       BlockFace supportFace,
                                       float chance,
                                       Random random) {
        if (z < 0 || z > 15 || bottom >= topExclusive) {
            return;
        }
        int maxY = Math.min(topExclusive, context.getWorldInfo().getMaxHeight());
        int minY = Math.max(bottom, context.getWorldInfo().getMinHeight());
        for (int x = minX; x < maxXExclusive; x++) {
            for (int y = minY; y < maxY; y++) {
                if (random.nextFloat() < chance) {
                    createVineStrip(context, x, y, z, minY, supportFace, random);
                }
            }
        }
    }

    private void createVineStrip(GenerationContext context,
                                 int x,
                                 int y,
                                 int z,
                                 int bottom,
                                 BlockFace supportFace,
                                 Random random) {
        if (!canPlaceVineAt(context, x, y, z, supportFace)) {
            return;
        }
        if (!context.setVine(x, y, z, supportFace)) {
            return;
        }

        int currentY = y - 1;
        while (currentY >= bottom && random.nextFloat() < 0.8f) {
            if (!canPlaceVineAt(context, x, currentY, z, supportFace)) {
                break;
            }
            if (!context.setVine(x, currentY, z, supportFace)) {
                break;
            }
            currentY--;
        }
    }

    private boolean canPlaceVineAt(GenerationContext context, int x, int y, int z, BlockFace supportFace) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return false;
        }
        Material current = context.getBlockType(x, y, z);
        if (!isAirLike(current)) {
            return false;
        }
        int supportX = x + supportFace.getModX();
        int supportZ = z + supportFace.getModZ();
        if (supportX < 0 || supportX > 15 || supportZ < 0 || supportZ > 15) {
            return false;
        }
        Material support = context.getBlockType(supportX, y, supportZ);
        return isSolidSupport(support);
    }

    private boolean tryPlaceVine(GenerationContext context, int x, int y, int z) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : faces) {
            int checkX = x + face.getModX();
            int checkZ = z + face.getModZ();
            
            if (checkX < 0 || checkX >= 16 || checkZ < 0 || checkZ >= 16) {
                continue;
            }
            
            Material adjacent = context.getBlockType(checkX, y, checkZ);
            if (isSolidSupport(adjacent)) {
                if (context.setVine(x, y, z, face)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void generateRandomLeafBlocks(GenerationContext context, LostCityProfile profile) {
        Random random = context.getRandom();
        float leafChance = seasonAdjustedLeafChance(profile.getChanceOfRandomLeafBlocks(), context.getSeason());
        if (leafChance <= 0.0f) {
            return;
        }
        int thickness = profile.getThicknessOfRandomLeafBlocks();
        if (thickness <= 0) {
            return;
        }

        int roofBaseY = context.getBuildingInfo().getMaxHeight();
        int startY = Math.max(context.getWorldInfo().getMinHeight() + 1, roofBaseY + 1);
        int maxY = context.getWorldInfo().getMaxHeight() - 1;
        if (startY > maxY) {
            return;
        }
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (random.nextFloat() > leafChance) {
                    continue;
                }

                for (int t = 0; t < thickness; t++) {
                    int y = startY + t;
                    if (y > maxY) {
                        break;
                    }

                    Material below = context.getBlockType(x, y - 1, z);
                    if (!isSolidSupport(below)) {
                        break;
                    }
                    Material existing = context.getBlockType(x, y, z);
                    if (!isAirLike(existing)) {
                        break;
                    }

                    Material leafType = seasonalLeafType(random, context.getSeason());
                    context.setBlock(x, y, z, leafType);
                }
            }
        }
    }

    private void cleanupGroundVegetation(GenerationContext context, BuildingInfo info) {
        if (info == null) {
            return;
        }
        if (!info.isCity) {
            return;
        }
        int minY = context.getWorldInfo().getMinHeight();
        int floorY = info.getCityGroundLevel();
        int fromY = Math.max(minY, floorY - 8);
        int toY = Math.min(context.getWorldInfo().getMaxHeight() - 1, floorY + 2);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = fromY; y <= toY; y++) {
                    Material current = context.getBlockType(x, y, z);
                    if (!isGroundPollutingVegetation(current)) {
                        continue;
                    }
                    if (y <= floorY) {
                        context.setBlock(x, y, z, Material.AIR);
                        continue;
                    }
                    Material below = context.getBlockType(x, y - 1, z);
                    if (!isSolidSupport(below)) {
                        context.setBlock(x, y, z, Material.AIR);
                    }
                }
                Material floor = context.getBlockType(x, floorY, z);
                if (info.hasBuilding && isAirLike(floor)) {
                    context.setBlock(x, floorY, z, pickRuinFloorMaterial(context, x, z, floorY));
                }
            }
        }
    }

    private boolean isGroundPollutingVegetation(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.equals("VINE")
                || name.equals("MOSS_BLOCK")
                || name.equals("MOSS_CARPET")
                || name.contains("GRASS")
                || name.contains("FERN")
                || name.contains("SEAGRASS")
                || name.startsWith("KELP")
                || name.endsWith("_SAPLING")
                || name.endsWith("_LEAVES");
    }

    private Material pickRuinFloorMaterial(GenerationContext context, int x, int z, int y) {
        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) context.getChunkX() * 341873128712L;
        hash ^= (long) context.getChunkZ() * 132897987541L;
        hash ^= (long) x * 0x517cc1b727220a95L;
        hash ^= (long) z * 0x94d049bb133111ebL;
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

    private void placeTorches(GenerationContext context, int y) {
        tryPlaceTorch(context, 3, y, 3);
        tryPlaceTorch(context, 12, y, 3);
        tryPlaceTorch(context, 3, y, 12);
        tryPlaceTorch(context, 12, y, 12);
    }

    private void tryPlaceTorch(GenerationContext context, int x, int y, int z) {
        context.setTorchWithSupport(x, y, z);
    }

    private float seasonAdjustedVineChance(float base, Season season) {
        float factor = switch (season) {
            case SPRING -> 1.2f;
            case SUMMER -> 1.0f;
            case AUTUMN -> 0.7f;
            case WINTER -> 0.2f;
        };
        return clamp01(base * factor);
    }

    private float seasonAdjustedLeafChance(float base, Season season) {
        float factor = switch (season) {
            case SPRING -> 1.1f;
            case SUMMER -> 1.0f;
            case AUTUMN -> 1.35f;
            case WINTER -> 0.35f;
        };
        return clamp01(base * factor);
    }

    private Material seasonalLeafType(Random random, Season season) {
        return switch (season) {
            case SPRING -> random.nextBoolean() ? Material.BIRCH_LEAVES : Material.OAK_LEAVES;
            case SUMMER -> random.nextBoolean() ? Material.JUNGLE_LEAVES : Material.OAK_LEAVES;
            case AUTUMN -> random.nextBoolean() ? Material.ACACIA_LEAVES : Material.DARK_OAK_LEAVES;
            case WINTER -> Material.SPRUCE_LEAVES;
        };
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private boolean isAirLike(Material material) {
        return material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private boolean isSolidSupport(Material material) {
        if (material == null || material == Material.BARRIER || isAirLike(material)) {
            return false;
        }
        try {
            return material.isSolid();
        } catch (Throwable ignored) {
            // Unit-test environments may not bootstrap full registry access.
            return true;
        }
    }

    private static final class PaletteConditionContext extends ConditionContext {
        private final String biome;

        private PaletteConditionContext(int level,
                                        int floor,
                                        int floorsBelowGround,
                                        int floorsAboveGround,
                                        String part,
                                        String belowPart,
                                        String building,
                                        com.during.cityloader.util.ChunkCoord coord,
                                        String biome) {
            super(level, floor, floorsBelowGround, floorsAboveGround, part, belowPart, building, coord);
            this.biome = biome;
        }

        @Override
        public boolean isSphere() {
            return false;
        }

        @Override
        public ResourceLocation getBiome() {
            return new ResourceLocation(biome);
        }
    }
}
