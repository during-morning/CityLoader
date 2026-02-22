package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CityCoreStage torch todo 对齐测试")
class CityCoreStageTorchTodoTest {

    private Field serverField;
    private Server previousServer;

    @BeforeEach
    void setUp() throws Exception {
        Server server = mock(Server.class);
        when(server.createBlockData(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            BlockData data = mock(BlockData.class);
            when(data.getMaterial()).thenReturn(material);
            return data;
        });
        when(server.createBlockData(anyString())).thenAnswer(invocation -> {
            String definition = invocation.getArgument(0);
            if (definition.startsWith("minecraft:wall_torch[")) {
                BlockData data = mock(BlockData.class);
                when(data.getMaterial()).thenReturn(Material.WALL_TORCH);
                return data;
            }
            if (definition.startsWith("minecraft:vine[")) {
                BlockData data = mock(BlockData.class);
                when(data.getMaterial()).thenReturn(Material.VINE);
                return data;
            }
            throw new IllegalArgumentException("unsupported blockdata: " + definition);
        });

        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = (Server) serverField.get(null);
        serverField.set(null, server);
    }

    @AfterEach
    void tearDown() throws Exception {
        BuildingInfo.resetCache();
        serverField.set(null, previousServer);
    }

    @Test
    @DisplayName("palette.info.torch 应写入 postTodo 并在后处理阶段放置火把")
    void shouldScheduleAndExecuteTorchTodoFromPaletteInfo() throws Exception {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(2026L);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setGroundLevel(70);
        profile.setCityChance(1.0f);
        profile.setVineChance(0.0f);
        profile.setChanceOfRandomLeafBlocks(0.0f);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2026L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.dimension()).thenReturn("world");
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> flatHeightmap(64));
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> flatHeightmap(64));

        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        info.isCity = true;
        info.hasBuilding = true;
        info.groundLevel = 70;
        info.cityLevel = 0;
        info.floors = 1;
        info.cellars = 0;

        BuildingPart part = mock(BuildingPart.class);
        when(part.getDepth()).thenReturn(1);
        when(part.getHeight()).thenReturn(1);
        when(part.getWidth()).thenReturn(1);
        when(part.getSliceLayers()).thenReturn(List.of(List.of("T")));

        info.floorTypes = new BuildingPart[]{part};
        info.floorTypes2 = new BuildingPart[]{null};
        info.floorTransforms = new Transform[]{Transform.ROTATE_NONE};
        info.floorTransforms2 = new Transform[]{Transform.ROTATE_NONE};

        PaletteRE paletteRe = new Gson().fromJson("""
                {
                  "name": "torch_todo_palette",
                  "palette": [
                    {"char": "T", "block": "minecraft:stone", "torch": true}
                  ]
                }
                """, PaletteRE.class);
        paletteRe.setRegistryName(new ResourceLocation("test", "torch_todo_palette"));
        CompiledPalette compiledPalette = new CompiledPalette(new Palette(paletteRe));

        Field compiledPaletteField = BuildingInfo.class.getDeclaredField("compiledPalette");
        compiledPaletteField.setAccessible(true);
        compiledPaletteField.set(info, compiledPalette);

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
                0,
                0);

        new CityCoreStage().generate(context);
        assertTrue(info.getPalettePostTodoCount() > 0, "CityCoreStage 应写入 palette torch todo");

        new PostProcessStage().generate(context);
        assertEquals(0, info.getPalettePostTodoCount(), "PostProcessStage 执行后应清空 palette todo");

        context.flush();

        verify(region, atLeastOnce()).setBlockData(
                eq(0),
                eq(70),
                eq(0),
                argThat(data -> data != null && data.getMaterial() == Material.TORCH));
    }

    private ChunkHeightmap flatHeightmap(int height) {
        ChunkHeightmap map = new ChunkHeightmap();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                map.setHeight(x, z, height);
            }
        }
        return map;
    }
}
