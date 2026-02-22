package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.during.cityloader.worldgen.lost.regassets.BuildingRE;
import com.during.cityloader.worldgen.lost.regassets.CityStyleRE;
import com.during.cityloader.worldgen.lost.regassets.MultiBuildingRE;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.ScatteredRE;
import com.during.cityloader.worldgen.lost.regassets.WorldStyleRE;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ScatteredStage 资产驱动测试")
class ScatteredStageTest {

    private static final Gson GSON = new Gson();

    @BeforeEach
    void setUp() throws Exception {
        // Mock Bukkit Server for createBlockData
        if (Bukkit.getServer() == null) {
            Server server = mock(Server.class);
            when(server.createBlockData(any(Material.class))).thenAnswer(invocation -> {
                Material m = invocation.getArgument(0);
                BlockData bd = mock(BlockData.class);
                when(bd.getMaterial()).thenReturn(m);
                return bd;
            });
            when(server.createBlockData(anyString())).thenAnswer(invocation -> {
                BlockData bd = mock(BlockData.class);
                return bd;
            });
            
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, server);
        }
    }

    @AfterEach
    void tearDown() {
        BuildingInfo.resetCache();
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("应在非城市区块按 scattered 资产放置结构")
    void shouldGenerateScatteredOutsideCity() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(11223344L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerBasicScatteredAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.01},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:style"}
                  ],
                  "scattered": {
                    "areasize": 1,
                    "chance": 1.0,
                    "weightnone": 0,
                    "list": [
                      {"name": "simple_scattered", "weight": 1, "maxheightdiff": 3}
                    ]
                  }
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, Biome.PLAINS, false, 11223344L);
        ChunkCoord coord = findNonCityCoord(provider);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertFalse(info.isCity, "scattered 测试要求命中非城市区块");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        GenerationContext realContext = new GenerationContext(
                worldInfo,
                region,
                provider,
                info,
                new Random(2026L),
                coord.chunkX(),
                coord.chunkZ());
        GenerationContext context = spy(realContext);

        // Mock BlockData to avoid Bukkit dependency issues in unit tests
        BlockData mockBlockData = mock(BlockData.class);
        when(mockBlockData.getMaterial()).thenReturn(Material.STONE);
        doReturn(mockBlockData).when(context).parseBlockData(anyString());

        new ScatteredStage().generate(context);
        context.flush();

        // Verify setBlockData is called with our mock
        verify(region, atLeastOnce()).setBlockData(anyInt(), anyInt(), anyInt(), eq(mockBlockData));
    }

    @Test
    @DisplayName("不匹配生物群系时不应生成 scattered 结构")
    void shouldSkipWhenBiomeFilterFails() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(44556677L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerBasicScatteredAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.01},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:style"}
                  ],
                  "scattered": {
                    "areasize": 1,
                    "chance": 1.0,
                    "weightnone": 0,
                    "list": [
                      {
                        "name": "simple_scattered",
                        "weight": 1,
                        "biomes": {
                          "if_any": ["#minecraft:is_deep_ocean"]
                        }
                      }
                    ]
                  }
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, Biome.PLAINS, false, 44556677L);
        ChunkCoord coord = findNonCityCoord(provider);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertFalse(info.isCity, "scattered 测试要求命中非城市区块");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        GenerationContext context = new GenerationContext(
                worldInfo,
                region,
                provider,
                info,
                new Random(7L),
                coord.chunkX(),
                coord.chunkZ());

        new ScatteredStage().generate(context);

        verify(region, never()).setType(anyInt(), anyInt(), anyInt(), any(Material.class));
    }

    @Test
    @DisplayName("repeatslice 应按列地形高度修复而非整块最低高度")
    void shouldApplyRepeatSlicePerColumnHeight() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(20260215L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerRepeatsliceAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.01},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:style"}
                  ],
                  "scattered": {
                    "areasize": 1,
                    "chance": 1.0,
                    "weightnone": 0,
                    "list": [
                      {"name": "simple_scattered", "weight": 1}
                    ]
                  }
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, Biome.PLAINS, true, 20260215L);
        ChunkCoord coord = findNonCityCoord(provider);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertFalse(info.isCity, "scattered 测试要求命中非城市区块");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        GenerationContext context = new GenerationContext(
                worldInfo,
                region,
                provider,
                info,
                new Random(99L),
                coord.chunkX(),
                coord.chunkZ());

        new ScatteredStage().generate(context);
        context.flush();

        int worldX = (coord.chunkX() << 4) + 1;
        int worldZ = (coord.chunkZ() << 4) + 1;
        verify(region, atLeastOnce()).setBlockData(eq(worldX), eq(62), eq(worldZ), 
            org.mockito.ArgumentMatchers.argThat(bd -> bd != null && bd.getMaterial() == Material.STONE));
        verify(region, never()).setBlockData(eq(worldX), eq(60), eq(worldZ), 
            org.mockito.ArgumentMatchers.argThat(bd -> bd != null && bd.getMaterial() == Material.STONE));
    }

    @Test
    @DisplayName("multibuilding 应在网格内完整放置，不应跨网格截断")
    void shouldKeepMultiBuildingInsideGrid() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(31415926L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerMultiScatteredAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.01},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:style"}
                  ],
                  "scattered": {
                    "areasize": 3,
                    "chance": 1.0,
                    "weightnone": 0,
                    "list": [
                      {"name": "multi_scattered", "weight": 1}
                    ]
                  }
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, Biome.PLAINS, false, 31415926L);
        ChunkCoord gridStart = findNonCityGrid(provider, 3);

        int generatedChunks = 0;
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                int chunkX = gridStart.chunkX() + dx;
                int chunkZ = gridStart.chunkZ() + dz;
                BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", chunkX, chunkZ), provider);

                LimitedRegion region = mock(LimitedRegion.class);
                when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);
                AtomicBoolean placedStone = new AtomicBoolean(false);
                doAnswer(invocation -> {
                    org.bukkit.block.data.BlockData blockData = invocation.getArgument(3);
                    if (blockData != null && blockData.getMaterial() == Material.STONE) {
                        placedStone.set(true);
                    }
                    return null;
                }).when(region).setBlockData(anyInt(), anyInt(), anyInt(), any());

                GenerationContext realContext = new GenerationContext(
                        worldInfo,
                        region,
                        provider,
                        info,
                        new Random(7000L + (dx * 31L) + dz),
                        chunkX,
                        chunkZ);
                GenerationContext context = spy(realContext);

                // Mock BlockData behavior
                BlockData mockBlockData = mock(BlockData.class);
                when(mockBlockData.getMaterial()).thenReturn(Material.STONE);
                doReturn(mockBlockData).when(context).parseBlockData(anyString());

                new ScatteredStage().generate(context);
                context.flush();

                if (placedStone.get()) {
                    generatedChunks++;
                }
            }
        }

        assertEquals(4, generatedChunks);
    }

    private void registerBasicScatteredAssets() {
        CityStyleRE cityStyle = GSON.fromJson("{\"selectors\":{}}", CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new ResourceLocation("test", "style"), cityStyle);

        PaletteRE palette = GSON.fromJson("""
                {
                  "palette": [
                    {"char": "X", "block": "minecraft:stone"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "scattered_palette"), palette);

        BuildingPartRE part = GSON.fromJson("""
                {
                  "xsize": 1,
                  "zsize": 1,
                  "ysize": 1,
                  "palette": "test:scattered_palette",
                  "slices": [
                    ["X"]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new ResourceLocation("test", "simple_part"), part);

        BuildingRE building = GSON.fromJson("""
                {
                  "minfloors": 1,
                  "maxfloors": 1,
                  "mincellars": 0,
                  "maxcellars": 0,
                  "parts": [
                    {"part": "simple_part", "weight": 1.0}
                  ]
                }
                """, BuildingRE.class);
        AssetRegistries.BUILDINGS.register(new ResourceLocation("test", "simple_building"), building);

        ScatteredRE scattered = GSON.fromJson("""
                {
                  "buildings": ["simple_building"],
                  "terrainheight": "highest",
                  "terrainfix": "none",
                  "heightoffset": 0
                }
                """, ScatteredRE.class);
        AssetRegistries.SCATTERED.register(new ResourceLocation("test", "simple_scattered"), scattered);
    }

    private void registerRepeatsliceAssets() {
        CityStyleRE cityStyle = GSON.fromJson("{\"selectors\":{}}", CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new ResourceLocation("test", "style"), cityStyle);

        PaletteRE palette = GSON.fromJson("""
                {
                  "palette": [
                    {"char": "X", "block": "minecraft:stone"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "scattered_palette"), palette);

        BuildingPartRE part = GSON.fromJson("""
                {
                  "xsize": 2,
                  "zsize": 2,
                  "ysize": 1,
                  "palette": "test:scattered_palette",
                  "slices": [
                    ["XX", "XX"]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new ResourceLocation("test", "simple_part"), part);

        BuildingRE building = GSON.fromJson("""
                {
                  "minfloors": 1,
                  "maxfloors": 1,
                  "mincellars": 0,
                  "maxcellars": 0,
                  "parts": [
                    {"part": "simple_part", "weight": 1.0}
                  ]
                }
                """, BuildingRE.class);
        AssetRegistries.BUILDINGS.register(new ResourceLocation("test", "simple_building"), building);

        ScatteredRE scattered = GSON.fromJson("""
                {
                  "buildings": ["simple_building"],
                  "terrainheight": "highest",
                  "terrainfix": "repeatslice",
                  "heightoffset": 0
                }
                """, ScatteredRE.class);
        AssetRegistries.SCATTERED.register(new ResourceLocation("test", "simple_scattered"), scattered);
    }

    private void registerMultiScatteredAssets() {
        CityStyleRE cityStyle = GSON.fromJson("{\"selectors\":{}}", CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new ResourceLocation("test", "style"), cityStyle);

        PaletteRE palette = GSON.fromJson("""
                {
                  "palette": [
                    {"char": "X", "block": "minecraft:stone"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "scattered_palette"), palette);

        BuildingPartRE part = GSON.fromJson("""
                {
                  "xsize": 1,
                  "zsize": 1,
                  "ysize": 1,
                  "palette": "test:scattered_palette",
                  "slices": [
                    ["X"]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new ResourceLocation("test", "simple_part"), part);

        for (String id : new String[] {"mb00", "mb01", "mb10", "mb11"}) {
            BuildingRE building = GSON.fromJson("""
                    {
                      "minfloors": 1,
                      "maxfloors": 1,
                      "mincellars": 0,
                      "maxcellars": 0,
                      "parts": [
                        {"part": "simple_part", "weight": 1.0}
                      ]
                    }
                    """, BuildingRE.class);
            AssetRegistries.BUILDINGS.register(new ResourceLocation("test", id), building);
        }

        MultiBuildingRE multi = GSON.fromJson("""
                {
                  "dimx": 2,
                  "dimz": 2,
                  "buildings": [
                    ["mb00", "mb01"],
                    ["mb10", "mb11"]
                  ]
                }
                """, MultiBuildingRE.class);
        AssetRegistries.MULTI_BUILDINGS.register(new ResourceLocation("test", "multi_2x2"), multi);

        ScatteredRE scattered = GSON.fromJson("""
                {
                  "multibuilding": "multi_2x2",
                  "terrainheight": "highest",
                  "terrainfix": "none",
                  "heightoffset": 0
                }
                """, ScatteredRE.class);
        AssetRegistries.SCATTERED.register(new ResourceLocation("test", "multi_scattered"), scattered);
    }

    private WorldStyle registerWorldStyle(String json) {
        WorldStyleRE worldStyleRE = GSON.fromJson(json, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new ResourceLocation("test", "scattered_world"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(null, "test:scattered_world");
        assertNotNull(worldStyle);
        return worldStyle;
    }

    private IDimensionInfo createProvider(World world,
                                          WorldStyle worldStyle,
                                          Biome biome,
                                          boolean ruggedTerrain,
                                          long seed) {
        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test", true, true, true, true);
        profile.setCityChance(0.01); // 经典模式，1% 概率为城市中心
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(seed);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getWorldStyle()).thenReturn(worldStyle);
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(biome);
        when(provider.dimension()).thenReturn("world");
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> createHeightmap(ruggedTerrain));
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> createHeightmap(ruggedTerrain));
        return provider;
    }

    private ChunkHeightmap createHeightmap(boolean rugged) {
        ChunkHeightmap heightmap = new ChunkHeightmap();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = rugged ? 60 + ((x + z) % 6) : 64;
                heightmap.setHeight(x, z, h);
            }
        }
        return heightmap;
    }

    private ChunkCoord findNonCityCoord(IDimensionInfo provider) {
        for (int x = 1; x < 128; x++) {
            for (int z = 1; z < 128; z++) {
                ChunkCoord coord = new ChunkCoord("world", x, z);
                BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
                if (!info.isCity) {
                    return coord;
                }
            }
        }
        throw new IllegalStateException("未找到可用于 scattered 测试的非城市区块");
    }

    private ChunkCoord findNonCityGrid(IDimensionInfo provider, int areaSize) {
        for (int gx = 0; gx < 128; gx++) {
            for (int gz = 0; gz < 128; gz++) {
                int startX = gx * areaSize;
                int startZ = gz * areaSize;
                boolean allNonCity = true;
                for (int dx = 0; dx < areaSize && allNonCity; dx++) {
                    for (int dz = 0; dz < areaSize; dz++) {
                        ChunkCoord coord = new ChunkCoord("world", startX + dx, startZ + dz);
                        if (BuildingInfo.getBuildingInfo(coord, provider).isCity) {
                            allNonCity = false;
                            break;
                        }
                    }
                }
                if (allNonCity) {
                    return new ChunkCoord("world", startX, startZ);
                }
            }
        }
        throw new IllegalStateException("未找到可用于 multibuilding 测试的非城市网格");
    }
}
