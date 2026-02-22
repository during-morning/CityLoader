package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.during.cityloader.worldgen.lost.regassets.BuildingRE;
import com.during.cityloader.worldgen.lost.regassets.CityStyleRE;
import com.during.cityloader.worldgen.lost.regassets.MultiBuildingRE;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.StyleRE;
import com.during.cityloader.worldgen.lost.regassets.WorldStyleRE;
import com.google.gson.Gson;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BuildingInfo 决策测试")
class BuildingInfoTest {

    private World world;
    private IDimensionInfo provider;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);

        AssetRegistries.reset();
        AssetRegistries.load(world);

        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(world, "lostcities:standard");
        assertNotNull(worldStyle, "测试前置失败：应能加载标准 worldstyle");

        provider = mock(IDimensionInfo.class);
        when(provider.getProfile()).thenReturn(new LostCityProfile("test"));
        when(provider.getSeed()).thenReturn(12345L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getWorldStyle()).thenReturn(worldStyle);
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.dimension()).thenReturn("world");
        when(provider.getHeightmap(org.mockito.ArgumentMatchers.any(ChunkCoord.class))).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });
    }

    @AfterEach
    void tearDown() {
        BuildingInfo.resetCache();
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("应能稳定生成区块决策并命中缓存")
    void shouldBuildAndCacheChunkDecision() {
        ChunkCoord coord = new ChunkCoord("world", 8, 8);

        BuildingInfo first = BuildingInfo.getBuildingInfo(coord, provider);
        BuildingInfo second = BuildingInfo.getBuildingInfo(coord, provider);

        assertNotNull(first);
        assertSame(first, second, "同区块应命中缓存返回同一实例");
        assertTrue(first.groundLevel > 0, "地面高度应有效");
        assertNotNull(first.getCompiledPalette(), "应能生成编译调色板对象");
    }

    @Test
    @DisplayName("应能提供楼层访问边界保护")
    void shouldGuardFloorAccess() {
        ChunkCoord coord = new ChunkCoord("world", 12, 12);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        assertFalse(info.isValidFloor(999), "超出上限楼层应返回 false");
        assertNull(info.getFloor(999), "超出上限楼层应返回 null");
        assertNull(info.getFloorPart2(999), "超出上限楼层应返回 null");
    }

    @Test
    @DisplayName("postTodo 应支持排队、消费并清空")
    void shouldDrainPostTodoQueue() {
        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 6, 6), provider);
        AtomicInteger counter = new AtomicInteger();

        info.addPostTodo(counter::incrementAndGet);
        info.addPostTodo(counter::incrementAndGet);
        info.addPalettePostTodo(1, 2, 3, "part",
                new CompiledPalette.Information("test:loot", "minecraft:zombie", true, Map.of("a", "b")));

        assertEquals(2, info.getPostTodoCount());
        info.drainPostTodo().forEach(Runnable::run);
        assertEquals(2, counter.get());
        assertEquals(0, info.getPostTodoCount());
        assertTrue(info.drainPostTodo().isEmpty());

        assertEquals(1, info.getPalettePostTodoCount());
        assertEquals(1, info.drainPalettePostTodo().size());
        assertEquals(0, info.getPalettePostTodoCount());
        assertTrue(info.drainPalettePostTodo().isEmpty());
    }

    @Test
    @DisplayName("高速路/铁路区块不应再放置建筑")
    void shouldDisableBuildingOnInfrastructureChunks() {
        WorldStyleRE forceCity = new Gson().fromJson(
                "{\"settings\":{\"citychance\":1.0}}",
                WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "forcecity"), forceCity);
        WorldStyle style = AssetRegistries.WORLDSTYLES.get(world, "test:forcecity");
        assertNotNull(style);
        when(provider.getWorldStyle()).thenReturn(style);

        ChunkCoord coord = new ChunkCoord("world", 32, 32);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        assertTrue(info.isCityRaw(), "citychance=1.0 时区块应为城市区块");
        assertTrue(info.highwayXLevel > 0 || info.highwayZLevel > 0 || info.xRailCorridor || info.zRailCorridor);
        assertFalse(info.hasBuilding, "基础设施区块应关闭建筑放置");

        assertEquals(info.groundLevel + info.cityLevel * 6, info.getCityGroundLevel(), "城市地坪应遵循 Ground + cityLevel*6");
        if (info.highwayXLevel > 0) {
            assertEquals(info.getCityGroundLevel() + 6, info.highwayXLevel, "高速路应位于城市地坪上一层");
        }
        if (info.highwayZLevel > 0) {
            assertEquals(info.getCityGroundLevel() + 6, info.highwayZLevel, "高速路应位于城市地坪上一层");
        }
    }

    @Test
    @DisplayName("CityStyle 旧 buildings 字段应参与建筑选择")
    void shouldUseLegacyBuildingsInSelectionChain() {
        String chosenBuilding = AssetRegistries.BUILDINGS.getIterable().iterator().next().getName();

        String cityStyleJson = String.format("{\"buildings\":[\"%s\"],\"buildingchance\":1.0}", chosenBuilding);
        CityStyleRE cityStyle = new Gson().fromJson(cityStyleJson, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "legacy_chain_style"), cityStyle);

        WorldStyleRE worldStyleRe = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [{"factor": 1.0, "citystyle": "test:legacy_chain_style"}]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "legacy_chain_world"), worldStyleRe);
        when(provider.getWorldStyle()).thenReturn(AssetRegistries.WORLDSTYLES.get(world, "test:legacy_chain_world"));

        BuildingInfo info = null;
        for (int x = 1; x < 48 && info == null; x++) {
            for (int z = 1; z < 48; z++) {
                BuildingInfo candidate = BuildingInfo.getBuildingInfo(new ChunkCoord("world", x, z), provider);
                if (candidate.isCityRaw() && candidate.hasBuilding) {
                    info = candidate;
                    break;
                }
            }
        }

        assertNotNull(info, "应找到至少一个可放置建筑的城市区块");
        assertNotNull(info.buildingType);
        assertEquals(chosenBuilding, info.buildingType.getName());
    }

    @Test
    @DisplayName("CityStyle 继承 style 后应能加载父级调色板")
    void shouldInheritStylePaletteFromParentCityStyle() {
        String chosenBuilding = AssetRegistries.BUILDINGS.getIterable().iterator().next().getName();

        PaletteRE paletteRe = new Gson().fromJson("""
                {
                  "name": "inherit_palette",
                  "palette": [
                    {"char": "I", "block": "minecraft:stone"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new com.during.cityloader.util.ResourceLocation("test", "inherit_palette"), paletteRe);

        StyleRE styleRe = new Gson().fromJson("""
                {
                  "randompalettes": [
                    [{"factor": 1.0, "palette": "test:inherit_palette"}]
                  ]
                }
                """, StyleRE.class);
        AssetRegistries.STYLES.register(new com.during.cityloader.util.ResourceLocation("test", "inherit_style"), styleRe);

        String parentStyleJson = String.format("{\"style\":\"test:inherit_style\",\"selectors\":{\"buildings\":[{\"factor\":1.0,\"value\":\"%s\"}]}}", chosenBuilding);
        CityStyleRE parent = new Gson().fromJson(parentStyleJson, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_parent"), parent);

        CityStyleRE child = new Gson().fromJson("""
                {
                  "inherit": "test:palette_parent",
                  "buildingchance": 1.0
                }
                """, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_child"), child);

        WorldStyleRE worldStyleRe = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [{"factor": 1.0, "citystyle": "test:palette_child"}]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_chain_world"), worldStyleRe);
        when(provider.getWorldStyle()).thenReturn(AssetRegistries.WORLDSTYLES.get(world, "test:palette_chain_world"));

        ChunkCoord coord = new ChunkCoord("world", 9, 9);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        assertNotNull(info.getCompiledPalette().get('I'));
    }

    @Test
    @DisplayName("Style 使用 common 时应回退到 palette_common")
    void shouldResolvePaletteCommonFallback() {
        String chosenBuilding = AssetRegistries.BUILDINGS.getIterable().iterator().next().getName();

        PaletteRE paletteRe = new Gson().fromJson("""
                {
                  "name": "palette_common",
                  "palette": [
                    {"char": "P", "block": "minecraft:polished_andesite"}
                  ]
                }
                """, PaletteRE.class);
        AssetRegistries.PALETTES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_common"), paletteRe);

        StyleRE styleRe = new Gson().fromJson("""
                {
                  "randompalettes": [
                    [{"factor": 1.0, "palette": "common"}]
                  ]
                }
                """, StyleRE.class);
        AssetRegistries.STYLES.register(new com.during.cityloader.util.ResourceLocation("test", "style_common"), styleRe);

        String cityStyleJson = String.format(
                "{\"style\":\"test:style_common\",\"selectors\":{\"buildings\":[{\"factor\":1.0,\"value\":\"%s\"}]}}",
                chosenBuilding);
        CityStyleRE cityStyle = new Gson().fromJson(cityStyleJson, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_common_style"), cityStyle);

        WorldStyleRE worldStyleRe = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [{"factor": 1.0, "citystyle": "test:palette_common_style"}]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "palette_common_world"), worldStyleRe);
        when(provider.getWorldStyle()).thenReturn(AssetRegistries.WORLDSTYLES.get(world, "test:palette_common_world"));

        ChunkCoord coord = new ChunkCoord("world", 9, 9);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        assertNotNull(info.getCompiledPalette().get('P'));
    }

    @Test
    @DisplayName("多建筑槽位丢失时应优先回退到 empty 建筑而不是全量随机")
    void shouldFallbackToEmptyBuildingWhenMultibuildingSlotIsMissing() {
        String chosenBuilding = null;
        for (var candidate : AssetRegistries.BUILDINGS.getIterable()) {
            if (candidate == null) {
                continue;
            }
            String path = candidate.getId().getPath();
            if (!"common_empty".equals(path) && !"common_void".equals(path)) {
                chosenBuilding = candidate.getName();
                break;
            }
        }
        assertNotNull(chosenBuilding, "测试前置失败：应存在非 empty 建筑");

        MultiBuildingRE multi = new Gson().fromJson("""
                {
                  "dimx": 2,
                  "dimz": 2,
                  "buildings": [
                    ["test:missing_slot_building", "test:missing_slot_building"],
                    ["test:missing_slot_building", "test:missing_slot_building"]
                  ]
                }
                """, MultiBuildingRE.class);
        AssetRegistries.MULTI_BUILDINGS.register(new com.during.cityloader.util.ResourceLocation("test", "broken_multi"), multi);

        String cityStyleJson = String.format("""
                {
                  "buildingchance": 1.0,
                  "selectors": {
                    "multibuildings": [{"factor": 1.0, "value": "test:broken_multi"}],
                    "buildings": [{"factor": 1.0, "value": "%s"}]
                  }
                }
                """, chosenBuilding);
        CityStyleRE cityStyle = new Gson().fromJson(cityStyleJson, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "broken_multi_style"), cityStyle);

        WorldStyleRE worldStyleRe = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [{"factor": 1.0, "citystyle": "test:broken_multi_style"}]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "broken_multi_world"), worldStyleRe);
        when(provider.getWorldStyle()).thenReturn(AssetRegistries.WORLDSTYLES.get(world, "test:broken_multi_world"));

        BuildingInfo info = null;
        for (int x = 1; x < 48 && info == null; x++) {
            for (int z = 1; z < 48; z++) {
                BuildingInfo candidate = BuildingInfo.getBuildingInfo(new ChunkCoord("world", x, z), provider);
                if (candidate.isCityRaw() && candidate.hasBuilding && candidate.multiBuilding != null) {
                    info = candidate;
                    break;
                }
            }
        }

        assertNotNull(info, "应找到至少一个多建筑城市区块");
        assertNotNull(info.buildingType);
        String selectedName = info.buildingType.getName();
        assertTrue(selectedName.endsWith(":common_empty") || selectedName.endsWith(":common_void"),
                "缺失多建筑槽位应回退到 empty/void 建筑，实际为: " + selectedName);
    }

    @Test
    @DisplayName("BuildingInfo 应保留楼层 transform 信息")
    void shouldKeepFloorTransformFromSelectedPart() {
        BuildingPartRE partRe = new Gson().fromJson("""
                {
                  "xsize": 1,
                  "zsize": 1,
                  "ysize": 1,
                  "slices": [
                    ["A"]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new com.during.cityloader.util.ResourceLocation("test", "transform_part"), partRe);

        BuildingRE buildingRe = new Gson().fromJson("""
                {
                  "minfloors": 1,
                  "maxfloors": 1,
                  "parts": [
                    {"part": "test:transform_part", "factor": 1.0, "transform": 1}
                  ]
                }
                """, BuildingRE.class);
        AssetRegistries.BUILDINGS.register(new com.during.cityloader.util.ResourceLocation("test", "transform_building"), buildingRe);

        CityStyleRE cityStyle = new Gson().fromJson("""
                {
                  "buildingchance": 1.0,
                  "selectors": {
                    "buildings": [{"factor": 1.0, "value": "test:transform_building"}]
                  }
                }
                """, CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "transform_style"), cityStyle);

        WorldStyleRE worldStyleRe = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [{"factor": 1.0, "citystyle": "test:transform_style"}]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "transform_world"), worldStyleRe);
        when(provider.getWorldStyle()).thenReturn(AssetRegistries.WORLDSTYLES.get(world, "test:transform_world"));

        BuildingInfo info = null;
        for (int x = 1; x < 48 && info == null; x++) {
            for (int z = 1; z < 48; z++) {
                BuildingInfo candidate = BuildingInfo.getBuildingInfo(new ChunkCoord("world", x, z), provider);
                if (candidate.isCityRaw() && candidate.hasBuilding) {
                    info = candidate;
                    break;
                }
            }
        }

        assertNotNull(info, "应命中至少一个可放置建筑的城市区块");
        assertEquals(Transform.ROTATE_90, info.getFloorTransform(0));
        assertEquals(Transform.ROTATE_NONE, info.getFloorPart2Transform(0));
    }
}
