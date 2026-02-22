package com.during.cityloader.generator;

import com.during.cityloader.config.PluginConfig;
import com.during.cityloader.config.ProfileConfig;
import com.during.cityloader.season.Season;
import com.during.cityloader.season.SeasonAdapter;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.LostCityTerrainFeature;
import com.during.cityloader.worldgen.PaperDimensionInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 城市区块填充器
 * 默认走 worldgen/lost + worldgen/gen 新链路。
 */
public class CityBlockPopulator extends BlockPopulator {

    private final Logger logger;
    private final Supplier<PluginConfig> configSupplier;
    private final SeasonAdapter seasonAdapter;
    private final Predicate<World> worldGenerationPredicate;
    private final LostCityTerrainFeature terrainFeature;
    private final Map<String, IDimensionInfo> dimensionInfoCache = new ConcurrentHashMap<>();

    public CityBlockPopulator(Logger logger,
                              Supplier<PluginConfig> configSupplier,
                              SeasonAdapter seasonAdapter,
                              Predicate<World> worldGenerationPredicate) {
        this.logger = logger;
        this.configSupplier = configSupplier;
        this.seasonAdapter = seasonAdapter;
        this.worldGenerationPredicate = worldGenerationPredicate;
        this.terrainFeature = LostCityTerrainFeature.DEFAULT;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo,
                         @NotNull Random random,
                         int chunkX,
                         int chunkZ,
                         @NotNull LimitedRegion limitedRegion) {
        World world = Bukkit.getWorld(worldInfo.getName());
        if (world == null) {
            logger.warning("无法获取世界对象: " + worldInfo.getName());
            return;
        }
        if (!shouldGenerateInWorld(world)) {
            return;
        }

        try {
            ensureAssetsLoaded(world);

            IDimensionInfo dimensionInfo = dimensionInfoCache.computeIfAbsent(
                    world.getUID().toString(),
                    key -> createDimensionInfo(world));

            Season season = resolveSeason(world);
            terrainFeature.generate(worldInfo, random, chunkX, chunkZ, limitedRegion, dimensionInfo, season);
        } catch (Exception e) {
            logger.severe("区块生成失败 [" + chunkX + ", " + chunkZ + "]: " + e.getMessage());
        }
    }

    public void invalidateWorldCache() {
        dimensionInfoCache.clear();
    }

    public IDimensionInfo getOrCreateDimensionInfo(World world) {
        if (world == null) {
            return null;
        }
        ensureAssetsLoaded(world);
        return dimensionInfoCache.computeIfAbsent(
                world.getUID().toString(),
                key -> createDimensionInfo(world));
    }

    private void ensureAssetsLoaded(World world) {
        if (AssetRegistries.isLoaded()) {
            return;
        }

        synchronized (AssetRegistries.class) {
            if (AssetRegistries.isLoaded()) {
                return;
            }
            AssetRegistries.load(world);
            logger.info("新链路资产加载完成: " + AssetRegistries.getStatistics());
        }
    }

    private IDimensionInfo createDimensionInfo(World world) {
        PluginConfig runtimeConfig = currentConfig();
        ProfileConfig profileConfig = runtimeConfig == null ? null : runtimeConfig.getProfileConfig();

        LostCityProfile profile = profileConfig == null
                ? new LostCityProfile("default")
                : profileConfig.resolveProfile(world);
        LostCityProfile outsideProfile = profileConfig == null
                ? profile
                : profileConfig.resolveOutsideProfile(profile);

        WorldStyle worldStyle = resolveWorldStyle(world, profile);
        return new PaperDimensionInfo(world, profile, outsideProfile, worldStyle);
    }

    private Season resolveSeason(World world) {
        PluginConfig runtimeConfig = currentConfig();
        Season fallback = runtimeConfig == null ? Season.SPRING : runtimeConfig.getDefaultSeason();
        if (seasonAdapter == null || world == null) {
            return fallback;
        }

        try {
            Season season = seasonAdapter.getCurrentSeason(world);
            return season == null ? fallback : season;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private PluginConfig currentConfig() {
        if (configSupplier == null) {
            return null;
        }
        try {
            return configSupplier.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean shouldGenerateInWorld(World world) {
        if (worldGenerationPredicate == null) {
            return world.getEnvironment() == World.Environment.NORMAL;
        }
        try {
            return worldGenerationPredicate.test(world);
        } catch (Exception ignored) {
            return false;
        }
    }

    private WorldStyle resolveWorldStyle(World world, LostCityProfile profile) {
        WorldStyle worldStyle = null;
        if (profile != null) {
            String preferredStyle = profile.getWorldStyle();
            if (preferredStyle != null && !preferredStyle.isBlank()) {
                worldStyle = AssetRegistries.WORLDSTYLES.get(world, normalizeWorldStyleId(preferredStyle));
            }
        }
        if (worldStyle == null) {
            worldStyle = AssetRegistries.WORLDSTYLES.get(world, "lostcities:standard");
        }
        if (worldStyle == null) {
            for (WorldStyle fallback : AssetRegistries.WORLDSTYLES.getIterable()) {
                worldStyle = fallback;
                break;
            }
        }
        if (worldStyle == null) {
            throw new IllegalStateException("未找到任何 worldstyle，无法初始化维度信息");
        }
        return worldStyle;
    }

    private String normalizeWorldStyleId(String worldStyleName) {
        if (worldStyleName == null || worldStyleName.isBlank()) {
            return "lostcities:standard";
        }
        String trimmed = worldStyleName.trim();
        return trimmed.indexOf(':') >= 0 ? trimmed : "lostcities:" + trimmed;
    }
}
