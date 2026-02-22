package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.WorldStyleRE;
import com.during.cityloader.worldgen.lost.regassets.data.BiomeMatcher;
import com.during.cityloader.worldgen.lost.regassets.data.CityBiomeMultiplier;
import com.during.cityloader.worldgen.lost.regassets.data.CityStyleSelector;
import com.during.cityloader.worldgen.lost.regassets.data.MultiSettings;
import com.during.cityloader.worldgen.lost.regassets.data.ScatteredSettings;
import com.during.cityloader.worldgen.lost.regassets.data.WorldPartSettings;
import com.during.cityloader.worldgen.lost.regassets.data.WorldSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 世界样式类
 */
public class WorldStyle implements ILostCityAsset {

    private final ResourceLocation name;
    private final String outsideStyle;
    private final WorldSettings settings;
    private final MultiSettings multiSettings;
    private final WorldPartSettings partSettings;
    private final ScatteredSettings scatteredSettings;
    private final List<CityStyleSelector> cityStyleSelectors;
    private final List<CityBiomeMultiplier> cityBiomeMultipliers;

    public WorldStyle(WorldStyleRE object) {
        this.name = object.getRegistryName();
        this.outsideStyle = object.getOutsideStyle();
        this.settings = object.getSettings();
        this.multiSettings = object.getMultiSettings();
        this.partSettings = object.getParts();
        this.scatteredSettings = object.getScattered();
        this.cityStyleSelectors = object.getCityStyleSelectors() == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(object.getCityStyleSelectors()));
        this.cityBiomeMultipliers = object.getCityBiomeMultipliers() == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(object.getCityBiomeMultipliers()));
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public String getOutsideStyle() {
        return outsideStyle;
    }

    public WorldSettings getSettings() {
        return settings;
    }

    public MultiSettings getMultiSettings() {
        return multiSettings;
    }

    public WorldPartSettings getPartSettings() {
        return partSettings;
    }

    public ScatteredSettings getScatteredSettings() {
        return scatteredSettings;
    }

    public List<CityStyleSelector> getCityStyleSelectors() {
        return cityStyleSelectors;
    }

    public List<CityBiomeMultiplier> getCityBiomeMultipliers() {
        return cityBiomeMultipliers;
    }

    public String pickCityStyle(Random random, String biomeName) {
        List<CityStyleSelector> candidates = new ArrayList<>();
        int total = 0;
        for (CityStyleSelector selector : cityStyleSelectors) {
            if (selector == null || selector.getCityStyle() == null || selector.getCityStyle().isBlank()) {
                continue;
            }
            if (!matches(selector.getBiomes(), biomeName)) {
                continue;
            }
            int weight = Math.max(1, Math.round(selector.getFactor() * 100));
            total += weight;
            candidates.add(selector);
        }

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1 || total <= 0) {
            return candidates.get(0).getCityStyle();
        }

        int roll = random.nextInt(total);
        int current = 0;
        for (CityStyleSelector selector : candidates) {
            current += Math.max(1, Math.round(selector.getFactor() * 100));
            if (roll < current) {
                return selector.getCityStyle();
            }
        }
        return candidates.get(candidates.size() - 1).getCityStyle();
    }

    /**
     * 获取随机城市样式名（适配 City.java 调用签名）
     * 根据区块的生物群系选择匹配的城市样式
     *
     * @param provider 维度信息
     * @param coord    区块坐标
     * @param random   随机数生成器
     * @return 城市样式名，或 null
     */
    public String getRandomCityStyle(com.during.cityloader.worldgen.IDimensionInfo provider,
                                     com.during.cityloader.util.ChunkCoord coord,
                                     Random random) {
        // 获取区块中心的生物群系名称
        String biomeName = null;
        if (provider.getWorld() != null) {
            try {
                org.bukkit.block.Biome biome = provider.getBiome(
                        coord.chunkX() * 16 + 8, 64, coord.chunkZ() * 16 + 8);
                if (biome != null) {
                    biomeName = biome.name().toLowerCase(Locale.ROOT);
                }
            } catch (Exception e) {
                // 世界未加载时忽略
            }
        }
        return pickCityStyle(random, biomeName);
    }

    /**
     * 获取城市概率乘数（适配 City.java 调用签名）
     * 根据区块的生物群系返回城市概率的乘数
     *
     * @param provider 维度信息
     * @param coord    区块坐标
     * @return 城市概率乘数
     */
    public float getCityChanceMultiplier(com.during.cityloader.worldgen.IDimensionInfo provider,
                                         com.during.cityloader.util.ChunkCoord coord) {
        String biomeName = null;
        if (provider.getWorld() != null) {
            try {
                org.bukkit.block.Biome biome = provider.getBiome(
                        coord.chunkX() * 16 + 8, 64, coord.chunkZ() * 16 + 8);
                if (biome != null) {
                    biomeName = biome.name().toLowerCase(Locale.ROOT);
                }
            } catch (Exception e) {
                // 世界未加载时忽略
            }
        }
        return getCityBiomeMultiplier(biomeName);
    }

    public float getCityBiomeMultiplier(String biomeName) {
        float multiplier = 1.0f;
        for (CityBiomeMultiplier entry : cityBiomeMultipliers) {
            if (entry == null || entry.getBiomes() == null) {
                continue;
            }
            if (matches(entry.getBiomes(), biomeName)) {
                multiplier *= entry.getMultiplier();
            }
        }
        return multiplier;
    }

    private boolean matches(BiomeMatcher matcher, String biomeName) {
        if (matcher == null) {
            return true;
        }
        String biome = biomeName == null ? "" : biomeName.toLowerCase(Locale.ROOT);

        if (!matcher.getExcluding().isEmpty()) {
            for (String token : matcher.getExcluding()) {
                if (tokenMatches(token, biome)) {
                    return false;
                }
            }
        }

        if (!matcher.getIfAll().isEmpty()) {
            for (String token : matcher.getIfAll()) {
                if (!tokenMatches(token, biome)) {
                    return false;
                }
            }
        }

        if (!matcher.getIfAny().isEmpty()) {
            for (String token : matcher.getIfAny()) {
                if (tokenMatches(token, biome)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private boolean tokenMatches(String token, String biome) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String normalized = token.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("#")) {
            String tail = normalized.substring(1);
            int colon = tail.indexOf(':');
            String simple = colon >= 0 ? tail.substring(colon + 1) : tail;
            simple = simple.replace("is_", "").replace('_', ' ');
            return biome.contains(simple.replace(" ", "_")) || biome.contains(simple.replace(" ", ""));
        }
        return biome.equals(normalized) || biome.endsWith(normalized);
    }
}
