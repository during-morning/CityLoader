package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.City;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.regassets.PredefinedCityRE;
import com.during.cityloader.worldgen.lost.regassets.data.StreetSettings;
import com.google.gson.Gson;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CityCoreStage 预定义街道路件测试")
class CityCoreStagePredefinedStreetTest {

    @AfterEach
    void tearDown() {
        BuildingInfo.resetCache();
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("预定义街道 type=end + connections=w 时应强制 end 且不旋转")
    void shouldUseForcedEndPlacementFromPredefinedStreet() throws Exception {
        IDimensionInfo provider = mockProvider();
        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 0, 0), provider);
        info.isCity = true;
        info.hasBuilding = false;
        info.highwayXLevel = 0;
        info.highwayZLevel = 0;

        PredefinedCityRE city = new Gson().fromJson("""
                {
                  "dimension": "world",
                  "chunkx": 0,
                  "chunkz": 0,
                  "streets": [
                    {
                      "rel_chunk_x": 0,
                      "rel_chunk_z": 0,
                      "connections": "w",
                      "type": "end"
                    }
                  ]
                }
                """, PredefinedCityRE.class);
        city.setRegistryName(new ResourceLocation("test", "predef_end"));
        AssetRegistries.PREDEFINED_CITIES.register(city.getRegistryName(), city);
        City.cleanCache();

        StreetSettings streetSettings = new StreetSettings();
        streetSettings.setParts(Map.of("end", "test:end_piece"));

        Object placement = invokeSelectStreetPartPlacement(info, streetSettings);
        assertNotNull(placement, "应返回 street part 放置结果");
        assertEquals("test:end_piece", invokePlacementString(placement, "partId"));
        assertEquals(Transform.ROTATE_NONE, invokePlacementTransform(placement));
    }

    @Test
    @DisplayName("预定义街道 type=corner + connections=se 时应映射为 bend 并旋转 180")
    void shouldMapCornerTypeToBendAndRotate() throws Exception {
        IDimensionInfo provider = mockProvider();
        BuildingInfo info = BuildingInfo.getBuildingInfo(new ChunkCoord("world", 5, -3), provider);
        info.isCity = true;
        info.hasBuilding = false;
        info.highwayXLevel = 0;
        info.highwayZLevel = 0;

        PredefinedCityRE city = new Gson().fromJson("""
                {
                  "dimension": "world",
                  "chunkx": 5,
                  "chunkz": -3,
                  "streets": [
                    {
                      "rel_chunk_x": 0,
                      "rel_chunk_z": 0,
                      "connections": "se",
                      "type": "corner"
                    }
                  ]
                }
                """, PredefinedCityRE.class);
        city.setRegistryName(new ResourceLocation("test", "predef_corner"));
        AssetRegistries.PREDEFINED_CITIES.register(city.getRegistryName(), city);
        City.cleanCache();

        StreetSettings streetSettings = new StreetSettings();
        streetSettings.setParts(Map.of("bend", "test:bend_piece"));

        Object placement = invokeSelectStreetPartPlacement(info, streetSettings);
        assertNotNull(placement, "应返回 street part 放置结果");
        assertEquals("test:bend_piece", invokePlacementString(placement, "partId"));
        assertEquals(Transform.ROTATE_180, invokePlacementTransform(placement));
    }

    private IDimensionInfo mockProvider() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getSeaLevel()).thenReturn(63);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getSeed()).thenReturn(2026L);

        IDimensionInfo provider = mock(IDimensionInfo.class);
        LostCityProfile profile = new LostCityProfile("test");
        profile.setGroundLevel(70);
        profile.setCityChance(0.0);
        when(provider.getProfile()).thenReturn(profile);
        when(provider.getSeed()).thenReturn(2026L);
        when(provider.getWorld()).thenReturn(world);
        when(provider.dimension()).thenReturn("world");
        when(provider.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.PLAINS);
        when(provider.getHeightmap(any(ChunkCoord.class))).thenReturn(flatHeightmap(64));
        when(provider.getHeightmap(anyInt(), anyInt())).thenReturn(flatHeightmap(64));
        return provider;
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

    private Object invokeSelectStreetPartPlacement(BuildingInfo info, StreetSettings streetSettings) throws Exception {
        Method method = CityCoreStage.class.getDeclaredMethod("selectStreetPartPlacement", BuildingInfo.class, StreetSettings.class);
        method.setAccessible(true);
        return method.invoke(new CityCoreStage(), info, streetSettings);
    }

    private String invokePlacementString(Object placement, String methodName) throws Exception {
        Method accessor = placement.getClass().getDeclaredMethod(methodName);
        accessor.setAccessible(true);
        return (String) accessor.invoke(placement);
    }

    private Transform invokePlacementTransform(Object placement) throws Exception {
        Method accessor = placement.getClass().getDeclaredMethod("transform");
        accessor.setAccessible(true);
        return (Transform) accessor.invoke(placement);
    }
}
