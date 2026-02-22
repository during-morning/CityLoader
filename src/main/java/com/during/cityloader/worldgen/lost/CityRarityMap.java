package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.NoiseGeneratorPerlin;

/**
 * 城市稀有度噪声图
 * 使用 Perlin 噪声生成连续的城市密度场，
 * 使城市呈连片分布而非碎片化随机分布
 *
 * 当 CITY_CHANCE < 0 时启用噪声模式
 */
public class CityRarityMap {

    private final NoiseGeneratorPerlin perlinCity;
    private final double scale;
    private final double offset;
    private final double innerScale;

    /**
     * @param seed       世界种子
     * @param scale      噪声缩放（值越大城市间距越大）
     * @param offset     偏移量（值越大城市越稀有）
     * @param innerScale 内部缩放（控制噪声振幅）
     */
    public CityRarityMap(long seed, double scale, double offset, double innerScale) {
        // 4 层八度，与 LostCities 保持一致
        perlinCity = new NoiseGeneratorPerlin(seed, 4);
        this.scale = scale;
        this.offset = offset;
        this.innerScale = innerScale;
    }

    /**
     * 计算指定区块的城市因子
     *
     * @param cx 区块 X 坐标
     * @param cz 区块 Z 坐标
     * @return 城市因子，0 表示非城市，>0 表示城市（值越大城市密度越高）
     */
    public float getCityFactor(int cx, int cz) {
        double factor = perlinCity.getValue(cx / scale, cz / scale) * innerScale - offset;
        if (factor < 0) {
            factor = 0;
        }
        return (float) factor;
    }
}
