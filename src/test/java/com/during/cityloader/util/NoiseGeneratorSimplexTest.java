package com.during.cityloader.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simplex 噪声生成器测试")
class NoiseGeneratorSimplexTest {

    @Test
    @DisplayName("输出范围应在 [-1, 1] 之间")
    void testRange() {
        NoiseGeneratorSimplex generator = new NoiseGeneratorSimplex(new Random(12345));
        for (int i = 0; i < 1000; i++) {
            double val = generator.getValue(i * 0.1, i * 0.1);
            assertTrue(val >= -1.0 && val <= 1.0, "Value " + val + " out of range");
        }
    }

    @Test
    @DisplayName("相同种子应产生相同输出")
    void testDeterminism() {
        NoiseGeneratorSimplex gen1 = new NoiseGeneratorSimplex(new Random(12345));
        NoiseGeneratorSimplex gen2 = new NoiseGeneratorSimplex(new Random(12345));
        
        for (int i = 0; i < 100; i++) {
            assertEquals(gen1.getValue(i, i), gen2.getValue(i, i), 0.000001);
        }
    }
}
