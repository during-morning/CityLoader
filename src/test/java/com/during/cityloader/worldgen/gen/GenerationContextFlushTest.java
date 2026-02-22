package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GenerationContext Flush 行为测试")
class GenerationContextFlushTest {

    @Test
    @DisplayName("setBlockData 应仅写入 ChunkDriver 缓冲，flush 后才下发到 LimitedRegion")
    void shouldWriteToRegionOnlyAfterFlush() {
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");

        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        IDimensionInfo dimensionInfo = mock(IDimensionInfo.class);
        when(dimensionInfo.getWorld()).thenReturn(world);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        BuildingInfo info = mock(BuildingInfo.class);
        GenerationContext context = new GenerationContext(
                worldInfo,
                region,
                dimensionInfo,
                info,
                new Random(2026L),
                0,
                0);

        BlockData stone = mock(BlockData.class);
        when(stone.getMaterial()).thenReturn(Material.STONE);
        context.setBlockData(1, 70, 2, stone);

        verify(region, never()).setBlockData(anyInt(), anyInt(), anyInt(), eq(stone));

        context.flush();

        verify(region).setBlockData(eq(1), eq(70), eq(2), eq(stone));
    }
}
