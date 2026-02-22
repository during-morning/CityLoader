package com.during.cityloader.worldgen.gen;

import com.during.cityloader.season.Season;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DisplayName("GenerationContext 季节上下文测试")
class GenerationContextSeasonTest {

    @Test
    @DisplayName("旧构造器应回退到 SPRING")
    void legacyConstructorShouldFallbackToSpring() {
        GenerationContext context = new GenerationContext(
                mock(WorldInfo.class),
                mock(LimitedRegion.class),
                null,
                mock(BuildingInfo.class),
                new Random(1L),
                0,
                0);

        assertEquals(Season.SPRING, context.getSeason());
    }

    @Test
    @DisplayName("新构造器应保留显式季节")
    void explicitConstructorShouldKeepSeason() {
        GenerationContext context = new GenerationContext(
                mock(WorldInfo.class),
                mock(LimitedRegion.class),
                null,
                mock(BuildingInfo.class),
                new Random(2L),
                0,
                0,
                Season.WINTER);

        assertEquals(Season.WINTER, context.getSeason());
    }
}
