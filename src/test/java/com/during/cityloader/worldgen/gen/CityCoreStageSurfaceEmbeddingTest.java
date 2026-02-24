package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CityCoreStage 地形嵌合测试")
class CityCoreStageSurfaceEmbeddingTest {

    @AfterEach
    void tearDown() {
        BuildingInfo.resetCache();
    }

    @Test
    @DisplayName("街区表面目标应锁定边界并保持中心平台")
    void shouldLockStreetEdgesAndKeepCenterPlateau() throws Exception {
        IDimensionInfo provider = mockProvider();
        BuildingInfo center = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        BuildingInfo west = center.getXmin();
        BuildingInfo east = center.getXmax();
        BuildingInfo north = center.getZmin();
        BuildingInfo south = center.getZmax();

        center.isCity = true;
        center.hasBuilding = false;
        center.groundLevel = 70;
        center.cityLevel = 0;

        west.isCity = true;
        west.groundLevel = 64;
        west.cityLevel = 0;

        east.isCity = true;
        east.groundLevel = 76;
        east.cityLevel = 0;

        north.isCity = true;
        north.groundLevel = 68;
        north.cityLevel = 0;

        south.isCity = true;
        south.groundLevel = 72;
        south.cityLevel = 0;

        int fallbackY = center.getCityGroundLevel();
        int expectedWest = Math.round((center.getCityGroundLevel() + west.getCityGroundLevel()) / 2.0f);
        int expectedEast = Math.round((center.getCityGroundLevel() + east.getCityGroundLevel()) / 2.0f);
        int expectedNorth = Math.round((center.getCityGroundLevel() + north.getCityGroundLevel()) / 2.0f);
        int expectedSouth = Math.round((center.getCityGroundLevel() + south.getCityGroundLevel()) / 2.0f);

        int[][] targets = invokeStreetTargets(center, fallbackY);

        for (int i = 1; i < 15; i++) {
            assertEquals(expectedWest, targets[0][i], "西边界高度应与邻区块锁定");
            assertEquals(expectedEast, targets[15][i], "东边界高度应与邻区块锁定");
            assertEquals(expectedNorth, targets[i][0], "北边界高度应与邻区块锁定");
            assertEquals(expectedSouth, targets[i][15], "南边界高度应与邻区块锁定");
        }

        for (int x = 4; x <= 11; x++) {
            for (int z = 4; z <= 11; z++) {
                assertEquals(fallbackY, targets[x][z], "中心平台应保持平整");
            }
        }
    }

    @Test
    @DisplayName("建筑区地表应中心平整且边缘向低邻块下沉")
    void shouldCreateBuildingApronTowardsLowNeighbors() throws Exception {
        IDimensionInfo provider = mockProviderWithNeighborHeights(70, 52);
        BuildingInfo center = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        center.isCity = true;
        center.hasBuilding = true;
        center.groundLevel = 70;
        center.cityLevel = 0;

        int centerY = center.getCityGroundLevel() - 1;
        int[][] targets = invokeBuildingTargets(center, centerY);

        for (int x = 4; x <= 11; x++) {
            for (int z = 4; z <= 11; z++) {
                assertEquals(centerY, targets[x][z], "建筑区中心应保持平台高度");
            }
        }

        assertTrue(targets[0][8] < centerY, "西边缘应低于中心");
        assertTrue(targets[15][8] < centerY, "东边缘应低于中心");
        assertTrue(targets[8][0] < centerY, "北边缘应低于中心");
        assertTrue(targets[8][15] < centerY, "南边缘应低于中心");
    }

    @Test
    @DisplayName("大高差邻块时建筑边缘应允许更深下沉以形成过渡")
    void shouldAllowDeeperApronDropForLargeHeightDifference() throws Exception {
        IDimensionInfo provider = mockProviderWithNeighborHeights(70, 40);
        BuildingInfo center = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        center.isCity = true;
        center.hasBuilding = true;
        center.groundLevel = 70;
        center.cityLevel = 0;

        int centerY = center.getCityGroundLevel() - 1;
        int[][] targets = invokeBuildingTargets(center, centerY);

        assertEquals(centerY - 8, targets[0][8], "边缘下沉应达到更深过渡上限");
        assertEquals(centerY - 8, targets[15][8], "边缘下沉应达到更深过渡上限");
    }

    @Test
    @DisplayName("建筑净空应保留与非建筑邻块相接的边缘过渡带")
    void shouldKeepTransitionBandWhenClearingBuildingEnvelope() throws Exception {
        IDimensionInfo provider = mockProvider();
        BuildingInfo center = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        center.isCity = true;
        center.hasBuilding = true;
        center.groundLevel = 70;
        center.cityLevel = 0;
        center.cellars = 1;
        center.floors = 2;

        GenerationContext context = mock(GenerationContext.class);
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getMinHeight()).thenReturn(-64);
        when(worldInfo.getMaxHeight()).thenReturn(320);
        when(context.getWorldInfo()).thenReturn(worldInfo);

        Method method = CityCoreStage.class.getDeclaredMethod(
                "clearBuildingAirEnvelope", GenerationContext.class, BuildingInfo.class);
        method.setAccessible(true);
        method.invoke(new CityCoreStage(), context, center);

        verify(context, never()).setBlock(eq(0), anyInt(), anyInt(), eq(Material.AIR));
        verify(context, never()).setBlock(eq(15), anyInt(), anyInt(), eq(Material.AIR));
        verify(context, never()).setBlock(anyInt(), anyInt(), eq(0), eq(Material.AIR));
        verify(context, never()).setBlock(anyInt(), anyInt(), eq(15), eq(Material.AIR));
    }

    @Test
    @DisplayName("建筑区应执行全区地表嵌合，不仅边缘列")
    void shouldApplySurfaceEmbeddingOnInnerColumnsForBuildingChunks() throws Exception {
        IDimensionInfo provider = mockProviderWithNeighborHeights(70, 58);
        BuildingInfo center = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        center.isCity = true;
        center.hasBuilding = true;
        center.hasStreet = false;
        center.groundLevel = 70;
        center.cityLevel = 0;

        GenerationContext context = mock(GenerationContext.class);
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getMinHeight()).thenReturn(-64);
        when(worldInfo.getMaxHeight()).thenReturn(320);
        when(context.getWorldInfo()).thenReturn(worldInfo);
        when(context.getDimensionInfo()).thenReturn(provider);
        when(context.getBuildingInfo()).thenReturn(center);
        when(context.getChunkX()).thenReturn(0);
        when(context.getChunkZ()).thenReturn(0);
        when(context.resolveMaterial(any(), eq(Material.STONE))).thenReturn(Material.STONE);
        when(context.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);

        Method method = CityCoreStage.class.getDeclaredMethod(
                "prepareCitySurface", GenerationContext.class, BuildingInfo.class);
        method.setAccessible(true);
        method.invoke(new CityCoreStage(), context, center);

        verify(context, atLeastOnce()).setBlock(eq(8), anyInt(), eq(8), eq(Material.STONE));
    }

    private int[][] invokeStreetTargets(BuildingInfo info, int fallbackY) throws Exception {
        GenerationContext context = mock(GenerationContext.class);
        when(context.getBuildingInfo()).thenReturn(info);
        when(context.getDimensionInfo()).thenReturn(info.provider);
        when(context.getChunkX()).thenReturn(info.coord.chunkX());
        when(context.getChunkZ()).thenReturn(info.coord.chunkZ());

        Method method = CityCoreStage.class.getDeclaredMethod(
                "buildStreetSurfaceTargets", GenerationContext.class, BuildingInfo.class, int.class);
        method.setAccessible(true);
        return (int[][]) method.invoke(new CityCoreStage(), context, info, fallbackY);
    }

    private int[][] invokeBuildingTargets(BuildingInfo info, int centerY) throws Exception {
        GenerationContext context = mock(GenerationContext.class);
        when(context.getBuildingInfo()).thenReturn(info);
        when(context.getDimensionInfo()).thenReturn(info.provider);
        when(context.getChunkX()).thenReturn(info.coord.chunkX());
        when(context.getChunkZ()).thenReturn(info.coord.chunkZ());

        Method method = CityCoreStage.class.getDeclaredMethod(
                "buildBuildingSurfaceTargets", GenerationContext.class, BuildingInfo.class, int.class);
        method.setAccessible(true);
        return (int[][]) method.invoke(new CityCoreStage(), context, info, centerY);
    }

    private IDimensionInfo mockProvider() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getSeed()).thenReturn(2026L);

        LostCityProfile profile = new LostCityProfile("test");
        profile.setGroundLevel(70);
        profile.setCityChance(0.0);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2026L);
        when(provider.dimension()).thenReturn("world");
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.getHeightmap(any(ChunkCoord.class))).thenReturn(flatHeightmap(64));
        when(provider.getHeightmap(anyInt(), anyInt())).thenReturn(flatHeightmap(64));
        return provider;
    }

    private IDimensionInfo mockProviderWithNeighborHeights(int centerHeight, int neighborHeight) {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getSeed()).thenReturn(2026L);

        LostCityProfile profile = new LostCityProfile("test");
        profile.setGroundLevel(centerHeight);
        profile.setCityChance(0.0);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2026L);
        when(provider.dimension()).thenReturn("world");
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> {
            ChunkCoord coord = invocation.getArgument(0);
            boolean center = coord.chunkX() == 0 && coord.chunkZ() == 0;
            return flatHeightmap(center ? centerHeight : neighborHeight);
        });
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> {
            int chunkX = invocation.getArgument(0);
            int chunkZ = invocation.getArgument(1);
            boolean center = chunkX == 0 && chunkZ == 0;
            return flatHeightmap(center ? centerHeight : neighborHeight);
        });
        return provider;
    }

    private ChunkHeightmap flatHeightmap(int y) {
        ChunkHeightmap map = new ChunkHeightmap();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                map.setHeight(x, z, y);
            }
        }
        return map;
    }
}
