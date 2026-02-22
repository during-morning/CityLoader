package com.during.cityloader.config;

import com.during.cityloader.worldgen.LostCityProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProfileConfig 维度启用策略测试")
class ProfileConfigTest {

    @Test
    @DisplayName("selected-profile 应对所有维度生效")
    void selectedProfileShouldEnableAllWorlds() {
        ProfileConfig config = new ProfileConfig(
                "floating",
                List.of(),
                Map.of(
                        "default", new LostCityProfile("default"),
                        "floating", new LostCityProfile("floating")));

        World nether = mockWorld("minecraft:the_nether", World.Environment.NETHER);
        assertTrue(config.isGenerationEnabled(nether));
        assertEquals("floating", config.resolveProfileName(nether));
    }

    @Test
    @DisplayName("dimensions-with-profiles 应启用指定维度并解析对应 profile")
    void dimensionOverridesShouldEnableConfiguredWorld() {
        ProfileConfig config = new ProfileConfig(
                "",
                List.of("minecraft:the_nether=space"),
                Map.of(
                        "default", new LostCityProfile("default"),
                        "space", new LostCityProfile("space")));

        World nether = mockWorld("minecraft:the_nether", World.Environment.NETHER);
        assertTrue(config.isGenerationEnabled(nether));
        assertTrue(config.hasDimensionProfile(nether));
        assertEquals("space", config.resolveProfileName(nether));
    }

    @Test
    @DisplayName("未配置覆盖时保持兼容：仅 NORMAL 启用")
    void fallbackShouldKeepNormalOnlyBehavior() {
        ProfileConfig config = new ProfileConfig(
                "",
                List.of(),
                Map.of("default", new LostCityProfile("default")));

        World overworld = mockWorld("minecraft:overworld", World.Environment.NORMAL);
        World nether = mockWorld("minecraft:the_nether", World.Environment.NETHER);

        assertTrue(config.isGenerationEnabled(overworld));
        assertFalse(config.isGenerationEnabled(nether));
        assertEquals("default", config.resolveProfileName(overworld));
        assertEquals("default", config.resolveProfileName(nether));
    }

    private World mockWorld(String key, World.Environment environment) {
        World world = mock(World.class);
        String[] split = key.split(":", 2);
        when(world.getKey()).thenReturn(new NamespacedKey(split[0], split[1]));
        when(world.getEnvironment()).thenReturn(environment);
        return world;
    }
}
