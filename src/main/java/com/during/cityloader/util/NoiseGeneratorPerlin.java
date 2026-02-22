package com.during.cityloader.util;

import java.util.Random;

/**
 * 多八度 Simplex 噪声叠加器
 * 移植自 LostCities，将 Forge 的 RandomSource 替换为 java.util.Random
 * 
 * 同时兼任 PerlinNoiseGenerator14 的角色，
 * 避免依赖 Minecraft 内部类 SimplexNoise
 */
public class NoiseGeneratorPerlin {

    private final NoiseGeneratorSimplex[] noiseLevels;
    private final int levels;

    /**
     * 构造多八度噪声生成器
     *
     * @param seed     随机数种子
     * @param levelsIn 八度层数（越多细节越丰富）
     */
    public NoiseGeneratorPerlin(Random seed, int levelsIn) {
        this.levels = levelsIn;
        this.noiseLevels = new NoiseGeneratorSimplex[levelsIn];

        for (int i = 0; i < levelsIn; ++i) {
            this.noiseLevels[i] = new NoiseGeneratorSimplex(seed);
        }
    }

    /**
     * 使用 long 种子构造（兼容 LostCities 的 PerlinNoiseGenerator14 构造方式）
     *
     * @param seed     长整型种子
     * @param levelsIn 八度层数
     */
    public NoiseGeneratorPerlin(long seed, int levelsIn) {
        this(new Random(seed), levelsIn);
    }

    /**
     * 计算指定坐标的噪声值
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 多八度叠加后的噪声值
     */
    public double getValue(double x, double y) {
        double d0 = 0.0D;
        double d1 = 1.0D;

        for (int i = 0; i < this.levels; ++i) {
            d0 += this.noiseLevels[i].getValue(x * d1, y * d1) / d1;
            d1 /= 2.0D;
        }

        return d0;
    }

    /**
     * 批量计算区域噪声值
     *
     * @param buffer 结果缓冲区（可为 null，会自动创建）
     * @param x      起始 X
     * @param z      起始 Z
     * @param xWidth X 方向宽度
     * @param zWidth Z 方向宽度
     * @param xScale X 缩放
     * @param zScale Z 缩放
     * @param factor 八度间衰减因子
     * @return 填充后的缓冲区
     */
    public double[] getRegion(double[] buffer, double x, double z,
                              int xWidth, int zWidth,
                              double xScale, double zScale, double factor) {
        if (buffer != null && buffer.length >= xWidth * zWidth) {
            java.util.Arrays.fill(buffer, 0.0D);
        } else {
            buffer = new double[xWidth * zWidth];
        }

        double d1 = 1.0D;
        double d0 = 1.0D;

        for (int j = 0; j < this.levels; ++j) {
            this.noiseLevels[j].add(buffer, x, z, xWidth, zWidth,
                    xScale * d0 * d1, zScale * d0 * d1, 0.55D / d1);
            d0 *= factor;
            d1 *= 0.5D;
        }

        return buffer;
    }
}
