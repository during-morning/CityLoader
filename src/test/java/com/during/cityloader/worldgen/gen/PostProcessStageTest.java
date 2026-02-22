package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.loot.LootTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PostProcessStage 后处理对齐测试")
class PostProcessStageTest {

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
        when(server.getLootTable(any())).thenReturn(mock(LootTable.class));

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
    @DisplayName("无地面支撑时应回退放置壁挂火把")
    void shouldPlaceWallTorchWhenGroundSupportMissing() {
        TestFixture fixture = createFixture(0.0f);
        BuildingInfo info = fixture.info();
        info.isCity = true;
        info.hasBuilding = true;
        info.floors = 1;
        info.cellars = 0;
        info.groundLevel = 70;

        int torchY = info.getCityGroundLevel() + 2;
        fixture.context().setBlock(4, torchY, 3, Material.STONE);

        new PostProcessStage().generate(fixture.context());
        fixture.context().flush();

        verify(fixture.region(), atLeastOnce()).setBlockData(
                eq(3),
                eq(torchY),
                eq(3),
                argThat(data -> data != null && data.getMaterial() == Material.WALL_TORCH));
    }

    @Test
    @DisplayName("应在建筑边缘生成连续藤蔓条带")
    void shouldGenerateBoundaryVineStrip() {
        TestFixture fixture = createFixture(1.0f);
        BuildingInfo info = fixture.info();
        info.isCity = true;
        info.hasBuilding = true;
        info.floors = 2;
        info.cellars = 0;
        info.groundLevel = 70;

        BuildingInfo east = info.getXmax();
        east.isCity = true;
        east.hasBuilding = false;
        east.floors = 0;
        east.cellars = 0;
        east.groundLevel = 70;

        int bottom = info.getCityGroundLevel() + 3;
        int topExclusive = info.getMaxHeight();
        for (int y = bottom; y < topExclusive; y++) {
            fixture.context().setBlock(15, y, 0, Material.STONE);
        }

        new PostProcessStage().generate(fixture.context());
        fixture.context().flush();

        verify(fixture.region(), atLeastOnce()).setBlockData(
                eq(14),
                anyInt(),
                eq(0),
                argThat(data -> data != null && data.getMaterial() == Material.VINE));
    }

    @Test
    @DisplayName("postTodo 应在后处理阶段执行且只执行一次")
    void shouldExecuteAndClearPostTodo() {
        TestFixture fixture = createFixture(0.0f);
        BuildingInfo info = fixture.info();
        info.hasBuilding = false;

        AtomicInteger counter = new AtomicInteger();
        info.addPostTodo(counter::incrementAndGet);
        info.addPalettePostTodo(2, 72, 2, "test_part",
                new CompiledPalette.Information("test:loot", "minecraft:zombie", true, Map.of("demo", true)));
        fixture.context().setBlock(2, 71, 2, Material.STONE);

        PostProcessStage stage = new PostProcessStage();
        stage.generate(fixture.context());
        stage.generate(fixture.context());
        fixture.context().flush();

        assertEquals(1, counter.get());
        assertEquals(0, info.getPostTodoCount());
        assertEquals(0, info.getPalettePostTodoCount());
        verify(fixture.region(), atLeastOnce()).setBlockData(
                eq(2),
                eq(72),
                eq(2),
                argThat(data -> data != null && data.getMaterial() == Material.TORCH));
    }

    @Test
    @DisplayName("palette loot/mob/tag 应在 flush 后写回 blockstate")
    void shouldWritePaletteLootMobAndTagAfterFlush() {
        TestFixture fixture = createFixture(0.0f);
        BuildingInfo info = fixture.info();
        info.hasBuilding = false;

        info.addPalettePostTodo(1, 70, 1, "test_part",
                new CompiledPalette.Information("minecraft:chests/simple_dungeon", null, false, Map.of()));
        info.addPalettePostTodo(2, 70, 2, "test_part",
                new CompiledPalette.Information(null, "minecraft:zombie", false, Map.of()));
        info.addPalettePostTodo(3, 70, 3, "test_part",
                new CompiledPalette.Information(null, null, false, Map.of("Lock", "cityloader_lock")));

        fixture.context().setBlock(1, 70, 1, Material.CHEST);
        fixture.context().setBlock(2, 70, 2, Material.SPAWNER);
        fixture.context().setBlock(3, 70, 3, Material.FURNACE);

        Chest chestState = mock(Chest.class);
        CreatureSpawner spawnerState = mock(CreatureSpawner.class);
        Container furnaceState = mock(Container.class);

        when(fixture.region().getBlockState(1, 70, 1)).thenReturn(chestState);
        when(fixture.region().getBlockState(2, 70, 2)).thenReturn(spawnerState);
        when(fixture.region().getBlockState(3, 70, 3)).thenReturn(furnaceState);

        new PostProcessStage().generate(fixture.context());
        fixture.context().flush();

        verify(chestState).setLootTable(any(), anyLong());
        verify(spawnerState).setSpawnedType(eq(EntityType.ZOMBIE));
        verify(furnaceState).setLock(eq("cityloader_lock"));

        verify(fixture.region()).setBlockState(eq(1), eq(70), eq(1), eq(chestState));
        verify(fixture.region()).setBlockState(eq(2), eq(70), eq(2), eq(spawnerState));
        verify(fixture.region()).setBlockState(eq(3), eq(70), eq(3), eq(furnaceState));
    }

    private TestFixture createFixture(float vineChance) {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(2026L);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setCityChance(1.0f);
        profile.setGroundLevel(70);
        profile.setVineChance(vineChance);
        profile.setChanceOfRandomLeafBlocks(0.0f);
        profile.setChestWithoutLootChance(0.0f);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2026L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.dimension()).thenReturn("world");
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(invocation -> flatHeightmap(64));
        when(provider.getHeightmap(anyInt(), anyInt())).thenAnswer(invocation -> flatHeightmap(64));

        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);

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

        return new TestFixture(context, info, region);
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

    private record TestFixture(GenerationContext context, BuildingInfo info, LimitedRegion region) {
    }
}
