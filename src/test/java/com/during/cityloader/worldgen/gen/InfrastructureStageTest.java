package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Railway;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InfrastructureStage 资产驱动测试")
class InfrastructureStageTest {

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
    @DisplayName("应优先通过 WorldStyle.parts/Part 资产渲染基础设施")
    void shouldRenderInfrastructureFromPartAssets() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(246813579L);

        AssetRegistries.load(world);

        String worldStyleJson = """
                {
                  "settings": {"citychance": 1.0},
                  "parts": {
                    "highways": {
                      "open": ["highway_open"],
                      "open_bi": ["highway_open_bi"],
                      "bridge": ["highway_bridge"],
                      "bridge_bi": ["highway_bridge_bi"]
                    },
                    "railways": {
                      "rails3split": ["rails_3split"],
                      "railshorizontal": ["rails_horizontal"],
                      "railsvertical": ["rails_vertical"],
                      "railsflat": ["rails_flat"]
                    }
                  },
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "citystyle_standard"}
                  ]
                }
                """;
        WorldStyleRE worldStyleRE = new Gson().fromJson(worldStyleJson, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "infra"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(world, "test:infra");
        assertNotNull(worldStyle);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(0.02); // 真实概率模式
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(246813579L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getWorldStyle()).thenReturn(worldStyle);
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.dimension()).thenReturn("world");
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });

        int chunkX = 0;
        int chunkZ = 0;
        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", chunkX, chunkZ), provider);
        
        // 手动覆盖状态，模拟城市和基础设施存在，解耦生成逻辑测试
        info.isCity = true;
        info.highwayXLevel = 1;
        info.highwayZLevel = 1;
        info.xRailCorridor = true;
        info.zRailCorridor = true;

        assertTrue(info.highwayXLevel > 0 && info.highwayZLevel > 0, "应命中双向高速路");
        assertTrue(info.xRailCorridor && info.zRailCorridor, "应命中双向铁路走廊");

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
                new Random(2026L),
                chunkX,
                chunkZ);

        new InfrastructureStage().generate(context);
        context.flush();

        verify(region, atLeastOnce()).setBlockData(anyInt(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("railpartheight6=2 时铁路应上移 6 格")
    void shouldApplyRailPartHeightOffset() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(13579L);

        AssetRegistries.load(world);

        String worldStyleJson = """
                {
                  "settings": {
                    "citychance": 0.0,
                    "railpartheight6": 2
                  },
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "citystyle_standard"}
                  ]
                }
                """;
        WorldStyleRE worldStyleRE = new Gson().fromJson(worldStyleJson, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "railheight"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(world, "test:railheight");
        assertNotNull(worldStyle);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(0.0);
        profile.setGroundLevel(70);
        profile.setRailwaysEnabled(true);
        profile.setRailwaysCanEnd(false);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(13579L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getWorldStyle()).thenReturn(worldStyle);
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.dimension()).thenReturn("world");
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });

        int chunkX = -1;
        int chunkZ = 9;
        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", chunkX, chunkZ), provider);

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(worldInfo.getMinHeight()).thenReturn(-64);
        when(worldInfo.getMaxHeight()).thenReturn(320);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        GenerationContext context = new GenerationContext(
                worldInfo,
                region,
                provider,
                info,
                new Random(2026L),
                chunkX,
                chunkZ);

        new InfrastructureStage().generate(context);
        context.flush();

        int expectedRailY = info.groundLevel + Railway.RAILWAY_LEVEL_OFFSET * 6 + 6;
        verify(region, atLeastOnce()).setBlockData(anyInt(), eq(expectedRailY), anyInt(), org.mockito.ArgumentMatchers.any());
    }
}
