package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GenerationContext 方块状态解析测试")
class GenerationContextParseBlockDataTest {

    private Field serverField;
    private Server previousServer;
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = mock(Server.class);

        when(server.createBlockData(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            BlockData data = mock(BlockData.class);
            when(data.getMaterial()).thenReturn(material);
            return data;
        });

        when(server.createBlockData(anyString())).thenAnswer(invocation -> {
            String definition = invocation.getArgument(0);
            if ("minecraft:oak_stairs[facing=north,half=bottom]".equals(definition)) {
                BlockData data = mock(BlockData.class);
                when(data.getMaterial()).thenReturn(Material.OAK_STAIRS);
                return data;
            }
            throw new IllegalArgumentException("invalid blockdata: " + definition);
        });

        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = (Server) serverField.get(null);
        serverField.set(null, server);
    }

    @AfterEach
    void tearDown() throws Exception {
        serverField.set(null, previousServer);
    }

    @Test
    @DisplayName("应优先解析完整 blockstate 定义")
    void shouldParseFullBlockStateDefinition() {
        GenerationContext context = createContext();

        BlockData data = context.parseBlockData("minecraft:oak_stairs[facing=north,half=bottom]");

        assertNotNull(data);
        assertEquals(Material.OAK_STAIRS, data.getMaterial());
        verify(server, atLeastOnce()).createBlockData(eq("minecraft:oak_stairs[facing=north,half=bottom]"));
    }

    @Test
    @DisplayName("完整定义解析失败时应回退到材料解析")
    void shouldFallbackToMaterialOnlyWhenStateParsingFails() {
        GenerationContext context = createContext();

        BlockData data = context.parseBlockData("minecraft:stone_bricks[invalid=true]");

        assertNotNull(data);
        assertEquals(Material.STONE_BRICKS, data.getMaterial());
        verify(server, atLeastOnce()).createBlockData(eq(Material.STONE_BRICKS));
    }

    @Test
    @DisplayName("未知命名空间方块应回退到可用原版别名")
    void shouldFallbackUnknownNamespaceToVanillaAlias() {
        GenerationContext context = createContext();

        BlockData data = context.parseBlockData("immersive_weathering:exposed_iron_bars[waterlogged=false]");

        assertNotNull(data);
        assertEquals(Material.IRON_BARS, data.getMaterial());
        verify(server, atLeastOnce()).createBlockData(eq(Material.IRON_BARS));
    }

    @Test
    @DisplayName("未知命名空间工作台方块应回退到 crafting_table")
    void shouldFallbackCustomWorkbenchToCraftingTable() {
        GenerationContext context = createContext();

        BlockData data = context.parseBlockData("pomkotsmechs:mechworkbench[facing=west]");

        assertNotNull(data);
        assertEquals(Material.CRAFTING_TABLE, data.getMaterial());
        verify(server, atLeastOnce()).createBlockData(eq(Material.CRAFTING_TABLE));
    }

    private GenerationContext createContext() {
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");

        LimitedRegion region = mock(LimitedRegion.class);
        BuildingInfo info = mock(BuildingInfo.class);

        return new GenerationContext(worldInfo, region, null, info, new Random(2026L), 0, 0);
    }
}
