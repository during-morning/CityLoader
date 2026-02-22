package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.util.TimedCache;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.Building;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.ConditionContext;
import com.during.cityloader.worldgen.lost.cityassets.MultiBuilding;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.cityassets.Style;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.data.BiomeMatcher;
import com.during.cityloader.worldgen.lost.regassets.data.BuildingSettings;
import com.during.cityloader.worldgen.lost.regassets.data.CityStyleSelector;
import com.during.cityloader.worldgen.lost.regassets.data.SelectorEntry;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 建筑信息类
 * 缓存区块级别的城市生成状态信息
 */
public class BuildingInfo {

    private static final int FLOOR_HEIGHT = 6;

    public final ChunkCoord coord;
    public final IDimensionInfo provider;
    public final LostCityProfile profile;

    public boolean isCity;
    public boolean hasBuilding;
    public int groundLevel;
    public final int waterLevel;

    public ILostCityBuilding buildingType;
    public BuildingPart[] floorTypes;
    public BuildingPart[] floorTypes2;
    public Transform[] floorTransforms;
    public Transform[] floorTransforms2;
    public int[] floorLevels;
    public int[] floorLevels2;

    public int floors;
    public int cellars;

    public int highwayXLevel; // 0=none, 1=at city level, 2=at city level+6
    public int highwayZLevel;
    public boolean xRailCorridor;
    public boolean zRailCorridor;

    public int cityLevel;
    public ILostCityMultiBuilding multiBuilding;
    public MultiPos multiBuildingPos;
    public boolean xBridge;
    public boolean zBridge;

    private static final Supplier<Integer> CACHE_TIMEOUT = () -> 300;
    private static final TimedCache<ChunkCoord, BuildingInfo> BUILDING_INFO_MAP = new TimedCache<>(CACHE_TIMEOUT);
    private static final TimedCache<ChunkCoord, LostChunkCharacteristics> CITY_INFO_MAP = new TimedCache<>(CACHE_TIMEOUT);
    private static final TimedCache<ChunkCoord, Integer> CITY_LEVEL_CACHE = new TimedCache<>(CACHE_TIMEOUT);

    private BuildingInfo xmin = null;
    private BuildingInfo xmax = null;
    private BuildingInfo zmin = null;
    private BuildingInfo zmax = null;
    private CompiledPalette compiledPalette = null;
    private final List<Runnable> postTodo = new ArrayList<>();
    private final List<PalettePostTodo> palettePostTodo = new ArrayList<>();

    private final CityStyle cityStyle;

    private BuildingInfo(ChunkCoord coord, IDimensionInfo provider) {
        this.coord = coord;
        this.provider = provider;
        this.profile = provider.getProfile();

        this.groundLevel = calculateGroundLevel(coord, provider);
        World world = provider.getWorld();
        this.waterLevel = world == null ? 63 : world.getSeaLevel();

        Random random = chunkRandom(provider.getSeed(), coord.chunkX(), coord.chunkZ(), 0x9E3779B97F4A7C15L);
        String biomeName = getBiomeName(provider, coord, groundLevel);

        this.cityStyle = resolveCityStyle(random, biomeName);

        // LostCities 语义：cityFactor 需要超过 cityThreshold 才算城市区块
        float cityFactor = City.getCityFactor(coord, provider, profile);
        this.isCity = cityFactor > profile.getCityThreshold();

        this.cityLevel = isCity ? getCityLevel(coord, provider) : 0;
        CITY_LEVEL_CACHE.put(coord, this.cityLevel);

        this.highwayXLevel = isCity && Math.floorMod(coord.chunkZ(), 32) == 0 ? getCityGroundLevel() + FLOOR_HEIGHT : 0;
        this.highwayZLevel = isCity && Math.floorMod(coord.chunkX(), 32) == 0 ? getCityGroundLevel() + FLOOR_HEIGHT : 0;
        this.xRailCorridor = false;
        this.zRailCorridor = false;

        boolean streetChunk = isStreetChunk(coord.chunkX(), coord.chunkZ());
        boolean infrastructureChunk = highwayXLevel > 0 || highwayZLevel > 0;
        this.hasBuilding = isCity && !streetChunk && !infrastructureChunk;

        MultiPlacement placement = resolveMultiBuilding(cityStyle, provider, coord);
        this.multiBuilding = placement.multiBuilding;
        this.multiBuildingPos = placement.multiPos;

        Building resolvedBuilding = hasBuilding ? resolveBuilding(random, cityStyle, placement, provider, coord) : null;
        this.buildingType = resolvedBuilding;

        if (resolvedBuilding != null) {
            configureFloors(random, resolvedBuilding, cityStyle, biomeName);
        } else {
            this.floors = 0;
            this.cellars = 0;
            this.floorTypes = new BuildingPart[0];
            this.floorTypes2 = new BuildingPart[0];
            this.floorTransforms = new Transform[0];
            this.floorTransforms2 = new Transform[0];
            this.hasBuilding = false;
        }
        initializeCorridorCandidates();

        boolean waterBiome = biomeName.contains("ocean") || biomeName.contains("river") || biomeName.contains("beach");
        this.xBridge = highwayXLevel > 0 && shouldUseBridgeStyle(waterBiome, true);
        this.zBridge = highwayZLevel > 0 && shouldUseBridgeStyle(waterBiome, false);
    }

    public static BuildingInfo getBuildingInfo(ChunkCoord coord, IDimensionInfo provider) {
        return BUILDING_INFO_MAP.computeIfAbsent(coord, k -> new BuildingInfo(k, provider));
    }

    public static LostChunkCharacteristics getChunkCharacteristics(ChunkCoord coord, IDimensionInfo provider) {
        return CITY_INFO_MAP.computeIfAbsent(coord, k -> {
            LostChunkCharacteristics characteristics = new LostChunkCharacteristics();
            BuildingInfo info = getBuildingInfo(k, provider);
            characteristics.isCity = info.isCity;
            characteristics.couldHaveBuilding = info.hasBuilding;
            characteristics.cityLevel = info.cityLevel;
            characteristics.multiPos = info.multiBuildingPos;
            characteristics.multiBuilding = info.multiBuilding;
            characteristics.buildingType = info.buildingType;
            characteristics.cityStyle = info.cityStyle;
            return characteristics;
        });
    }

    public static boolean isCityRaw(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        if (coord == null || provider == null || profile == null) {
            return false;
        }
        float cityFactor = City.getCityFactor(coord, provider, profile);
        return cityFactor > profile.getCityThreshold();
    }

    public static int getCityLevel(ChunkCoord coord, IDimensionInfo provider) {
        if (coord == null || provider == null || provider.getProfile() == null) {
            return 0;
        }
        Integer cached = CITY_LEVEL_CACHE.get(coord);
        if (cached != null) {
            return cached;
        }
        int level = calculateCityLevel(coord, provider, provider.getProfile());
        CITY_LEVEL_CACHE.put(coord, level);
        return level;
    }

    public BuildingInfo getXmin() {
        if (xmin == null) {
            xmin = getBuildingInfo(coord.west(), provider);
        }
        return xmin;
    }

    public BuildingInfo getXmax() {
        if (xmax == null) {
            xmax = getBuildingInfo(coord.east(), provider);
        }
        return xmax;
    }

    public BuildingInfo getZmin() {
        if (zmin == null) {
            zmin = getBuildingInfo(coord.north(), provider);
        }
        return zmin;
    }

    public BuildingInfo getZmax() {
        if (zmax == null) {
            zmax = getBuildingInfo(coord.south(), provider);
        }
        return zmax;
    }

    public int getCityGroundLevel() {
        return groundLevel + cityLevel * FLOOR_HEIGHT;
    }

    public int getMaxHeight() {
        if (hasBuilding && floors > 0) {
            return getCityGroundLevel() + floors * FLOOR_HEIGHT;
        }
        return getCityGroundLevel();
    }

    public boolean isCityRaw() {
        return isCity;
    }

    public int getCityLevel() {
        Integer cached = CITY_LEVEL_CACHE.get(coord);
        return cached == null ? cityLevel : cached;
    }

    public CityStyle getCityStyle() {
        return cityStyle;
    }

    public CompiledPalette getCompiledPalette() {
        if (compiledPalette == null) {
            compiledPalette = createPalette();
        }
        return compiledPalette;
    }

    public boolean isValidFloor(int floor) {
        if (floor < -cellars) {
            return false;
        }
        return floor < floors;
    }

    public BuildingPart getFloor(int floor) {
        if (!isValidFloor(floor)) {
            return null;
        }
        int index = floor + cellars;
        if (index >= 0 && index < floorTypes.length) {
            return floorTypes[index];
        }
        return null;
    }

    public BuildingPart getFloorPart2(int floor) {
        if (!isValidFloor(floor)) {
            return null;
        }
        int index = floor + cellars;
        if (index >= 0 && index < floorTypes2.length) {
            return floorTypes2[index];
        }
        return null;
    }

    public Transform getFloorTransform(int floor) {
        if (!isValidFloor(floor)) {
            return Transform.ROTATE_NONE;
        }
        int index = floor + cellars;
        if (index >= 0 && index < floorTransforms.length && floorTransforms[index] != null) {
            return floorTransforms[index];
        }
        return Transform.ROTATE_NONE;
    }

    public Transform getFloorPart2Transform(int floor) {
        if (!isValidFloor(floor)) {
            return Transform.ROTATE_NONE;
        }
        int index = floor + cellars;
        if (index >= 0 && index < floorTransforms2.length && floorTransforms2[index] != null) {
            return floorTransforms2[index];
        }
        return Transform.ROTATE_NONE;
    }

    public void addPostTodo(Runnable task) {
        if (task == null) {
            return;
        }
        synchronized (postTodo) {
            postTodo.add(task);
        }
    }

    public List<Runnable> drainPostTodo() {
        synchronized (postTodo) {
            if (postTodo.isEmpty()) {
                return List.of();
            }
            List<Runnable> drained = List.copyOf(postTodo);
            postTodo.clear();
            return drained;
        }
    }

    public void clearPostTodo() {
        synchronized (postTodo) {
            postTodo.clear();
        }
    }

    public int getPostTodoCount() {
        synchronized (postTodo) {
            return postTodo.size();
        }
    }

    public void addPalettePostTodo(int x, int y, int z, String partName, CompiledPalette.Information information) {
        if (information == null) {
            return;
        }

        String resolvedPart = (partName == null || partName.isBlank()) ? "<none>" : partName;
        String resolvedBuilding = buildingType == null ? "<none>" : buildingType.getName();
        ConditionTodo loot = information.loot() == null || information.loot().isBlank()
                ? null
                : new ConditionTodo(information.loot(), resolvedPart, resolvedBuilding);
        ConditionTodo mob = information.mob() == null || information.mob().isBlank()
                ? null
                : new ConditionTodo(information.mob(), resolvedPart, resolvedBuilding);
        Map<String, Object> tag = information.tag() == null || information.tag().isEmpty()
                ? Map.of()
                : Map.copyOf(information.tag());
        boolean torch = information.torch();

        if (!torch && loot == null && mob == null && tag.isEmpty()) {
            return;
        }

        PalettePostTodo todo = new PalettePostTodo(x, y, z, loot, mob, tag, torch);
        synchronized (palettePostTodo) {
            palettePostTodo.add(todo);
        }
    }

    public List<PalettePostTodo> drainPalettePostTodo() {
        synchronized (palettePostTodo) {
            if (palettePostTodo.isEmpty()) {
                return List.of();
            }
            List<PalettePostTodo> drained = List.copyOf(palettePostTodo);
            palettePostTodo.clear();
            return drained;
        }
    }

    public void clearPalettePostTodo() {
        synchronized (palettePostTodo) {
            palettePostTodo.clear();
        }
    }

    public int getPalettePostTodoCount() {
        synchronized (palettePostTodo) {
            return palettePostTodo.size();
        }
    }

    public static void cleanupCache() {
        BUILDING_INFO_MAP.cleanup();
        CITY_INFO_MAP.cleanup();
        CITY_LEVEL_CACHE.cleanup();
        DamageArea.resetCache();
        Railway.cleanCache();
        City.cleanCache();
    }

    public static void resetCache() {
        BUILDING_INFO_MAP.clear();
        CITY_INFO_MAP.clear();
        CITY_LEVEL_CACHE.clear();
        DamageArea.resetCache();
        Railway.cleanCache();
        City.cleanCache();
    }

    private int calculateGroundLevel(ChunkCoord coord, IDimensionInfo provider) {
        if (provider == null || provider.getProfile() == null) {
            return 64;
        }
        return Math.max(1, provider.getProfile().getGroundLevel());
    }

    private boolean shouldUseBridgeStyle(boolean waterBiome, boolean xAxis) {
        if (waterBiome) {
            return true;
        }
        ChunkHeightmap heightmap = provider.getHeightmap(coord);
        if (heightmap == null) {
            return false;
        }

        int laneY = xAxis ? highwayXLevel : highwayZLevel;
        int waterishColumns = 0;
        int deepDropColumns = 0;

        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                int h1 = clampHeight(heightmap.getHeight(x, 7));
                int h2 = clampHeight(heightmap.getHeight(x, 8));
                int min = Math.min(h1, h2);
                if (min <= waterLevel + 1) {
                    waterishColumns++;
                }
                if (laneY - min >= 12) {
                    deepDropColumns++;
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                int h1 = clampHeight(heightmap.getHeight(7, z));
                int h2 = clampHeight(heightmap.getHeight(8, z));
                int min = Math.min(h1, h2);
                if (min <= waterLevel + 1) {
                    waterishColumns++;
                }
                if (laneY - min >= 12) {
                    deepDropColumns++;
                }
            }
        }

        return waterishColumns >= 5 || deepDropColumns >= 8;
    }

    private int clampHeight(int height) {
        if (height <= 0) {
            return groundLevel;
        }
        return height;
    }

    private CityStyle resolveCityStyle(Random random, String biomeName) {
        WorldStyle worldStyle = provider.getWorldStyle();
        if (worldStyle != null) {
            String selected = worldStyle.pickCityStyle(random, biomeName);
            CityStyle cityStyle = lookupCityStyle(worldStyle.getId(), selected);
            if (cityStyle != null) {
                return cityStyle;
            }
        }

        CityStyle standard = AssetRegistries.CITYSTYLES.get(provider.getWorld(), "lostcities:citystyle_standard");
        if (standard != null) {
            return standard;
        }

        for (CityStyle fallback : AssetRegistries.CITYSTYLES.getIterable()) {
            return fallback;
        }
        return null;
    }

    private static int calculateCityLevel(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        int height = sampleHeight(provider, coord, profile.getGroundLevel());
        if (profile.isUseAvgHeightmap()) {
            int total = height;
            int count = 1;

            ChunkCoord west = coord.west();
            if (isCityRaw(west, provider, profile)) {
                total += sampleHeight(provider, west, height);
                count++;
            }
            ChunkCoord east = coord.east();
            if (isCityRaw(east, provider, profile)) {
                total += sampleHeight(provider, east, height);
                count++;
            }
            ChunkCoord north = coord.north();
            if (isCityRaw(north, provider, profile)) {
                total += sampleHeight(provider, north, height);
                count++;
            }
            ChunkCoord south = coord.south();
            if (isCityRaw(south, provider, profile)) {
                total += sampleHeight(provider, south, height);
                count++;
            }
            height = total / count;
        }
        return getLevelBasedOnHeight(height, profile);
    }

    private static int sampleHeight(IDimensionInfo provider, ChunkCoord coord, int fallback) {
        if (provider == null || coord == null) {
            return fallback;
        }
        com.during.cityloader.worldgen.ChunkHeightmap heightmap = provider.getHeightmap(coord);
        if (heightmap == null) {
            return fallback;
        }
        return heightmap.getHeight();
    }

    private static int getLevelBasedOnHeight(int height, LostCityProfile profile) {
        if (height < profile.getCityLevel0Height()) {
            return 0;
        } else if (height < profile.getCityLevel1Height()) {
            return 1;
        } else if (height < profile.getCityLevel2Height()) {
            return 2;
        } else if (height < profile.getCityLevel3Height()) {
            return 3;
        } else if (height < profile.getCityLevel4Height()) {
            return 4;
        } else if (height < profile.getCityLevel5Height()) {
            return 5;
        } else if (height < profile.getCityLevel6Height()) {
            return 6;
        } else if (height < profile.getCityLevel7Height()) {
            return 7;
        } else {
            return 8;
        }
    }

    private void initializeCorridorCandidates() {
        if (!isCity) {
            xRailCorridor = false;
            zRailCorridor = false;
            return;
        }
        if (hasBuilding && cellars > 0) {
            xRailCorridor = false;
            zRailCorridor = false;
            return;
        }

        float chance = Math.max(0.0f, Math.min(1.0f, profile.getCorridorChance()));
        Random corridorRandom = chunkRandom(provider.getSeed(), coord.chunkX(), coord.chunkZ(), 0x2545F4914F6CDD1DL);
        xRailCorridor = corridorRandom.nextFloat() < chance;
        zRailCorridor = corridorRandom.nextFloat() < chance;
    }

    private boolean isStreetChunk(int chunkX, int chunkZ) {
        int gridSize = 4;
        int streetWidth = 1;
        int gx = Math.floorMod(chunkX, gridSize);
        int gz = Math.floorMod(chunkZ, gridSize);
        return gx < streetWidth || gz < streetWidth;
    }

    private MultiPlacement resolveMultiBuilding(CityStyle cityStyle, IDimensionInfo provider, ChunkCoord coord) {
        if (cityStyle == null) {
            return MultiPlacement.none();
        }

        String multiId = pickFromCityStyleChain(cityStyle, "multibuildings", chunkRandom(provider.getSeed(), coord.chunkX(), coord.chunkZ(), 0x1234ABCDL));
        if (multiId == null) {
            return MultiPlacement.none();
        }

        MultiBuilding candidate = findMultiBuilding(cityStyle.getId(), multiId);
        if (candidate == null || candidate.getDimX() <= 1 && candidate.getDimZ() <= 1) {
            return MultiPlacement.none();
        }

        int posX = Math.floorMod(coord.chunkX(), Math.max(1, candidate.getDimX()));
        int posZ = Math.floorMod(coord.chunkZ(), Math.max(1, candidate.getDimZ()));
        MultiPos pos = new MultiPos(posX, posZ, Math.max(1, candidate.getDimX()), Math.max(1, candidate.getDimZ()));

        return new MultiPlacement(candidate, pos);
    }

    private Building resolveBuilding(Random random, CityStyle cityStyle, MultiPlacement placement,
                                    IDimensionInfo provider, ChunkCoord coord) {
        if (placement.multiBuilding instanceof MultiBuilding mb) {
            String multiBuildingName = mb.getBuildingAt(placement.multiPos.getX(), placement.multiPos.getZ());
            if (multiBuildingName != null && !multiBuildingName.isBlank()) {
                Building fromMulti = findBuilding(mb.getId(), multiBuildingName);
                if (fromMulti != null) {
                    return fromMulti;
                }
                Building emptyFallback = findFallbackEmptyBuilding(mb.getId());
                if (emptyFallback != null) {
                    return emptyFallback;
                }
            }
        }

        String selected = pickFromCityStyleChain(cityStyle, "buildings", random);
        if (selected != null) {
            Building building = findBuilding(cityStyle.getId(), selected);
            if (building != null) {
                return building;
            }
        }

        List<Building> styledBuildings = collectExistingCityStyleBuildings(cityStyle);
        if (!styledBuildings.isEmpty()) {
            return styledBuildings.get(random.nextInt(styledBuildings.size()));
        }

        Building emptyFallback = findFallbackEmptyBuilding(cityStyle == null ? null : cityStyle.getId());
        if (emptyFallback != null) {
            return emptyFallback;
        }

        List<Building> all = new ArrayList<>();
        for (Building fallback : AssetRegistries.BUILDINGS.getIterable()) {
            if (fallback != null) {
                all.add(fallback);
            }
        }
        if (!all.isEmpty()) {
            return all.get(random.nextInt(all.size()));
        }
        return null;
    }

    private List<Building> collectExistingCityStyleBuildings(CityStyle cityStyle) {
        if (cityStyle == null) {
            return List.of();
        }

        List<Building> resolved = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (CityStyle style : resolveCityStyleChain(cityStyle)) {
            List<SelectorEntry> entries = new ArrayList<>();
            entries.addAll(style.getSelector("buildings"));
            appendLegacySelectorEntries(style, "buildings", entries);

            for (SelectorEntry entry : entries) {
                if (entry == null || entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                Building building = findBuilding(style.getId(), entry.getValue());
                if (building == null) {
                    continue;
                }
                String key = building.getId().toString();
                if (seen.add(key)) {
                    resolved.add(building);
                }
            }
        }
        return resolved;
    }

    private Building findFallbackEmptyBuilding(ResourceLocation owner) {
        for (String alias : List.of("common_empty", "common_void")) {
            if (owner != null) {
                Building inNamespace = findBuilding(owner, alias);
                if (inNamespace != null) {
                    return inNamespace;
                }
            }

            for (Building candidate : AssetRegistries.BUILDINGS.getIterable()) {
                if (candidate == null || candidate.getId() == null) {
                    continue;
                }
                if (alias.equals(candidate.getId().getPath())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void configureFloors(Random random, Building building, CityStyle cityStyle, String biomeName) {
        BuildingSettings settings = firstBuildingSettings(cityStyle);

        int minFloors = sanitizeRangeStart(building.getMinFloors(), settings == null ? null : settings.getMinFloors(), 1);
        int maxFloors = sanitizeRangeEnd(building.getMaxFloors(), settings == null ? null : settings.getMaxFloors(), minFloors);
        int minCellars = sanitizeRangeStart(building.getMinCellars(), settings == null ? null : settings.getMinCellars(), 0);
        int maxCellars = sanitizeRangeEnd(building.getMaxCellars(), settings == null ? null : settings.getMaxCellars(), minCellars);

        this.floors = boundedRandom(random, minFloors, maxFloors);
        this.cellars = boundedRandom(random, minCellars, maxCellars);

        int total = Math.max(0, floors + cellars);
        this.floorTypes = new BuildingPart[total];
        this.floorTypes2 = new BuildingPart[total];
        this.floorTransforms = new Transform[total];
        this.floorTransforms2 = new Transform[total];

        String below1 = "<none>";
        String below2 = "<none>";

        for (int floor = -cellars; floor < floors; floor++) {
            int index = floor + cellars;
            ConditionContext context = new FloorConditionContext(
                    cityLevel + floor,
                    floor,
                    cellars,
                    floors,
                    "<none>",
                    below1,
                    building.getName(),
                    coord,
                    biomeName);

            Building.PartSelection part1Selection = building.getRandomPartRef(random, context);
            Building.PartSelection part2Selection = building.getRandomPart2Ref(random, context);
            String part1 = part1Selection == null ? null : part1Selection.partName();
            String part2 = part2Selection == null ? null : part2Selection.partName();

            floorTypes[index] = resolvePart(building.getId(), part1);
            floorTypes2[index] = resolvePart(building.getId(), part2);
            floorTransforms[index] = part1Selection == null ? Transform.ROTATE_NONE : part1Selection.transform();
            floorTransforms2[index] = part2Selection == null ? Transform.ROTATE_NONE : part2Selection.transform();

            below1 = part1 == null ? "<none>" : part1;
            below2 = part2 == null ? "<none>" : part2;
        }
    }

    private BuildingPart resolvePart(ResourceLocation owner, String partName) {
        if (partName == null || partName.isBlank()) {
            return null;
        }
        return findPart(owner, partName);
    }

    private BuildingSettings firstBuildingSettings(CityStyle cityStyle) {
        for (CityStyle style : resolveCityStyleChain(cityStyle)) {
            if (style.getBuildingSettings() != null) {
                return style.getBuildingSettings();
            }
        }
        return null;
    }

    private String pickFromCityStyleChain(CityStyle cityStyle, String selector, Random random) {
        if (cityStyle == null) {
            return null;
        }

        List<SelectorEntry> entries = new ArrayList<>();
        for (CityStyle style : resolveCityStyleChain(cityStyle)) {
            entries.addAll(style.getSelector(selector));
            appendLegacySelectorEntries(style, selector, entries);
        }

        if (entries.isEmpty()) {
            return null;
        }

        int total = 0;
        List<SelectorEntry> normalized = new ArrayList<>();
        for (SelectorEntry entry : entries) {
            if (entry == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!matchesBiome(entry.getBiomes(), getBiomeName(provider, coord, groundLevel))) {
                continue;
            }
            total += Math.max(1, Math.round(entry.getFactor() * 100));
            normalized.add(entry);
        }

        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.size() == 1 || total <= 0) {
            return normalized.get(0).getValue();
        }

        int roll = random.nextInt(total);
        int current = 0;
        for (SelectorEntry entry : normalized) {
            current += Math.max(1, Math.round(entry.getFactor() * 100));
            if (roll < current) {
                return entry.getValue();
            }
        }
        return normalized.get(normalized.size() - 1).getValue();
    }

    private List<CityStyle> resolveCityStyleChain(CityStyle style) {
        if (style == null) {
            return List.of();
        }

        List<CityStyle> chain = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        CityStyle current = style;

        while (current != null) {
            String key = current.getId().toString();
            if (!seen.add(key)) {
                break;
            }
            chain.add(current);

            String inherit = current.getInherit();
            if (inherit == null || inherit.isBlank()) {
                break;
            }
            current = lookupCityStyle(current.getId(), inherit);
        }

        return Collections.unmodifiableList(chain);
    }

    private CityStyle lookupCityStyle(ResourceLocation owner, String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return null;
        }
        for (ResourceLocation candidate : resolveLocationCandidates(owner, styleId)) {
            CityStyle cityStyle = AssetRegistries.CITYSTYLES.get(provider.getWorld(), candidate);
            if (cityStyle != null) {
                return cityStyle;
            }
        }
        return null;
    }

    private CompiledPalette createPalette() {
        List<Palette> palettes = new ArrayList<>();

        // LostCities 顺序：先基础 style palette，再 building/part 局部覆盖
        String baseStyleId = firstCityStyleString(cityStyle, CityStyle::getStyle);
        if (cityStyle != null && baseStyleId != null) {
            Style style = findStyle(cityStyle.getId(), baseStyleId);
            if (style != null) {
                Random styleRandom = chunkRandom(provider.getSeed(), coord.chunkX(), coord.chunkZ(), 0xDEADBEEFL);
                for (String paletteId : style.pickRandomPalettes(styleRandom)) {
                    Palette palette = findPalette(style.getId(), paletteId);
                    if (palette != null) {
                        palettes.add(palette);
                    }
                }
            }
        }

        if (cityStyle != null) {
            Random paletteRandom = chunkRandom(provider.getSeed(), coord.chunkX(), coord.chunkZ(), 0x6C8E9CF570932BD5L);
            String selected = pickFromCityStyleChain(cityStyle, "palettes", paletteRandom);
            if (selected != null) {
                Palette palette = findPalette(cityStyle.getId(), selected);
                if (palette != null) {
                    palettes.add(palette);
                }
            }
        }

        if (buildingType instanceof Building building) {
            if (building.getLocalPalette() != null) {
                palettes.add(building.getLocalPalette());
            }
            if (building.getRefPalette() != null) {
                Palette refPalette = findPalette(building.getId(), building.getRefPalette());
                if (refPalette != null) {
                    palettes.add(refPalette);
                }
            }
        }

        for (BuildingPart part : floorTypes) {
            addPartPalette(part, palettes);
        }
        for (BuildingPart part : floorTypes2) {
            addPartPalette(part, palettes);
        }

        if (palettes.isEmpty()) {
            return new CompiledPalette();
        }

        return new CompiledPalette(palettes.toArray(Palette[]::new));
    }

    private void addPartPalette(BuildingPart part, List<Palette> palettes) {
        if (part == null) {
            return;
        }
        if (part.getLocalPalette() != null) {
            palettes.add(part.getLocalPalette());
        }
        if (part.getPalette() != null) {
            Palette ref = findPalette(part.getId(), part.getPalette());
            if (ref != null) {
                palettes.add(ref);
            }
        }
    }

    private int sanitizeRangeStart(int buildingValue, Integer styleValue, int fallback) {
        int value = buildingValue;
        if (value < 0 && styleValue != null) {
            value = styleValue;
        }
        if (value < 0) {
            value = fallback;
        }
        return value;
    }

    private int sanitizeRangeEnd(int buildingValue, Integer styleValue, int min) {
        int value = buildingValue;
        if (value < 0 && styleValue != null) {
            value = styleValue;
        }
        if (value < min) {
            value = min;
        }
        return value;
    }

    private int boundedRandom(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private Random chunkRandom(long worldSeed, int chunkX, int chunkZ, long salt) {
        long seed = worldSeed;
        seed ^= (long) chunkX * 341873128712L;
        seed ^= (long) chunkZ * 132897987541L;
        seed ^= salt;
        return new Random(seed);
    }

    private void appendLegacySelectorEntries(CityStyle style, String selector, List<SelectorEntry> entries) {
        if (style == null || selector == null) {
            return;
        }

        if ("buildings".equals(selector) || "building".equals(selector)) {
            appendLegacyEntries(style.getBuildings(), style.getBuildingWeights(), entries);
        } else if ("multibuildings".equals(selector) || "multibuilding".equals(selector)) {
            appendLegacyEntries(style.getMultiBuildings(), style.getMultiBuildingWeights(), entries);
        }
    }

    private void appendLegacyEntries(List<String> values, List<Float> weights, List<SelectorEntry> entries) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value == null || value.isBlank()) {
                continue;
            }
            float factor = 1.0f;
            if (weights != null && i < weights.size() && weights.get(i) != null) {
                factor = Math.max(0.0001f, weights.get(i));
            }
            SelectorEntry entry = new SelectorEntry();
            entry.setFactor(factor);
            entry.setValue(value);
            entries.add(entry);
        }
    }

    private String firstCityStyleString(CityStyle base, java.util.function.Function<CityStyle, String> extractor) {
        if (base == null || extractor == null) {
            return null;
        }
        for (CityStyle style : resolveCityStyleChain(base)) {
            String value = extractor.apply(style);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Building findBuilding(ResourceLocation owner, String raw) {
        for (ResourceLocation candidate : resolveLocationCandidates(owner, raw)) {
            Building building = AssetRegistries.BUILDINGS.get(provider.getWorld(), candidate);
            if (building != null) {
                return building;
            }
        }
        return null;
    }

    private MultiBuilding findMultiBuilding(ResourceLocation owner, String raw) {
        for (ResourceLocation candidate : resolveLocationCandidates(owner, raw)) {
            MultiBuilding multiBuilding = (MultiBuilding) AssetRegistries.MULTI_BUILDINGS.get(provider.getWorld(), candidate);
            if (multiBuilding != null) {
                return multiBuilding;
            }
        }
        return null;
    }

    private BuildingPart findPart(ResourceLocation owner, String raw) {
        for (ResourceLocation candidate : resolveLocationCandidates(owner, raw)) {
            BuildingPart part = AssetRegistries.PARTS.get(provider.getWorld(), candidate);
            if (part != null) {
                return part;
            }
        }
        return null;
    }

    private Palette findPalette(ResourceLocation owner, String raw) {
        for (ResourceLocation candidate : resolveLocationCandidates(owner, raw)) {
            Palette palette = AssetRegistries.PALETTES.get(provider.getWorld(), candidate);
            if (palette != null) {
                return palette;
            }
        }
        if (raw != null && !raw.isBlank() && !raw.contains(":")) {
            String prefixed = raw.toLowerCase(Locale.ROOT);
            if (!prefixed.startsWith("palette_")) {
                prefixed = "palette_" + prefixed;
                for (ResourceLocation candidate : resolveLocationCandidates(owner, prefixed)) {
                    Palette palette = AssetRegistries.PALETTES.get(provider.getWorld(), candidate);
                    if (palette != null) {
                        return palette;
                    }
                }
            }
        }
        return null;
    }

    private Style findStyle(ResourceLocation owner, String raw) {
        for (ResourceLocation candidate : resolveLocationCandidates(owner, raw)) {
            Style style = AssetRegistries.STYLES.get(provider.getWorld(), candidate);
            if (style != null) {
                return style;
            }
        }
        return null;
    }

    private List<ResourceLocation> resolveLocationCandidates(ResourceLocation owner, String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.contains(":")) {
            return List.of(new ResourceLocation(value));
        }

        List<ResourceLocation> candidates = new ArrayList<>(2);
        candidates.add(new ResourceLocation(owner.getNamespace(), value));
        if (!"lostcities".equals(owner.getNamespace())) {
            candidates.add(new ResourceLocation("lostcities", value));
        }
        return candidates;
    }

    private String getBiomeName(IDimensionInfo provider, ChunkCoord coord, int y) {
        int x = (coord.chunkX() << 4) + 8;
        int z = (coord.chunkZ() << 4) + 8;
        Biome biome = provider.getBiome(x, y, z);
        if (biome == null) {
            return "minecraft:plains";
        }
        return "minecraft:" + biome.name().toLowerCase(Locale.ROOT);
    }

    private boolean matchesBiome(BiomeMatcher matcher, String biome) {
        if (matcher == null) {
            return true;
        }

        if (!matcher.getExcluding().isEmpty()) {
            for (String excluded : matcher.getExcluding()) {
                if (tokenMatches(excluded, biome)) {
                    return false;
                }
            }
        }

        if (!matcher.getIfAll().isEmpty()) {
            for (String required : matcher.getIfAll()) {
                if (!tokenMatches(required, biome)) {
                    return false;
                }
            }
        }

        if (!matcher.getIfAny().isEmpty()) {
            for (String option : matcher.getIfAny()) {
                if (tokenMatches(option, biome)) {
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
        String normalized = token.toLowerCase(Locale.ROOT);
        String normalizedBiome = biome == null ? "" : biome.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            String tag = normalized.substring(1);
            int colon = tag.indexOf(':');
            String simple = colon >= 0 ? tag.substring(colon + 1) : tag;
            simple = simple.replace("is_", "").replace('_', ' ');
            return normalizedBiome.contains(simple.replace(" ", "_"))
                    || normalizedBiome.contains(simple.replace(" ", ""));
        }
        return normalizedBiome.equals(normalized) || normalizedBiome.endsWith(normalized);
    }

    private record MultiPlacement(ILostCityMultiBuilding multiBuilding, MultiPos multiPos) {
        private static MultiPlacement none() {
            return new MultiPlacement(null, null);
        }
    }

    public record ConditionTodo(String condition, String part, String building) {
    }

    public record PalettePostTodo(int x, int y, int z,
                                  ConditionTodo loot,
                                  ConditionTodo mob,
                                  Map<String, Object> tag,
                                  boolean torch) {
    }

    private static final class FloorConditionContext extends ConditionContext {

        private final String biome;

        private FloorConditionContext(int level, int floor, int floorsBelowGround, int floorsAboveGround,
                                      String part, String belowPart, String building, ChunkCoord coord,
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

    /**
     * 检测是否有水平单轨
     * 当此chunk在网格中心且左右城市都需要单轨时返回true
     */
    public boolean hasHorizontalMonorail() {
        if (!profile.getLandscapeType().equals("space") && 
            !profile.getLandscapeType().equals("spheres") &&
            !profile.getLandscapeType().equals("cavernspheres")) {
            return false;
        }
        
        if (!isCity) {
            return false;
        }
        
        // 检查是否是网格中心
        if (coord.chunkX() % 16 != 0 || coord.chunkZ() % 16 != 0) {
            return false;
        }
        
        // 检查左右是否有城市需要单轨
        BuildingInfo left = getXmin();
        BuildingInfo right = getXmax();
        
        boolean leftWants = left != null && left.isCity;
        boolean rightWants = right != null && right.isCity;
        
        return leftWants && rightWants;
    }

    /**
     * 检测是否有垂直单轨
     */
    public boolean hasVerticalMonorail() {
        if (!profile.getLandscapeType().equals("space") && 
            !profile.getLandscapeType().equals("spheres") &&
            !profile.getLandscapeType().equals("cavernspheres")) {
            return false;
        }
        
        if (!isCity) {
            return false;
        }
        
        // 检查是否是网格中心
        if (coord.chunkX() % 16 != 0 || coord.chunkZ() % 16 != 0) {
            return false;
        }
        
        // 检查前后是否有城市需要单轨
        BuildingInfo north = getZmin();
        BuildingInfo south = getZmax();
        
        boolean northWants = north != null && north.isCity;
        boolean southWants = south != null && south.isCity;
        
        return northWants && southWants;
    }

    /**
     * 是否有任何单轨连接
     */
    public boolean hasMonorail() {
        return hasHorizontalMonorail() || hasVerticalMonorail();
    }

    /**
     * 是否有X方向桥梁
     */
    public boolean hasXBridge() {
        return xBridge;
    }

    /**
     * 是否有Z方向桥梁
     */
    public boolean hasZBridge() {
        return zBridge;
    }

    /**
     * 是否有X方向走廊（铁路）
     */
    public boolean hasXCorridor() {
        if (!xRailCorridor) {
            return false;
        }

        BuildingInfo cursor = getXmin();
        int safety = 0;
        while (cursor.canRailGoThrough() && cursor.xRailCorridor && safety++ < 256) {
            cursor = cursor.getXmin();
        }
        if (!cursor.hasBuilding || cursor.cellars == 0) {
            return false;
        }

        cursor = getXmax();
        safety = 0;
        while (cursor.canRailGoThrough() && cursor.xRailCorridor && safety++ < 256) {
            cursor = cursor.getXmax();
        }
        return cursor.hasBuilding && cursor.cellars > 0;
    }

    /**
     * 是否有Z方向走廊（铁路）
     */
    public boolean hasZCorridor() {
        if (!zRailCorridor) {
            return false;
        }

        BuildingInfo cursor = getZmin();
        int safety = 0;
        while (cursor.canRailGoThrough() && cursor.zRailCorridor && safety++ < 256) {
            cursor = cursor.getZmin();
        }
        if (!cursor.hasBuilding || cursor.cellars == 0) {
            return false;
        }

        cursor = getZmax();
        safety = 0;
        while (cursor.canRailGoThrough() && cursor.zRailCorridor && safety++ < 256) {
            cursor = cursor.getZmax();
        }
        return cursor.hasBuilding && cursor.cellars > 0;
    }

    private boolean canRailGoThrough() {
        if (!isCity) {
            return false;
        }
        if (!hasBuilding) {
            return true;
        }
        return cellars == 0;
    }
}
