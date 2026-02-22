package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.PredefinedCity;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.during.cityloader.worldgen.lost.regassets.BuildingRE;
import com.during.cityloader.worldgen.lost.regassets.CityStyleRE;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.PredefinedCityRE;
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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DamageStage 废墟系统测试")
class DamageStageTest {

    private static final Gson GSON = new Gson();

    @BeforeEach
    void setUp() throws Exception {
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
    @DisplayName("应使用 palette.damaged 进行方块降级替换")
    void shouldApplyPaletteAwareDamage() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(99887766L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerDamageAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.02},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:damage_style"}
                  ]
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, true, 99887766L);
        ChunkCoord coord = new ChunkCoord("world", 5, 5);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertTrue(info.hasBuilding, "damage 测试需要命中建筑区块 (通过 PredefinedCity)");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(region.getType(anyInt(), anyInt(), anyInt())).thenReturn(Material.COBBLESTONE);
        BlockData cobbleData = mock(BlockData.class);
        when(cobbleData.getMaterial()).thenReturn(Material.COBBLESTONE);
        when(region.getBlockData(anyInt(), anyInt(), anyInt())).thenReturn(cobbleData);

        DamageStage stage = new DamageStage();
        for (int i = 0; i < 24; i++) {
            GenerationContext context = new GenerationContext(
                    worldInfo,
                    region,
                    provider,
                    info,
                    new Random(1000L + i),
                    coord.chunkX(),
                    coord.chunkZ());
            stage.generate(context);
            context.flush();
        }

        verify(region, atLeastOnce()).setBlockData(anyInt(), anyInt(), anyInt(), 
            argThat(bd -> bd.getMaterial() == Material.GOLD_BLOCK));
    }

    @Test
    @DisplayName("配置关闭 damage 时不应执行破坏")
    void shouldSkipWhenDamageDisabled() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(1234567L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerDamageAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.02},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:damage_style"}
                  ]
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, false, 1234567L);
        ChunkCoord coord = new ChunkCoord("world", 5, 5);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertTrue(info.hasBuilding, "关闭测试也应先命中建筑区块 (通过 PredefinedCity)");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(region.getType(anyInt(), anyInt(), anyInt())).thenReturn(Material.COBBLESTONE);

        GenerationContext context = new GenerationContext(
                worldInfo,
                region,
                provider,
                info,
                new Random(42L),
                coord.chunkX(),
                coord.chunkZ());

        new DamageStage().generate(context);
        context.flush();

        verify(region, never()).setBlockData(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("同 seed+chunk 下 damage 输出应与上下文随机数无关")
    void shouldProduceDeterministicDamagePattern() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeed()).thenReturn(24682468L);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        registerDamageAssets();
        WorldStyle worldStyle = registerWorldStyle("""
                {
                  "settings": {"citychance": 0.02},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "test:damage_style"}
                  ]
                }
                """);

        IDimensionInfo provider = createProvider(world, worldStyle, true, 24682468L);
        ChunkCoord coord = new ChunkCoord("world", 5, 5);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        assertTrue(info.hasBuilding, "deterministic 测试需要命中建筑区块 (通过 PredefinedCity)");

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion regionA = mock(LimitedRegion.class);
        when(regionA.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(regionA.getType(anyInt(), anyInt(), anyInt())).thenReturn(Material.COBBLESTONE);
        BlockData cobbleDataA = mock(BlockData.class);
        when(cobbleDataA.getMaterial()).thenReturn(Material.COBBLESTONE);
        when(regionA.getBlockData(anyInt(), anyInt(), anyInt())).thenReturn(cobbleDataA);

        LimitedRegion regionB = mock(LimitedRegion.class);
        when(regionB.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(regionB.getType(anyInt(), anyInt(), anyInt())).thenReturn(Material.COBBLESTONE);
        BlockData cobbleDataB = mock(BlockData.class);
        when(cobbleDataB.getMaterial()).thenReturn(Material.COBBLESTONE);
        when(regionB.getBlockData(anyInt(), anyInt(), anyInt())).thenReturn(cobbleDataB);

        Set<String> writesA = new HashSet<>();
        Set<String> writesB = new HashSet<>();
        doAnswer(invocation -> {
            BlockData bd = invocation.getArgument(3);
            writesA.add(invocation.getArgument(0) + "," + invocation.getArgument(1) + "," + invocation.getArgument(2)
                    + ":" + bd.getMaterial().name());
            return null;
        }).when(regionA).setBlockData(anyInt(), anyInt(), anyInt(), any());
        doAnswer(invocation -> {
            BlockData bd = invocation.getArgument(3);
            writesB.add(invocation.getArgument(0) + "," + invocation.getArgument(1) + "," + invocation.getArgument(2)
                    + ":" + bd.getMaterial().name());
            return null;
        }).when(regionB).setBlockData(anyInt(), anyInt(), anyInt(), any());

        DamageStage stage = new DamageStage();
        GenerationContext contextA = new GenerationContext(
                worldInfo,
                regionA,
                provider,
                info,
                new Random(1L),
                coord.chunkX(),
                coord.chunkZ());
        stage.generate(contextA);
        contextA.flush();
        
        GenerationContext contextB = new GenerationContext(
                worldInfo,
                regionB,
                provider,
                info,
                new Random(999L),
                coord.chunkX(),
                coord.chunkZ());
        stage.generate(contextB);
        contextB.flush();

        assertEquals(writesA, writesB);
    }

    private void registerDamageAssets() {
        PaletteRE palette = GSON.fromJson("""
                {
                  "palette": [
                    {"char": "X", "block": "minecraft:cobblestone", "damaged": "minecraft:gold_block"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "damage_palette"), palette);

        BuildingPartRE part = GSON.fromJson("""
                {
                  "xsize": 1,
                  "zsize": 1,
                  "ysize": 1,
                  "palette": "test:damage_palette",
                  "slices": [
                    ["X"]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new ResourceLocation("test", "damage_part"), part);

        BuildingRE building = GSON.fromJson("""
                {
                  "minfloors": 1,
                  "maxfloors": 1,
                  "mincellars": 0,
                  "maxcellars": 0,
                  "parts": [
                    {"part": "damage_part", "weight": 1.0}
                  ]
                }
                """, BuildingRE.class);
        AssetRegistries.BUILDINGS.register(new ResourceLocation("test", "damage_building"), building);

        CityStyleRE style = GSON.fromJson("""
                {
                  "buildingchance": 1.0,
                  "explosionchance": 1.0,
                  "selectors": {
                    "buildings": [
                      {"value": "damage_building", "factor": 1.0}
                    ]
                  }
                }
                """, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new ResourceLocation("test", "damage_style"), style);

        PredefinedCityRE cityRE = GSON.fromJson("""
                {
                  "dimension": "world",
                  "chunkx": 5,
                  "chunkz": 5,
                  "radius": 100,
                  "citystyle": "test:damage_style"
                }
                """, PredefinedCityRE.class);
        cityRE.setRegistryName(new ResourceLocation("test", "predefined_city"));
        AssetRegistries.PREDEFINED_CITIES.register(cityRE.getRegistryName(), cityRE);
        com.during.cityloader.worldgen.lost.City.cleanCache();
    }

    private WorldStyle registerWorldStyle(String json) {
        WorldStyleRE worldStyleRE = GSON.fromJson(json, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new ResourceLocation("test", "damage_world"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(null, "test:damage_world");
        assertNotNull(worldStyle);
        return worldStyle;
    }

    private IDimensionInfo createProvider(World world, WorldStyle worldStyle, boolean damageEnabled, long seed) {
        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test", true, true, true, damageEnabled);
        profile.setCityChance(0.02); // 真实概率模式
        // 让测试稳定命中 damage 路径，避免依赖默认低概率爆炸。
        profile.setExplosionChance(1.0f);
        profile.setMiniExplosionChance(1.0f);
        profile.setExplosionMinRadius(3);
        profile.setExplosionMaxRadius(6);
        profile.setMiniExplosionMinRadius(2);
        profile.setMiniExplosionMaxRadius(4);
        profile.setExplosionMinHeight(64);
        profile.setExplosionMaxHeight(72);
        profile.setMiniExplosionMinHeight(62);
        profile.setMiniExplosionMaxHeight(72);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(seed);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getWorldStyle()).thenReturn(worldStyle);
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.dimension()).thenReturn("world");
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> createHeightmap());
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> createHeightmap());
        return provider;
    }

    private ChunkHeightmap createHeightmap() {
        ChunkHeightmap heightmap = new ChunkHeightmap();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                heightmap.setHeight(x, z, 64);
            }
        }
        return heightmap;
    }
}
