package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.IDimensionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DamageArea 爆炸采样测试")
class DamageAreaTest {

    @AfterEach
    void tearDown() {
        DamageArea.resetCache();
    }

    @Test
    @DisplayName("主/小爆炸概率为0时不应生成爆炸")
    void shouldReturnEmptyWhenAllChancesZero() {
        IDimensionInfo provider = mock(IDimensionInfo.class);
        when(provider.getSeed()).thenReturn(12345L);

        DamageArea area = DamageArea.getOrCreate(
                provider,
                new ChunkCoord("world", 0, 0),
                70, 90,
                60, 100,
                0.0f, 0.0f,
                10, 20,
                2, 6);

        assertTrue(area.isEmpty(), "概率为0时应无爆炸");
    }

    @Test
    @DisplayName("主爆炸概率为1时应生成爆炸")
    void shouldGenerateBlastWhenMainChanceOne() {
        IDimensionInfo provider = mock(IDimensionInfo.class);
        when(provider.getSeed()).thenReturn(67890L);

        DamageArea area = DamageArea.getOrCreate(
                provider,
                new ChunkCoord("world", 10, -3),
                75, 90,
                65, 95,
                1.0f, 0.0f,
                3, 5,
                1, 2);

        assertFalse(area.isEmpty(), "主爆炸概率为1时应至少有一组爆炸");
        assertTrue(area.getBlasts().stream().allMatch(blast -> blast.radius() >= 1), "半径应为正值");
    }
}
