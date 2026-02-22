package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GenerationContext 方块状态放置测试")
class GenerationContextBlockStatePlacementTest {

    private Field serverField;
    private Server previousServer;
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = mock(Server.class);

        when(server.createBlockData(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            return blockData(material);
        });

        when(server.createBlockData(anyString())).thenAnswer(invocation -> {
            String definition = invocation.getArgument(0);
            return switch (definition) {
                case "minecraft:powered_rail[shape=east_west,powered=true]" -> blockData(Material.POWERED_RAIL);
                case "minecraft:vine[north=true]" -> blockData(Material.VINE);
                case "minecraft:wall_torch[facing=east]" -> blockData(Material.WALL_TORCH);
                default -> throw new IllegalArgumentException("unsupported blockdata: " + definition);
            };
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
    @DisplayName("setRail 应写入带 shape/powered 的轨道状态")
    void shouldPlaceRailWithShapeAndPoweredState() {
        ContextFixture fixture = createContext();

        fixture.context().setRail(2, 70, 3, Material.POWERED_RAIL, Rail.Shape.EAST_WEST, true);
        fixture.context().flush();

        verify(server).createBlockData(eq("minecraft:powered_rail[shape=east_west,powered=true]"));
        verify(fixture.region()).setBlockData(
                eq(2),
                eq(70),
                eq(3),
                argThat(data -> data != null && data.getMaterial() == Material.POWERED_RAIL));
    }

    @Test
    @DisplayName("setVine 应写入指定方向附着状态")
    void shouldPlaceVineOnSupportFace() {
        ContextFixture fixture = createContext();

        boolean placed = fixture.context().setVine(4, 68, 5, BlockFace.NORTH);
        fixture.context().flush();

        assertTrue(placed);
        verify(server).createBlockData(eq("minecraft:vine[north=true]"));
        verify(fixture.region()).setBlockData(
                eq(4),
                eq(68),
                eq(5),
                argThat(data -> data != null && data.getMaterial() == Material.VINE));
    }

    @Test
    @DisplayName("setWallTorch 应按支撑面计算 torch facing")
    void shouldPlaceWallTorchFacingAwayFromSupport() {
        ContextFixture fixture = createContext();

        boolean placed = fixture.context().setWallTorch(6, 72, 7, BlockFace.WEST);
        fixture.context().flush();

        assertTrue(placed);
        verify(server).createBlockData(eq("minecraft:wall_torch[facing=east]"));
        verify(fixture.region()).setBlockData(
                eq(6),
                eq(72),
                eq(7),
                argThat(data -> data != null && data.getMaterial() == Material.WALL_TORCH));
    }

    private ContextFixture createContext() {
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getMinHeight()).thenReturn(-64);
        when(worldInfo.getMaxHeight()).thenReturn(320);

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
        return new ContextFixture(context, region);
    }

    private BlockData blockData(Material material) {
        BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        return data;
    }

    private record ContextFixture(GenerationContext context, LimitedRegion region) {
    }
}
