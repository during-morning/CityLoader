package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.Building;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.ConditionContext;
import com.during.cityloader.worldgen.lost.cityassets.MultiBuilding;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.cityassets.ScatteredBuilding;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.data.BiomeMatcher;
import com.during.cityloader.worldgen.lost.regassets.data.ScatteredSelector;
import com.during.cityloader.worldgen.lost.regassets.data.ScatteredSettings;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class ScatteredStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;
    private static final long SCATTERED_GRID_SALT = 0x61C8864680B583EBL;

    @Override
    public void generate(GenerationContext context) {
        if (!context.getDimensionInfo().getProfile().isScatteredEnabled()) {
            return;
        }
        if (context.isCity()) {
            return;
        }

        WorldStyle worldStyle = context.getDimensionInfo().getWorldStyle();
        if (worldStyle == null) {
            return;
        }
        ScatteredSettings settings = worldStyle.getScatteredSettings();
        if (settings == null) {
            return;
        }

        int areaSize = settings.getAreaSize() == null ? 20 : Math.max(1, settings.getAreaSize());
        int chunkX = context.getChunkX();
        int chunkZ = context.getChunkZ();
        int gridX = Math.floorDiv(chunkX, areaSize);
        int gridZ = Math.floorDiv(chunkZ, areaSize);
        int gridStartX = gridX * areaSize;
        int gridStartZ = gridZ * areaSize;

        Random gridRandom = gridRandom(context.getDimensionInfo().getSeed(), gridX, gridZ);
        float chance = settings.getChance() == null ? 0.0f : settings.getChance();
        if (chance <= 0.0f || gridRandom.nextFloat() >= chance) {
            return;
        }

        List<ScatteredCandidate> candidates = collectCandidates(
                context,
                worldStyle,
                settings,
                areaSize,
                gridX,
                gridZ,
                gridStartX,
                gridStartZ);
        ScatteredCandidate selected = pickScatteredCandidate(settings, candidates, gridRandom);
        if (selected == null) {
            return;
        }

        ScatteredBuilding scattered = selected.scattered();
        if (gridRandom.nextFloat() >= Math.max(0.0f, scattered.getChance())) {
            return;
        }

        Building building = resolveBuildingForChunk(
                context,
                selected.anchorChunkX(),
                selected.anchorChunkZ(),
                scattered,
                gridRandom);
        if (building == null) {
            return;
        }

        int baseY = resolveTerrainHeight(scattered.getTerrainHeight(), selected.anchorStats(), context.getBuildingInfo().waterLevel)
                + scattered.getHeightOffset();
        generateBuilding(context, building, scattered.getTerrainFix(), baseY, selected.biomeName(), gridRandom);
    }

    private ScatteredBuilding resolveScattered(GenerationContext context, WorldStyle worldStyle, String selectedName) {
        if (selectedName == null || selectedName.isBlank()) {
            return null;
        }

        World world = context.getDimensionInfo().getWorld();
        ScatteredBuilding scattered = null;
        if (worldStyle != null) {
            scattered = AssetRegistries.SCATTERED.get(world, resolveLocation(worldStyle.getId(), selectedName));
        }
        if (scattered == null) {
            scattered = AssetRegistries.SCATTERED.get(world, new ResourceLocation(selectedName.toLowerCase(Locale.ROOT)));
        }
        return scattered;
    }

    private List<ScatteredCandidate> collectCandidates(GenerationContext context,
                                                       WorldStyle worldStyle,
                                                       ScatteredSettings settings,
                                                       int areaSize,
                                                       int gridX,
                                                       int gridZ,
                                                       int gridStartX,
                                                       int gridStartZ) {
        List<ScatteredCandidate> candidates = new ArrayList<>();
        if (settings.getList() == null || settings.getList().isEmpty()) {
            return candidates;
        }

        for (ScatteredSelector selector : settings.getList()) {
            if (selector == null || selector.getName() == null || selector.getName().isBlank()) {
                continue;
            }

            ScatteredBuilding scattered = resolveScattered(context, worldStyle, selector.getName());
            if (scattered == null) {
                continue;
            }

            StructureSize size = resolveStructureSize(context, scattered);
            if (size == null || size.dimX() > areaSize || size.dimZ() > areaSize) {
                continue;
            }

            int minAnchorX = gridStartX;
            int maxAnchorX = gridStartX + areaSize - size.dimX();
            int minAnchorZ = gridStartZ;
            int maxAnchorZ = gridStartZ + areaSize - size.dimZ();

            Random anchorRandom = gridRandom(
                    context.getDimensionInfo().getSeed(),
                    gridX,
                    gridZ,
                    saltFrom(selector.getName()));
            int anchorChunkX = minAnchorX + boundedRandom(anchorRandom, 0, Math.max(0, maxAnchorX - minAnchorX));
            int anchorChunkZ = minAnchorZ + boundedRandom(anchorRandom, 0, Math.max(0, maxAnchorZ - minAnchorZ));

            HeightStats anchorStats = calculateHeightStats(context.getDimensionInfo(), anchorChunkX, anchorChunkZ);
            if (selector.getMaxHeightDiff() != null && anchorStats.diff() > selector.getMaxHeightDiff()) {
                continue;
            }

            String biomeName = getBiomeName(context.getDimensionInfo(), anchorChunkX, anchorChunkZ, anchorStats.average());
            if (!matchesBiome(selector.getBiomes(), biomeName)) {
                continue;
            }

            int weight = Math.max(1, selector.getWeight());
            candidates.add(new ScatteredCandidate(selector, scattered, anchorChunkX, anchorChunkZ, anchorStats, biomeName, weight));
        }

        return candidates;
    }

    private ScatteredCandidate pickScatteredCandidate(ScatteredSettings settings,
                                                      List<ScatteredCandidate> candidates,
                                                      Random random) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (ScatteredCandidate candidate : candidates) {
            totalWeight += candidate.weight();
        }

        int noneWeight = settings.getWeightNone() == null ? 0 : Math.max(0, settings.getWeightNone());
        int total = totalWeight + noneWeight;
        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);
        if (roll < noneWeight) {
            return null;
        }
        roll -= noneWeight;

        int cursor = 0;
        for (ScatteredCandidate candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private StructureSize resolveStructureSize(GenerationContext context, ScatteredBuilding scattered) {
        if (!scattered.hasMultiBuilding()) {
            return new StructureSize(1, 1);
        }
        MultiBuilding multi = AssetRegistries.MULTI_BUILDINGS.get(
                context.getDimensionInfo().getWorld(),
                resolveLocation(scattered.getId(), scattered.getMultiBuilding()));
        if (multi == null) {
            return null;
        }
        return new StructureSize(Math.max(1, multi.getDimX()), Math.max(1, multi.getDimZ()));
    }

    private Building resolveBuildingForChunk(GenerationContext context,
                                             int anchorChunkX,
                                             int anchorChunkZ,
                                             ScatteredBuilding scattered,
                                             Random random) {
        if (scattered.hasMultiBuilding()) {
            return resolveFromMultiBuilding(context, anchorChunkX, anchorChunkZ, scattered);
        }
        if (context.getChunkX() != anchorChunkX || context.getChunkZ() != anchorChunkZ) {
            return null;
        }
        if (!scattered.hasSingleBuilding()) {
            return null;
        }

        String buildingName = scattered.pickBuilding(random);
        if (buildingName == null || buildingName.isBlank()) {
            return null;
        }
        return AssetRegistries.BUILDINGS.get(
                context.getDimensionInfo().getWorld(),
                resolveLocation(scattered.getId(), buildingName));
    }

    private Building resolveFromMultiBuilding(GenerationContext context,
                                              int anchorChunkX,
                                              int anchorChunkZ,
                                              ScatteredBuilding scattered) {
        MultiBuilding multi = AssetRegistries.MULTI_BUILDINGS.get(
                context.getDimensionInfo().getWorld(),
                resolveLocation(scattered.getId(), scattered.getMultiBuilding()));
        if (multi == null) {
            return null;
        }

        int relX = context.getChunkX() - anchorChunkX;
        int relZ = context.getChunkZ() - anchorChunkZ;
        if (relX < 0 || relZ < 0 || relX >= multi.getDimX() || relZ >= multi.getDimZ()) {
            return null;
        }

        String buildingName = multi.getBuildingAt(relX, relZ);
        if (buildingName == null || buildingName.isBlank()) {
            return null;
        }
        return AssetRegistries.BUILDINGS.get(
                context.getDimensionInfo().getWorld(),
                resolveLocation(multi.getId(), buildingName));
    }

    private void generateBuilding(GenerationContext context,
                                  Building building,
                                  String terrainFix,
                                  int baseY,
                                  String biomeName,
                                  Random random) {
        int minFloors = clampFloor(building.getMinFloors(), 1);
        int maxFloors = clampFloor(building.getMaxFloors(), minFloors);
        int minCellars = clampFloor(building.getMinCellars(), 0);
        int maxCellars = clampFloor(building.getMaxCellars(), minCellars);

        int floors = boundedRandom(random, minFloors, maxFloors);
        int cellars = boundedRandom(random, minCellars, maxCellars);

        CompiledPalette buildingPalette = composeBuildingPalette(context, building);
        String belowPart1 = "<none>";
        String belowPart2 = "<none>";
        TerrainFixMode terrainFixMode = TerrainFixMode.from(terrainFix);

        for (int floor = -cellars; floor < floors; floor++) {
            ConditionContext conditionContext = new ScatteredConditionContext(
                    floor + cellars,
                    floor,
                    cellars,
                    floors,
                    "<none>",
                    belowPart1,
                    building.getName(),
                    new ChunkCoord(context.getWorldInfo().getName(), context.getChunkX(), context.getChunkZ()),
                    biomeName);

            Building.PartSelection partSelection = building.getRandomPartRef(random, conditionContext);
            Building.PartSelection part2Selection = building.getRandomPart2Ref(random, conditionContext);
            String partName = partSelection == null ? null : partSelection.partName();
            String part2Name = part2Selection == null ? null : part2Selection.partName();
            Transform transform = partSelection == null ? Transform.ROTATE_NONE : partSelection.transform();
            Transform transform2 = part2Selection == null ? Transform.ROTATE_NONE : part2Selection.transform();

            BuildingPart part = resolvePart(context, building.getId(), partName);
            BuildingPart part2 = resolvePart(context, building.getId(), part2Name);
            int floorY = baseY + floor * FLOOR_HEIGHT;
            TerrainFixMode modeForFloor = floor == -cellars ? terrainFixMode : TerrainFixMode.NONE;

            renderPart(context, part, composePartPalette(context, buildingPalette, part), floorY, false, transform, modeForFloor);
            renderPart(context, part2, composePartPalette(context, buildingPalette, part2), floorY, true, transform2, TerrainFixMode.NONE);

            belowPart1 = partName == null ? "<none>" : partName;
            belowPart2 = part2Name == null ? "<none>" : part2Name;
        }
    }

    private BuildingPart resolvePart(GenerationContext context, ResourceLocation owner, String rawPart) {
        if (rawPart == null || rawPart.isBlank()) {
            return null;
        }
        return AssetRegistries.PARTS.get(context.getDimensionInfo().getWorld(), resolveLocation(owner, rawPart));
    }

    private void renderPart(GenerationContext context,
                            BuildingPart part,
                            CompiledPalette palette,
                            int baseY,
                            boolean overlay,
                            Transform transform,
                            TerrainFixMode terrainFixMode) {
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

                    String definition = resolveFromPaletteString(context, palette, token);
                    if (definition == null || definition.isBlank()) {
                        continue;
                    }
                    if (definition.contains("structure_void")) {
                        continue;
                    }
                    context.setBlock(worldX, y, worldZ, definition);
                    // 保存基础材料用于地形修复
                    if (dy == 0) {
                        Material material = context.resolveMaterial(definition, null);
                        baseMaterials[worldX][worldZ] = material;
                    }
                }
            }
        }

        if (!overlay) {
            applyTerrainFix(context, baseMaterials, baseY, terrainFixMode);
        }
    }

    private void applyTerrainFix(GenerationContext context, Material[][] baseMaterials, int baseY, TerrainFixMode mode) {
        if (mode == TerrainFixMode.NONE) {
            return;
        }

        ChunkHeightmap heightmap = context.getDimensionInfo().getHeightmap(context.getChunkX(), context.getChunkZ());
        int fallbackMinHeight = calculateHeightStats(context.getDimensionInfo(), context.getChunkX(), context.getChunkZ()).min();

        switch (mode) {
            case REPEATSLICE -> {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material support = baseMaterials[x][z];
                        if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                            continue;
                        }
                        int terrainHeight = terrainHeight(heightmap, x, z, fallbackMinHeight);
                        for (int y = baseY - 1; y >= terrainHeight; y--) {
                            context.setBlock(x, y, z, support);
                        }
                    }
                }
            }
            case SLICEONLY -> {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material support = baseMaterials[x][z];
                        if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                            continue;
                        }
                        int terrainHeight = terrainHeight(heightmap, x, z, fallbackMinHeight);
                        if (terrainHeight < baseY) {
                            context.setBlock(x, terrainHeight, z, support);
                        }
                    }
                }
            }
            case CANOPY -> {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material support = baseMaterials[x][z];
                        if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                            continue;
                        }
                        int terrainHeight = terrainHeight(heightmap, x, z, fallbackMinHeight);
                        for (int y = terrainHeight; y < baseY; y++) {
                            context.setBlock(x, y, z, Material.AIR);
                        }
                        context.setBlock(x, baseY, z, support);
                        if (support.isSolid()) {
                            for (int y = baseY + 1; y <= baseY + 2; y++) {
                                context.setBlock(x, y, z, Material.GLOWSTONE);
                            }
                        }
                    }
                }
            }
            case FLOATING -> {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material support = baseMaterials[x][z];
                        if (support == null || support == Material.AIR || support == Material.STRUCTURE_VOID) {
                            continue;
                        }
                        int terrainHeight = terrainHeight(heightmap, x, z, fallbackMinHeight);
                        int floatHeight = terrainHeight + 3 + context.getRandom().nextInt(5);
                        for (int y = terrainHeight; y < floatHeight; y++) {
                            context.setBlock(x, y, z, Material.AIR);
                        }
                        for (int y = floatHeight; y < floatHeight + 3; y++) {
                            context.setBlock(x, y, z, support);
                        }
                    }
                }
            }
        }
    }

    private CompiledPalette composeBuildingPalette(GenerationContext context, Building building) {
        List<Palette> palettes = new ArrayList<>();
        if (building.getLocalPalette() != null) {
            palettes.add(building.getLocalPalette());
        }
        if (building.getRefPalette() != null) {
            Palette ref = AssetRegistries.PALETTES.get(
                    context.getDimensionInfo().getWorld(),
                    resolveLocation(building.getId(), building.getRefPalette()));
            if (ref != null) {
                palettes.add(ref);
            }
        }
        if (palettes.isEmpty()) {
            return context.palette();
        }
        return new CompiledPalette(context.palette(), palettes.toArray(Palette[]::new));
    }

    private CompiledPalette composePartPalette(GenerationContext context, CompiledPalette base, BuildingPart part) {
        if (part == null) {
            return base;
        }
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
            return base;
        }
        return new CompiledPalette(base, additions.toArray(Palette[]::new));
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

    private boolean matchesBiome(BiomeMatcher matcher, String biome) {
        if (matcher == null) {
            return true;
        }
        String normalizedBiome = biome == null ? "" : biome.toLowerCase(Locale.ROOT);

        if (!matcher.getExcluding().isEmpty()) {
            for (String excluded : matcher.getExcluding()) {
                if (tokenMatches(excluded, normalizedBiome)) {
                    return false;
                }
            }
        }
        if (!matcher.getIfAll().isEmpty()) {
            for (String required : matcher.getIfAll()) {
                if (!tokenMatches(required, normalizedBiome)) {
                    return false;
                }
            }
        }
        if (!matcher.getIfAny().isEmpty()) {
            for (String option : matcher.getIfAny()) {
                if (tokenMatches(option, normalizedBiome)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean tokenMatches(String token, String biome) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String normalized = token.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("#")) {
            String tail = normalized.substring(1);
            int colon = tail.indexOf(':');
            String simple = colon >= 0 ? tail.substring(colon + 1) : tail;
            simple = simple.replace("is_", "").replace('_', ' ');
            return biome.contains(simple.replace(" ", "_")) || biome.contains(simple.replace(" ", ""));
        }
        return biome.equals(normalized) || biome.endsWith(normalized);
    }

    private int resolveTerrainHeight(String mode, HeightStats stats, int waterLevel) {
        if (mode == null) {
            return stats.average();
        }
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "highest", "max" -> stats.max();
            case "lowest", "min" -> stats.min();
            case "ocean", "water" -> waterLevel;
            case "average", "avg", "mean" -> stats.average();
            default -> stats.average();
        };
    }

    private HeightStats calculateHeightStats(IDimensionInfo provider, int chunkX, int chunkZ) {
        ChunkHeightmap heightmap = provider.getHeightmap(chunkX, chunkZ);
        if (heightmap == null) {
            return new HeightStats(64, 64, 64);
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int total = 0;
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = heightmap.getHeight(x, z);
                min = Math.min(min, height);
                max = Math.max(max, height);
                total += height;
                count++;
            }
        }
        if (count == 0) {
            return new HeightStats(64, 64, 64);
        }
        return new HeightStats(min, max, total / count);
    }

    private String getBiomeName(IDimensionInfo provider, int chunkX, int chunkZ, int y) {
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        org.bukkit.block.Biome biome = provider.getBiome(x, y, z);
        if (biome == null) {
            return "minecraft:plains";
        }
        return "minecraft:" + biome.name().toLowerCase(Locale.ROOT);
    }

    private Random gridRandom(long worldSeed, int gridX, int gridZ) {
        return gridRandom(worldSeed, gridX, gridZ, SCATTERED_GRID_SALT);
    }

    private Random gridRandom(long worldSeed, int gridX, int gridZ, long salt) {
        long seed = worldSeed;
        seed ^= (long) gridX * 341873128712L;
        seed ^= (long) gridZ * 132897987541L;
        seed ^= salt;
        return new Random(seed);
    }

    private int boundedRandom(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private int clampFloor(int value, int fallback) {
        return value < 0 ? fallback : Math.max(value, fallback);
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

    private long saltFrom(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        long hash = normalized.hashCode();
        return SCATTERED_GRID_SALT ^ (hash * 0x9E3779B97F4A7C15L);
    }

    private int terrainHeight(ChunkHeightmap heightmap, int x, int z, int fallback) {
        if (heightmap == null) {
            return fallback;
        }
        if (x < 0 || x >= 16 || z < 0 || z >= 16) {
            return fallback;
        }
        return heightmap.getHeight(x, z);
    }

    private record HeightStats(int min, int max, int average) {
        int diff() {
            return max - min;
        }
    }

    private record StructureSize(int dimX, int dimZ) {
    }

    private record ScatteredCandidate(ScatteredSelector selector,
                                      ScatteredBuilding scattered,
                                      int anchorChunkX,
                                      int anchorChunkZ,
                                      HeightStats anchorStats,
                                      String biomeName,
                                      int weight) {
    }

    private enum TerrainFixMode {
        NONE,
        REPEATSLICE,
        SLICEONLY,
        CANOPY,
        FLOATING;

        private static TerrainFixMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return NONE;
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "repeatslice", "repeat" -> REPEATSLICE;
                case "sliceonly", "slice" -> SLICEONLY;
                case "canopy" -> CANOPY;
                case "floating", "float" -> FLOATING;
                default -> NONE;
            };
        }
    }

    private static final class ScatteredConditionContext extends ConditionContext {
        private final String biome;

        private ScatteredConditionContext(int level,
                                          int floor,
                                          int floorsBelowGround,
                                          int floorsAboveGround,
                                          String part,
                                          String belowPart,
                                          String building,
                                          ChunkCoord coord,
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
