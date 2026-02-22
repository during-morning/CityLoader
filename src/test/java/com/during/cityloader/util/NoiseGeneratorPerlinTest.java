package com.during.cityloader.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NoiseGeneratorPerlin 测试")
class NoiseGeneratorPerlinTest {

    @Test
    @DisplayName("同 seed 与坐标应生成相同噪声值")
    void shouldBeDeterministicForSameSeedAndCoordinate() {
        NoiseGeneratorPerlin a = new NoiseGeneratorPerlin(12345L, 4);
        NoiseGeneratorPerlin b = new NoiseGeneratorPerlin(12345L, 4);

        double va = a.getValue(12.75, -34.25);
        double vb = b.getValue(12.75, -34.25);

        assertTrue(Math.abs(va - vb) < 1.0e-12, "同 seed 同坐标噪声值应一致");
    }

    @Test
    @DisplayName("不同 seed 应生成不同噪声值")
    void shouldDifferForDifferentSeeds() {
        NoiseGeneratorPerlin a = new NoiseGeneratorPerlin(12345L, 4);
        NoiseGeneratorPerlin b = new NoiseGeneratorPerlin(54321L, 4);

        double va = a.getValue(8.5, 9.5);
        double vb = b.getValue(8.5, 9.5);

        assertNotEquals(va, vb, "不同 seed 的噪声值不应完全相同");
    }

    @Test
    @DisplayName("区域采样应可复现且非零场")
    void shouldGenerateStableNonZeroRegion() {
        NoiseGeneratorPerlin a = new NoiseGeneratorPerlin(2026L, 4);
        NoiseGeneratorPerlin b = new NoiseGeneratorPerlin(2026L, 4);

        double[] regionA = a.getRegion(null, 0.0, 0.0, 8, 8, 0.12, 0.12, 0.5);
        double[] regionB = b.getRegion(null, 0.0, 0.0, 8, 8, 0.12, 0.12, 0.5);

        assertArrayEquals(regionA, regionB, 1.0e-12, "同 seed 的区域噪声应完全一致");

        boolean nonZero = false;
        for (double v : regionA) {
            if (Math.abs(v) > 1.0e-9) {
                nonZero = true;
                break;
            }
        }
        assertTrue(nonZero, "区域噪声不应全为 0");
    }
}
