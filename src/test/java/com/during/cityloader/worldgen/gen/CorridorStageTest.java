package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CorridorStage 连通性测试")
class CorridorStageTest {

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
    @DisplayName("当前块无走廊时，也应为相邻走廊在边界开洞")
    void shouldOpenConnectionWhenNeighborHasCorridor() {
        AssetRegistries.reset();

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(424242L);

        AssetRegistries.load(world);

        WorldStyleRE worldStyleRE = new Gson().fromJson("""
                {
                  "settings": {"citychance": 1.0},
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "citystyle_standard"}
                  ]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new ResourceLocation("test", "corridor"), worldStyleRE);
        WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.get(world, "test:corridor");

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(1.0);
        profile.setGroundLevel(70);
        profile.setCorridorChance(1.0f);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(424242L);
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

        BuildingInfo current = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        BuildingInfo west = BuildingInfo.getBuildingInfo(new ChunkCoord("world", -1, 0), provider);
        BuildingInfo westWest = BuildingInfo.getBuildingInfo(new ChunkCoord("world", -2, 0), provider);

        current.isCity = true;
        current.groundLevel = 70;
        current.hasBuilding = true;
        current.cellars = 2;
        current.xRailCorridor = false;
        current.zRailCorridor = false;

        west.isCity = true;
        west.hasBuilding = false;
        west.cellars = 0;
        west.xRailCorridor = true;
        west.zRailCorridor = false;

        westWest.isCity = true;
        westWest.hasBuilding = true;
        westWest.cellars = 2;
        westWest.xRailCorridor = false;
        westWest.zRailCorridor = false;

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
                current,
                new Random(2026L),
                0,
                0);

        new CorridorStage().generate(context);
        context.flush();

        verify(region, atLeastOnce()).setBlockData(
                eq(0),
                eq(65),
                eq(7),
                argThat(data -> data != null && data.getMaterial() == Material.AIR));
    }
}
