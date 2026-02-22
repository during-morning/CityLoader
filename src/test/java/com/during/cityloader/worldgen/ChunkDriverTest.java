package com.during.cityloader.worldgen;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.generator.LimitedRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ChunkDriver 连接逻辑测试")
class ChunkDriverTest {

    private ChunkDriver driver;
    private LimitedRegion region;
    private World world;

    @BeforeEach
    void setUp() throws Exception {
        Server server = mock(Server.class);
        when(server.createBlockData(any(Material.class))).thenAnswer(i -> {
            Material m = i.getArgument(0);
            BlockData bd = mock(BlockData.class);
            when(bd.getMaterial()).thenReturn(m);
            if (m == Material.COBBLESTONE_WALL) {
                Wall wall = mock(Wall.class, withSettings().extraInterfaces(BlockData.class));
                when(wall.getMaterial()).thenReturn(m);
                when(wall.clone()).thenReturn(wall);
                return wall;
            }
            return bd;
        });
        when(server.createBlockData(anyString())).thenAnswer(invocation -> {
            String s = invocation.getArgument(0);
            if (s.contains("stairs")) {
                Stairs stairs = mock(Stairs.class, withSettings().extraInterfaces(BlockData.class));
                when(stairs.getMaterial()).thenReturn(Material.OAK_STAIRS);

                if (s.contains("facing=north")) when(stairs.getFacing()).thenReturn(BlockFace.NORTH);
                else if (s.contains("facing=east")) when(stairs.getFacing()).thenReturn(BlockFace.EAST);
                else if (s.contains("facing=south")) when(stairs.getFacing()).thenReturn(BlockFace.SOUTH);
                else if (s.contains("facing=west")) when(stairs.getFacing()).thenReturn(BlockFace.WEST);
                else when(stairs.getFacing()).thenReturn(BlockFace.NORTH);

                when(stairs.clone()).thenReturn(stairs);
                when(stairs.getHalf()).thenReturn(Stairs.Half.BOTTOM);
                return stairs;
            }
            return mock(BlockData.class);
        });

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);

        world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        
        region = mock(LimitedRegion.class);
        
        driver = new ChunkDriver();
        driver.setPrimer(world, region, 0, 0);
    }

    @Test
    @DisplayName("墙应自动连接到相邻的墙")
    void testWallConnection() {
        BlockData wallData = Bukkit.createBlockData(Material.COBBLESTONE_WALL);
        
        driver.currentAbsolute(0, 60, 0).block(wallData);
        driver.currentAbsolute(0, 60, 1).block(wallData);
        
        driver.actuallyGenerate();
        
        Wall wallMock = (Wall) wallData;
        verify(wallMock, atLeastOnce()).setHeight(BlockFace.SOUTH, Wall.Height.LOW);
        verify(wallMock, atLeastOnce()).setHeight(BlockFace.NORTH, Wall.Height.LOW);
    }

    @Test
    @DisplayName("楼梯应自动形成外角")
    void testStairOuterConnection() {
        BlockData stairBase = Bukkit.createBlockData("minecraft:oak_stairs[facing=north]");
        BlockData stairFront = Bukkit.createBlockData("minecraft:oak_stairs[facing=west]");
        
        driver.currentAbsolute(0, 60, 0).block(stairBase);
        driver.currentAbsolute(0, 60, -1).block(stairFront);
        
        driver.actuallyGenerate();
        
        verify((Stairs)stairBase).setShape(Stairs.Shape.OUTER_LEFT);
    }

    @Test
    @DisplayName("楼梯应自动形成内角")
    void testStairInnerConnection() {
        BlockData stairBase = Bukkit.createBlockData("minecraft:oak_stairs[facing=north]");
        BlockData stairBack = Bukkit.createBlockData("minecraft:oak_stairs[facing=west]");

        driver.currentAbsolute(0, 60, 0).block(stairBase);
        driver.currentAbsolute(0, 60, 1).block(stairBack);

        driver.actuallyGenerate();

        verify((Stairs) stairBase).setShape(Stairs.Shape.INNER_LEFT);
    }

    @Test
    @DisplayName("SectionCache 索引应按 y-x-z 排布")
    void testSectionCacheIndexOrder() throws Exception {
        BlockData stone = Bukkit.createBlockData(Material.STONE);
        int y = world.getMinHeight() + 3; // py=3, section=0

        driver.current(2, y, 4).block(stone);

        Field cacheField = ChunkDriver.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Object cache = cacheField.get(driver);

        Field sectionsField = cache.getClass().getDeclaredField("sections");
        sectionsField.setAccessible(true);
        Object[] sections = (Object[]) sectionsField.get(cache);
        Object section0 = sections[0];

        Field blocksField = section0.getClass().getDeclaredField("blocks");
        blocksField.setAccessible(true);
        BlockData[] blocks = (BlockData[]) blocksField.get(section0);

        int expected = (3 << 8) + (2 << 4) + 4; // py-x-z
        int old = (2 << 8) + (3 << 4) + 4;      // x-py-z

        assertSame(stone, blocks[expected]);
        assertNull(blocks[old], "旧索引槽位应为空");
    }

    @Test
    @DisplayName("索引修正后生成坐标应保持正确")
    void testGenerateCoordinatesAfterIndexFix() {
        BlockData stone = Bukkit.createBlockData(Material.STONE);
        int y = world.getMinHeight() + 3;

        driver.current(2, y, 4).block(stone);
        driver.actuallyGenerate();

        verify(region, atLeastOnce()).setBlockData(eq(2), eq(y), eq(4), eq(stone));
    }
}
