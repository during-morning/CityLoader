package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.TimedCache;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.PredefinedCity;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedBuilding;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedStreet;

import java.util.*;
import java.util.function.Supplier;

/**
 * 城市判定核心类
 * 移植自 LostCities，适配 Paper API
 *
 * 城市被定义为大球体，建筑在半径 70% 以内。
 * 支持两种城市分布模式：
 * 1. CITY_CHANCE >= 0: 随机城市中心 + 球形衰减
 * 2. CITY_CHANCE < 0:  Perlin 噪声连续密度场（推荐）
 */
public class City {

    /** 浮点数比较阈值 */
    private static final float FLOAT_THRESHOLD = 0.0001f;

    // 预定义城市/建筑/街道映射缓存
    private static Map<ChunkCoord, PredefinedCity> predefinedCityMap = null;
    private static Map<ChunkCoord, PredefinedBuilding> predefinedBuildingMap = null;
    private static Map<ChunkCoord, PredefinedStreet> predefinedStreetMap = null;

    // 城市稀有度噪声图（按维度+seed+噪声参数缓存）
    private static final Map<CityRarityKey, CityRarityMap> CITY_RARITY_MAP = Collections.synchronizedMap(
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CityRarityKey, CityRarityMap> eldest) {
                    return size() > 32;
                }
            });

    // 城市样式缓存
    private static final Supplier<Integer> CACHE_TIMEOUT = () -> 300;
    private static final TimedCache<ChunkCoord, CityStyle> CITY_STYLE_CACHE = new TimedCache<>(CACHE_TIMEOUT);

    /**
     * 清理所有缓存
     */
    public static void cleanCache() {
        predefinedCityMap = null;
        predefinedBuildingMap = null;
        predefinedStreetMap = null;
        CITY_RARITY_MAP.clear();
        CITY_STYLE_CACHE.clear();
    }

    /**
     * 获取城市稀有度噪声图
     */
    public static CityRarityMap getCityRarityMap(String dimension, long seed,
                                                  double scale, double offset, double innerScale) {
        CityRarityKey key = new CityRarityKey(
                normalizeDimension(dimension),
                seed,
                Double.doubleToLongBits(scale),
                Double.doubleToLongBits(offset),
                Double.doubleToLongBits(innerScale));
        return CITY_RARITY_MAP.computeIfAbsent(key,
                k -> new CityRarityMap(seed, scale, offset, innerScale));
    }

    private static String normalizeDimension(String dimension) {
        return dimension == null ? "<unknown>" : dimension;
    }

    /**
     * 获取预定义城市
     */
    public static PredefinedCity getPredefinedCity(ChunkCoord coord) {
        if (predefinedCityMap == null) {
            predefinedCityMap = new HashMap<>();
            for (PredefinedCity city : AssetRegistries.PREDEFINED_CITIES.getIterable()) {
                predefinedCityMap.put(new ChunkCoord(city.getDimension(),
                        city.getChunkX(), city.getChunkZ()), city);
            }
        }
        if (predefinedCityMap.isEmpty()) {
            return null;
        }
        return predefinedCityMap.get(coord);
    }

    /**
     * 获取预定义建筑（在指定坐标作为左上角的）
     */
    public static PredefinedBuilding getPredefinedBuildingAtTopLeft(ChunkCoord coord) {
        calculateBuildingMap();
        return predefinedBuildingMap.get(coord);
    }

    /**
     * 获取预定义街道
     */
    public static PredefinedStreet getPredefinedStreet(ChunkCoord coord) {
        if (predefinedStreetMap == null) {
            predefinedStreetMap = new HashMap<>();
            for (PredefinedCity city : AssetRegistries.PREDEFINED_CITIES.getIterable()) {
                if (city.getPredefinedStreets() != null) {
                    for (PredefinedStreet street : city.getPredefinedStreets()) {
                        predefinedStreetMap.put(new ChunkCoord(city.getDimension(),
                                city.getChunkX() + street.relChunkX(),
                                city.getChunkZ() + street.relChunkZ()), street);
                    }
                }
            }
        }
        if (predefinedStreetMap.isEmpty()) {
            return null;
        }
        return predefinedStreetMap.get(coord);
    }

    private static void calculateBuildingMap() {
        if (predefinedBuildingMap == null) {
            predefinedBuildingMap = new HashMap<>();
            for (PredefinedCity city : AssetRegistries.PREDEFINED_CITIES.getIterable()) {
                if (city.getPredefinedBuildings() != null) {
                    for (PredefinedBuilding building : city.getPredefinedBuildings()) {
                        predefinedBuildingMap.put(new ChunkCoord(city.getDimension(),
                                city.getChunkX() + building.relChunkX(),
                                city.getChunkZ() + building.relChunkZ()), building);
                    }
                }
            }
        }
    }

    /**
     * 判断指定区块是否为城市中心
     * 基于区块坐标和种子，使用随机数决定。
     * 概率由 profile.getCityChance() 决定。
     *
     * @param coord    区块坐标
     * @param provider 维度信息
     * @return 如果是城市中心则返回 true
     */
    public static boolean isCityCenter(ChunkCoord coord, IDimensionInfo provider) {
        PredefinedCity city = getPredefinedCity(coord);
        if (city != null) {
            return true;
        }
        LostCityProfile profile = provider.getProfile();
        double cityChance = getEffectiveCityChance(provider, profile);

        if (cityChance < 0) {
            CityRarityMap rarityMap = getCityRarityMap(
                    provider.dimension(),
                    provider.getSeed(),
                    profile.getCityPerlinScale(),
                    profile.getCityPerlinOffset(),
                    profile.getCityPerlinInnerScale());
            float factor = rarityMap.getCityFactor(coord.chunkX(), coord.chunkZ());
            return factor > profile.getCityThreshold();
        }

        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        Random cityCenterRandom = new Random(provider.getSeed() + chunkZ * 797003437L + chunkX * 295075153L);
        return cityCenterRandom.nextDouble() < cityChance;
    }

    /**
     * 获取城市半径
     *
     * @param coord    区块坐标
     * @param provider 维度信息
     * @return 城市半径（方块单位）
     */
    public static float getCityRadius(ChunkCoord coord, IDimensionInfo provider) {
        PredefinedCity city = getPredefinedCity(coord);
        if (city != null) {
            return city.getRadius();
        }
        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        Random cityRadiusRandom = new Random(provider.getSeed() + chunkZ * 100001653L + chunkX * 295075153L);
        LostCityProfile profile = provider.getProfile();
        int cityRange = profile.getCityMaxRadius() - profile.getCityMinRadius();
        if (cityRange < 1) {
            cityRange = 1;
        }
        return profile.getCityMinRadius() + cityRadiusRandom.nextInt(cityRange);
    }

    /**
     * 获取城市中心的城市样式名
     *
     * @param coord    区块坐标
     * @param provider 维度信息
     * @return 样式名称
     */
    public static String getCityStyleForCityCenter(ChunkCoord coord, IDimensionInfo provider) {
        PredefinedCity city = getPredefinedCity(coord);
        if (city != null && city.getCityStyle() != null) {
            return city.getCityStyle();
        }
        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        Random cityStyleRandom = new Random(provider.getSeed() + chunkZ * 899809363L + chunkX * 256203221L);
        WorldStyle worldStyle = provider.getWorldStyle();
        if (worldStyle != null) {
            return worldStyle.getRandomCityStyle(provider, coord, cityStyleRandom);
        }
        return null;
    }

    /**
     * 获取指定区块的城市样式（考虑所有周围城市的影响）
     *
     * @param coord    区块坐标
     * @param provider 维度信息
     * @param profile  配置档案
     * @return 城市样式对象
     */
    public static CityStyle getCityStyle(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        return CITY_STYLE_CACHE.computeIfAbsent(coord, k -> getCityStyleInt(coord, provider, profile));
    }

    private static CityStyle getCityStyleInt(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        Random cityStyleRandom = new Random(provider.getSeed() + chunkZ * 593441843L + chunkX * 217645177L);

        // 噪声模式
        double cityChance = getEffectiveCityChance(provider, profile);
        if (cityChance < 0) {
            CityRarityMap rarityMap = getCityRarityMap(
                    provider.dimension(), provider.getSeed(),
                    profile.getCityPerlinScale(), profile.getCityPerlinOffset(), profile.getCityPerlinInnerScale());
            float factor = rarityMap.getCityFactor(chunkX, chunkZ);
            String styleName;
            if (factor < profile.getCityStyleThreshold()) {
                styleName = profile.getCityStyleAlternative();
            } else {
                styleName = getCityStyleForCityCenter(coord, provider);
            }
            if (styleName != null) {
                return AssetRegistries.CITYSTYLES.get(provider.getWorld(), styleName);
            }
        }

        // 经典球形城市模式：搜索周围的城市中心
        List<float[]> styles = new ArrayList<>();
        int offset = (profile.getCityMaxRadius() + 15) / 16;
        for (int cx = chunkX - offset; cx <= chunkX + offset; cx++) {
            for (int cz = chunkZ - offset; cz <= chunkZ + offset; cz++) {
                ChunkCoord c = new ChunkCoord(coord.dimension(), cx, cz);
                if (isCityCenter(c, provider)) {
                    float radius = getCityRadius(c, provider);
                    float sqdist = (cx * 16 - (chunkX << 4)) * (cx * 16 - (chunkX << 4))
                            + (cz * 16 - (chunkZ << 4)) * (cz * 16 - (chunkZ << 4));
                    if (sqdist < radius * radius) {
                        float dist = (float) Math.sqrt(sqdist);
                        float factor = (radius - dist) / radius;
                        styles.add(new float[]{factor});
                    }
                }
            }
        }

        // 选择样式
        String cityStyleName;
        if (styles.isEmpty()) {
            WorldStyle worldStyle = provider.getWorldStyle();
            if (worldStyle != null) {
                cityStyleName = worldStyle.getRandomCityStyle(provider, coord, cityStyleRandom);
            } else {
                cityStyleName = null;
            }
        } else {
            cityStyleName = getCityStyleForCityCenter(coord, provider);
        }
        if (cityStyleName != null) {
            return AssetRegistries.CITYSTYLES.get(provider.getWorld(), cityStyleName);
        }
        return null;
    }

    /**
     * 计算城市因子 —— 核心方法
     * 返回 0 表示非城市区域，>0 表示城市区域（值越大越接近城市中心）
     *
     * @param coord    区块坐标
     * @param provider 维度信息
     * @param profile  配置档案
     * @return 城市因子 [0, 1]
     */
    public static float getCityFactor(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        // 1. 预定义建筑/街道强制城市因子为 1.0
        PredefinedBuilding predefinedBuilding = getPredefinedBuildingAtTopLeft(coord);
        if (predefinedBuilding != null) {
            return 1.0f;
        }
        PredefinedStreet predefinedStreet = getPredefinedStreet(coord);
        if (predefinedStreet != null) {
            return 1.0f;
        }

        // 检查西/西北/北方向的多区块预定义建筑
        predefinedBuilding = getPredefinedBuildingAtTopLeft(coord.west());
        if (predefinedBuilding != null && predefinedBuilding.multi()) {
            return 1.0f;
        }
        predefinedBuilding = getPredefinedBuildingAtTopLeft(coord.northWest());
        if (predefinedBuilding != null && predefinedBuilding.multi()) {
            return 1.0f;
        }
        predefinedBuilding = getPredefinedBuildingAtTopLeft(coord.north());
        if (predefinedBuilding != null && predefinedBuilding.multi()) {
            return 1.0f;
        }

        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        float factor = 0;

        // 2. 计算城市因子
        double cityChance = getEffectiveCityChance(provider, profile);
        if (cityChance < 0) {
            // 噪声模式：使用 Perlin 噪声产生连续的城市密度场
            CityRarityMap rarityMap = getCityRarityMap(
                    provider.dimension(), provider.getSeed(),
                    profile.getCityPerlinScale(), profile.getCityPerlinOffset(), profile.getCityPerlinInnerScale());
            factor = rarityMap.getCityFactor(chunkX, chunkZ);
        } else {
            // 经典模式：搜索周围的城市中心，累加球形衰减因子
            int offset = (profile.getCityMaxRadius() + 15) / 16;
            for (int cx = chunkX - offset; cx <= chunkX + offset; cx++) {
                for (int cz = chunkZ - offset; cz <= chunkZ + offset; cz++) {
                    ChunkCoord c = new ChunkCoord(coord.dimension(), cx, cz);
                    if (isCityCenter(c, provider)) {
                        float radius = getCityRadius(c, provider);
                        float sqdist = (cx * 16 - (chunkX << 4)) * (cx * 16 - (chunkX << 4))
                                + (cz * 16 - (chunkZ << 4)) * (cz * 16 - (chunkZ << 4));
                        if (sqdist < radius * radius) {
                            float dist = (float) Math.sqrt(sqdist);
                            factor += (radius - dist) / radius;
                        }
                    }
                }
            }
        }

        // 3. 地形高度限制
        if (factor > 0.0001 && provider.getWorld() != null) {
            ChunkHeightmap heightmap = provider.getHeightmap(coord);
            if (heightmap == null) {
                return 0;
            }
            if (heightmap.getHeight() < profile.getCityMinHeight()) {
                return 0;
            }
            if (heightmap.getHeight() > profile.getCityMaxHeight()) {
                return 0;
            }
        }

        // 4. 生物群系乘数
        if (factor > 0.0001 && provider.getWorldStyle() != null) {
            WorldStyle worldStyle = provider.getWorldStyle();
            float multiplier = worldStyle.getCityChanceMultiplier(provider, coord);
            factor *= multiplier;
        }

        // 5. 出生点距离衰减
        if (profile.getCitySpawnDistance2() > 0) {
            float dist = (float) Math.sqrt((chunkX << 4) * (chunkX << 4) + (chunkZ << 4) * (chunkZ << 4));
            double factorDist;
            if (dist <= profile.getCitySpawnDistance1()) {
                factorDist = profile.getCitySpawnMultiplier1();
            } else if (dist >= profile.getCitySpawnDistance2()) {
                factorDist = profile.getCitySpawnMultiplier2();
            } else {
                float f = (dist - profile.getCitySpawnDistance1())
                        / (profile.getCitySpawnDistance2() - profile.getCitySpawnDistance1());
                factorDist = profile.getCitySpawnMultiplier1()
                        + f * (profile.getCitySpawnMultiplier2() - profile.getCitySpawnMultiplier1());
            }
            factor *= (float) factorDist;
        }

        return Math.min(Math.max(factor, 0), 1);
    }

    private static double getEffectiveCityChance(IDimensionInfo provider, LostCityProfile profile) {
        if (profile == null) {
            return 0.0;
        }
        if (provider != null && provider.getWorldStyle() != null
                && provider.getWorldStyle().getSettings() != null
                && provider.getWorldStyle().getSettings().getCityChance() != null) {
            return provider.getWorldStyle().getSettings().getCityChance();
        }
        return profile.getCityChance();
    }

    private record CityRarityKey(String dimension,
                                 long seed,
                                 long scaleBits,
                                 long offsetBits,
                                 long innerScaleBits) {
    }
}
