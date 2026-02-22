package com.during.cityloader.worldgen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.PredefinedCityRE;
import com.during.cityloader.worldgen.lost.regassets.CityStyleRE;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("LostCityTerrainFeature 总控测试")
class LostCityTerrainFeatureTest {

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
    @DisplayName("应按阶段执行并向 LimitedRegion 写入方块")
    void shouldRunPipelineAndPlaceBlocks() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        // 使用高 citychance 的 worldstyle，降低非城市区块概率
        WorldStyleRE worldStyleRE = new Gson().fromJson(
                "{\"outsidestyle\":\"outside\",\"settings\":{\"citychance\":0.02}}",
                WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "world"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(world, "test:world");
        assertNotNull(worldStyle);

        // Register CityStyle
        CityStyleRE cityStyleRE = new Gson().fromJson("{\"selectors\":{}}", CityStyleRE.class);
        AssetRegistries.CITYSTYLES.register(new com.during.cityloader.util.ResourceLocation("test", "city_style"), cityStyleRE);

        // Register PredefinedCity at 0,0
        PredefinedCityRE cityRE = new Gson().fromJson("""
                {
                  "dimension": "world",
                  "chunkx": 0,
                  "chunkz": 0,
                  "radius": 50,
                  "citystyle": "test:city_style"
                }
                """, PredefinedCityRE.class);
        cityRE.setRegistryName(new com.during.cityloader.util.ResourceLocation("test", "predefined_city"));
        AssetRegistries.PREDEFINED_CITIES.register(cityRE.getRegistryName(), cityRE);
        com.during.cityloader.worldgen.lost.City.cleanCache();

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(0.02); // 真实概率模式
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(987654321L);
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
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> {
            ChunkHeightmap heightmap = new ChunkHeightmap();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, 64);
                }
            }
            return heightmap;
        });

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        LostCityTerrainFeature feature = LostCityTerrainFeature.DEFAULT;
        for (int i = 0; i < 8; i++) {
            feature.generate(worldInfo, new Random(100 + i), i, i, region, provider);
        }

        verify(region, atLeastOnce()).setBlockData(anyInt(), anyInt(), anyInt(), any());
    }
}
