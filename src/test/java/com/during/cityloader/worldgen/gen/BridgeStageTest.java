package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BridgeStage 行为测试")
class BridgeStageTest {

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
            when(server.createBlockData(anyString())).thenAnswer(invocation -> mock(BlockData.class));

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
    @DisplayName("标记为桥梁区块时不应被 bridgeChance 随机跳过")
    void shouldGenerateBridgeEvenWhenBridgeChanceIsZero() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(2468L);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(1.0);
        profile.setBridgeChance(0.0f);
        profile.setBridgeSupports(false);
        profile.setGroundLevel(70);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2468L);
        when(provider.getWorld()).thenReturn(world);
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
        info.isCity = true;
        info.highwayXLevel = 82;
        info.xBridge = true;
        info.highwayZLevel = 0;
        info.zBridge = false;

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
                new Random(99L),
                chunkX,
                chunkZ);

        new BridgeStage().generate(context);
        context.flush();

        verify(region, atLeastOnce()).setBlockData(anyInt(), eq(82), anyInt(), any());
    }

    @Test
    @DisplayName("非城市区块在邻接桥梁时应补齐桥面避免断桥")
    void shouldGenerateBridgeWhenNeighborHasBridge() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(1357L);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(1.0);
        profile.setBridgeSupports(false);
        profile.setGroundLevel(70);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(1357L);
        when(provider.getWorld()).thenReturn(world);
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

        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        info.isCity = false;
        info.xBridge = false;
        info.highwayXLevel = 0;
        info.zBridge = false;
        info.highwayZLevel = 0;

        BuildingInfo west = info.getXmin();
        west.isCity = true;
        west.xBridge = true;
        west.highwayXLevel = 82;

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
                new Random(7L),
                0,
                0);

        new BridgeStage().generate(context);
        context.flush();

        verify(region, atLeastOnce()).setBlockData(anyInt(), eq(82), anyInt(), any());
        verify(region, atLeastOnce()).setBlockData(anyInt(), eq(81), anyInt(), any());
    }
}
