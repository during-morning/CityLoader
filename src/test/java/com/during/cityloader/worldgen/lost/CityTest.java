package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.PredefinedCity;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.PredefinedCityRE;
import com.during.cityloader.worldgen.lost.regassets.data.WorldSettings;
import com.google.gson.Gson;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("City 核心逻辑测试")
class CityTest {

    private IDimensionInfo provider;
    private LostCityProfile profile;
    private World world;

    @BeforeEach
    void setUp() {
        provider = mock(IDimensionInfo.class);
        profile = new LostCityProfile("test");
        world = mock(World.class);

        when(provider.getProfile()).thenReturn(profile);
        when(provider.getWorld()).thenReturn(world);
        when(provider.getSeed()).thenReturn(12345L);
        when(provider.dimension()).thenReturn("world");
        
        // Default heightmap (flat 70)
        when(provider.getHeightmap(any(ChunkCoord.class))).thenAnswer(i -> {
            ChunkHeightmap h = new ChunkHeightmap();
            for(int x=0; x<16; x++) for(int z=0; z<16; z++) h.setHeight(x, z, 70);
            return h;
        });
    }

    @AfterEach
    void tearDown() {
        City.cleanCache();
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("PredefinedCity 应强制城市因子为 1.0")
    void testPredefinedCityFactor() {
        ChunkCoord coord = new ChunkCoord("world", 10, 10);
        
        PredefinedCityRE re = new Gson().fromJson("""
                {
                  "dimension": "world",
                  "chunkx": 10,
                  "chunkz": 10,
                  "radius": 100,
                  "citystyle": "test:style"
                }
                """, PredefinedCityRE.class);
        re.setRegistryName(new ResourceLocation("test", "predefined"));
        AssetRegistries.PREDEFINED_CITIES.register(re.getRegistryName(), re);

        float factor = City.getCityFactor(coord, provider, profile);
        assertEquals(1.0f, factor, 0.0001f);
    }

    @Test
    @DisplayName("经典模式：高概率下应生成城市")
    void testClassicModeHighChance() {
        profile.setCityChance(1.0); // 100% chance
        profile.setCityMinRadius(50);
        profile.setCityMaxRadius(50);
        
        ChunkCoord coord = new ChunkCoord("world", 0, 0); // (0,0) will be a city center
        
        assertTrue(City.isCityCenter(coord, provider));
        
        float factor = City.getCityFactor(coord, provider, profile);
        assertTrue(factor > 0, "Factor should be positive at city center");
    }

    @Test
    @DisplayName("经典模式：零概率下不应生成城市")
    void testClassicModeZeroChance() {
        profile.setCityChance(0.0);
        
        ChunkCoord coord = new ChunkCoord("world", 0, 0);
        
        assertFalse(City.isCityCenter(coord, provider));
        
        float factor = City.getCityFactor(coord, provider, profile);
        assertEquals(0.0f, factor, 0.0001f);
    }

    @Test
    @DisplayName("城市中心应受世界种子影响")
    void testCityCenterDependsOnSeed() {
        profile.setCityChance(0.5);

        IDimensionInfo otherSeedProvider = mock(IDimensionInfo.class);
        when(otherSeedProvider.getProfile()).thenReturn(profile);
        when(otherSeedProvider.getWorld()).thenReturn(world);
        when(otherSeedProvider.getSeed()).thenReturn(987654321L);
        when(otherSeedProvider.dimension()).thenReturn("world");

        boolean differs = false;
        for (int x = -32; x <= 32 && !differs; x++) {
            for (int z = -32; z <= 32; z++) {
                ChunkCoord coord = new ChunkCoord("world", x, z);
                boolean a = City.isCityCenter(coord, provider);
                boolean b = City.isCityCenter(coord, otherSeedProvider);
                if (a != b) {
                    differs = true;
                    break;
                }
            }
        }

        assertTrue(differs, "Different world seeds should not share identical city centers");
    }

    @Test
    @DisplayName("worldstyle 的 citychance 应覆盖 profile citychance")
    void testWorldStyleCityChanceOverride() {
        profile.setCityChance(0.0);

        WorldStyle worldStyle = mock(WorldStyle.class);
        WorldSettings settings = new WorldSettings();
        settings.setCityChance(1.0f);
        when(worldStyle.getSettings()).thenReturn(settings);
        when(worldStyle.getCityChanceMultiplier(any(), any())).thenReturn(1.0f);
        when(provider.getWorldStyle()).thenReturn(worldStyle);

        ChunkCoord coord = new ChunkCoord("world", 0, 0);
        assertTrue(City.isCityCenter(coord, provider));
    }

    @Test
    @DisplayName("噪声模式：应根据 Perlin 噪声生成城市因子")
    void testNoiseMode() {
        profile.setCityChance(-1.0); // Enable noise mode
        profile.setCityPerlinScale(0.1);
        profile.setCityPerlinOffset(0.5);
        
        ChunkCoord coord = new ChunkCoord("world", 100, 100);
        
        // This relies on NoiseGenerator implementation, but we check consistency
        float factor1 = City.getCityFactor(coord, provider, profile);
        float factor2 = City.getCityFactor(coord, provider, profile);
        
        assertEquals(factor1, factor2, "Noise mode should be deterministic");
        assertTrue(factor1 >= 0 && factor1 <= 1.0, "Factor must be in [0, 1]");
    }

    @Test
    @DisplayName("噪声模式：城市中心判定应由 Perlin 场驱动且非退化")
    void testNoiseModeCityCenterDistribution() {
        profile.setCityChance(-1.0);
        profile.setCityPerlinScale(24.0);
        profile.setCityPerlinInnerScale(1.0);
        profile.setCityPerlinOffset(0.35);
        profile.setCityThreshold(0.2f);

        boolean hasCityCenter = false;
        boolean hasNonCityCenter = false;

        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                ChunkCoord coord = new ChunkCoord("world", x, z);
                boolean cityCenter = City.isCityCenter(coord, provider);
                if (cityCenter) {
                    hasCityCenter = true;
                } else {
                    hasNonCityCenter = true;
                }
                if (hasCityCenter && hasNonCityCenter) {
                    break;
                }
            }
            if (hasCityCenter && hasNonCityCenter) {
                break;
            }
        }

        assertTrue(hasCityCenter, "噪声场中应存在城市中心候选区块");
        assertTrue(hasNonCityCenter, "噪声场中应存在非城市中心区块");

        ChunkCoord probe = new ChunkCoord("world", 18, -27);
        assertEquals(City.isCityCenter(probe, provider), City.isCityCenter(probe, provider),
                "同坐标多次计算应保持确定性");
    }

    @Test
    @DisplayName("CityRarityMap 缓存键应包含噪声参数")
    void testCityRarityMapCacheKeyShouldIncludeNoiseParameters() {
        CityRarityMap coarse = City.getCityRarityMap("world", 12345L, 24.0, 0.35, 1.0);
        CityRarityMap fine = City.getCityRarityMap("world", 12345L, 4.0, 0.35, 1.0);

        assertNotSame(coarse, fine, "不同 scale 不应复用同一 CityRarityMap");
        float coarseFactor = coarse.getCityFactor(32, -16);
        float fineFactor = fine.getCityFactor(32, -16);
        assertNotEquals(coarseFactor, fineFactor, "不同 scale 应产生不同密度场");
    }

    @Test
    @DisplayName("BuildingInfo.resetCache 应同步清理 City 缓存")
    void testBuildingInfoResetShouldCleanCityCaches() {
        CityRarityMap before = City.getCityRarityMap("world", 2468L, 12.0, 0.3, 1.0);
        BuildingInfo.resetCache();
        CityRarityMap after = City.getCityRarityMap("world", 2468L, 12.0, 0.3, 1.0);

        assertNotSame(before, after, "resetCache 后应重建 CityRarityMap");
    }
    
    @Test
    @DisplayName("高度限制：过低或过高应返回 0 因子")
    void testHeightLimits() {
        profile.setCityChance(1.0);
        profile.setCityMinHeight(80);
        
        ChunkCoord coord = new ChunkCoord("world", 0, 0); // Height is 70 (from setUp)
        
        float factor = City.getCityFactor(coord, provider, profile);
        assertEquals(0.0f, factor, 0.0001f, "Should be 0 because height 70 < min 80");
        
        // Test max height
        profile.setCityMinHeight(60);
        profile.setCityMaxHeight(65);
        factor = City.getCityFactor(coord, provider, profile);
        assertEquals(0.0f, factor, 0.0001f, "Should be 0 because height 70 > max 65");
    }
    
    @Test
    @DisplayName("Spawn 距离衰减应影响因子")
    void testSpawnDistanceAttenuation() {
        profile.setCityChance(1.0);
        profile.setCitySpawnDistance1(200);
        profile.setCitySpawnDistance2(400);
        profile.setCitySpawnMultiplier1(0.0f); // 0 at <200
        profile.setCitySpawnMultiplier2(1.0f); // 1 at >400
        
        // Case 1: Close to spawn (0,0) -> 0 dist
        ChunkCoord close = new ChunkCoord("world", 0, 0);
        assertEquals(0.0f, City.getCityFactor(close, provider, profile), 0.0001f);
        
        // Case 2: Far from spawn (100,0) -> 1600 dist
        ChunkCoord far = new ChunkCoord("world", 100, 0);
        float farFactor = City.getCityFactor(far, provider, profile);
        // We expect it to be non-zero (since chance=1.0 and dist > 400)
        assertTrue(farFactor > 0.0f, "Should be high factor far from spawn");
    }
}
