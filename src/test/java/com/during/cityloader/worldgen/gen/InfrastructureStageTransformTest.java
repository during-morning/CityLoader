package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InfrastructureStage Transform 渲染测试")
class InfrastructureStageTransformTest {

    private static final Gson GSON = new Gson();

    @BeforeEach
    void setUpBukkit() throws Exception {
        if (Bukkit.getServer() != null) {
            return;
        }
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

    @Test
    @DisplayName("ROTATE_90 应按 mapX/mapZ 坐标放置方块")
    void shouldMapCoordinatesForRotate90() throws Exception {
        InfrastructureStage stage = new InfrastructureStage();
        BuildingPart part = simplePart2x2();
        GenerationContext context = spy(createContext(Map.of(
                'A', "block_a",
                'B', "block_b",
                'C', "block_c",
                'D', "block_d"
        )));

        Method renderPart = InfrastructureStage.class.getDeclaredMethod(
                "renderPart",
                GenerationContext.class,
                BuildingPart.class,
                int.class,
                boolean.class,
                Transform.class,
                com.during.cityloader.worldgen.LostCityProfile.class);
        renderPart.setAccessible(true);
        renderPart.invoke(stage, context, part, 70, false, Transform.ROTATE_90, null);

        verify(context).setBlock(0, 70, 1, "block_a");
        verify(context).setBlock(0, 70, 0, "block_b");
        verify(context).setBlock(1, 70, 1, "block_c");
        verify(context).setBlock(1, 70, 0, "block_d");
    }

    private GenerationContext createContext(Map<Character, String> entries) {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        when(provider.getWorld()).thenReturn(world);

        BuildingInfo info = mock(BuildingInfo.class);
        CompiledPalette palette = mock(CompiledPalette.class);
        when(palette.get(anyChar(), any(Random.class))).thenAnswer(invocation -> {
            char token = invocation.getArgument(0);
            return entries.get(token);
        });
        when(info.getCompiledPalette()).thenReturn(palette);

        WorldInfo worldInfo = mock(WorldInfo.class);
        when(worldInfo.getName()).thenReturn("world");
        when(worldInfo.getEnvironment()).thenReturn(World.Environment.NORMAL);

        LimitedRegion region = mock(LimitedRegion.class);
        when(region.isInRegion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        return new GenerationContext(worldInfo, region, provider, info, new Random(1L), 0, 0);
    }

    private BuildingPart simplePart2x2() {
        BuildingPartRE partRE = GSON.fromJson("""
                {
                  "xsize": 2,
                  "zsize": 2,
                  "ysize": 1,
                  "slices": [
                    ["AB", "CD"]
                  ]
                }
                """, BuildingPartRE.class);
        partRE.setRegistryName(new ResourceLocation("test", "part_2x2"));
        return new BuildingPart(partRE);
    }
}
