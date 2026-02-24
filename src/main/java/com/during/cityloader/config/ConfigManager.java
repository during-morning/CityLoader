package com.during.cityloader.config;

import com.during.cityloader.exception.ConfigException;
import com.during.cityloader.season.Season;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置管理器
 * 负责加载、验证和重载插件配置
 * 
 * @author During
 * @since 1.4.0
 */
public class ConfigManager {

    private final Plugin plugin;
    private final Logger logger;
    private final File configFile;
    private PluginConfig config;

    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    /**
     * 加载配置
     * 如果配置文件不存在，则创建默认配置
     * 
     * @return 加载的配置对象
     * @throws ConfigException 如果加载失败
     */
    public PluginConfig loadConfig() throws ConfigException {
        try {
            // 检查配置文件是否存在
            if (!configFile.exists()) {
                logger.info("配置文件不存在，正在创建默认配置...");
                saveDefaultConfig();
            }

            // 加载YAML配置
            FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(configFile);

            // 解析配置
            config = parseConfig(yamlConfig);

            // 验证配置
            if (!config.validate()) {
                logger.warning("配置验证失败，使用默认值");
                config = createDefaultConfig();
            }

            logger.info("配置加载成功");
            return config;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载配置时发生错误", e);
            throw new ConfigException("无法加载配置文件", e);
        }
    }

    /**
     * 重载配置
     * 
     * @return 重载后的配置对象
     * @throws ConfigException 如果重载失败
     */
    public PluginConfig reloadConfig() throws ConfigException {
        logger.info("正在重载配置...");
        return loadConfig();
    }

    /**
     * 获取当前配置
     * 
     * @return 当前配置对象
     */
    public PluginConfig getConfig() {
        return config;
    }

    /**
     * 保存默认配置文件
     */
    private void saveDefaultConfig() throws IOException {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 从资源中复制默认配置
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
                logger.info("默认配置文件已创建");
            } else {
                // 如果资源不存在，创建一个基本配置
                plugin.saveDefaultConfig();
            }
        }
    }

    /**
     * 从YAML配置解析PluginConfig对象
     * 
     * @param yamlConfig YAML配置
     * @return 解析后的PluginConfig对象
     */
    private PluginConfig parseConfig(FileConfiguration yamlConfig) {
        try {
            // 解析默认季节
            String seasonStr = yamlConfig.getString("default-season", "SPRING");
            Season defaultSeason = Season.fromString(seasonStr);

            // 解析城市密度
            double cityDensity = yamlConfig.getDouble("city-density", 0.3);

            // 解析建筑高度
            int minBuildingHeight = yamlConfig.getInt("min-building-height", 3);
            int maxBuildingHeight = yamlConfig.getInt("max-building-height", 20);

            // 解析街道宽度
            int streetWidth = yamlConfig.getInt("street-width", 5);

            // 解析生成选项
            boolean generateUnderground = yamlConfig.getBoolean("generation.generate-underground", true);
            boolean generateStreets = yamlConfig.getBoolean("generation.generate-streets", true);
            boolean vanillaCompatible = yamlConfig.getBoolean("generation.vanilla-compatible", true);

            // 解析资源包路径
            // 如果为空列表，PaperResourceLoader 会自动扫描插件内置 /data 资产
            List<String> resourcePacks = yamlConfig.getStringList("resource-packs");

            // 解析调试选项
            boolean debugEnabled = yamlConfig.getBoolean("debug.enabled", false);
            boolean logResourceLoading = yamlConfig.getBoolean("debug.log-resource-loading", false);
            boolean logGeneration = yamlConfig.getBoolean("debug.log-generation", false);

            // 解析性能选项
            int cacheSize = yamlConfig.getInt("performance.cache-size", 1000);
            boolean asyncLoading = yamlConfig.getBoolean("performance.async-loading", true);

            ProfileConfig profileConfig = parseProfileConfig(yamlConfig);

            return new PluginConfig(
                    defaultSeason, cityDensity, minBuildingHeight, maxBuildingHeight,
                    streetWidth, generateUnderground, generateStreets, vanillaCompatible,
                    resourcePacks, debugEnabled, logResourceLoading, logGeneration,
                    cacheSize, asyncLoading, profileConfig);

        } catch (Exception e) {
            logger.log(Level.WARNING, "解析配置时发生错误，使用默认配置", e);
            return createDefaultConfig();
        }
    }

    /**
     * 创建默认配置对象
     * 
     * @return 默认配置对象
     */
    private PluginConfig createDefaultConfig() {
        // 资源包路径为空列表，将触发从jar内加载内置资源
        List<String> defaultResourcePacks = new ArrayList<>();

        return new PluginConfig(
                Season.SPRING, // 默认季节
                0.3, // 城市密度
                3, // 最小建筑高度
                20, // 最大建筑高度
                5, // 街道宽度
                true, // 生成地下结构
                true, // 生成街道
                true, // 原版兼容
                defaultResourcePacks, // 资源包路径
                false, // 调试模式
                false, // 记录资源加载
                false, // 记录生成详情
                1000, // 缓存大小
                true, // 异步加载
                createDefaultProfileConfig()
        );
    }

    private ProfileConfig parseProfileConfig(FileConfiguration yamlConfig) {
        ConfigurationSection profilesSection = yamlConfig.getConfigurationSection("profiles");
        if (profilesSection == null) {
            return createDefaultProfileConfig();
        }

        String selectedProfile = profilesSection.getString("selected-profile", "");
        List<String> dimensionsWithProfiles = profilesSection.getStringList("dimensions-with-profiles");

        ConfigurationSection definitions = profilesSection.getConfigurationSection("definitions");
        Map<String, com.during.cityloader.worldgen.LostCityProfile> profiles = new HashMap<>();

        com.during.cityloader.worldgen.LostCityProfile defaultProfile = new com.during.cityloader.worldgen.LostCityProfile("default");
        if (definitions != null && definitions.isConfigurationSection("default")) {
            applyProfileSection(defaultProfile, definitions.getConfigurationSection("default"));
        }
        profiles.put("default", defaultProfile);

        if (definitions != null) {
            for (String key : definitions.getKeys(false)) {
                if ("default".equalsIgnoreCase(key)) {
                    continue;
                }
                ConfigurationSection section = definitions.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String inherit = section.getString("inherit", "");
                com.during.cityloader.worldgen.LostCityProfile base = profiles.getOrDefault(inherit, defaultProfile);
                com.during.cityloader.worldgen.LostCityProfile profile = base.copy(key);
                applyProfileSection(profile, section);
                profiles.put(key, profile);
            }
        }

        return new ProfileConfig(selectedProfile, dimensionsWithProfiles, profiles);
    }

    private ProfileConfig createDefaultProfileConfig() {
        Map<String, com.during.cityloader.worldgen.LostCityProfile> profiles = new HashMap<>();
        profiles.put("default", new com.during.cityloader.worldgen.LostCityProfile("default"));
        return new ProfileConfig("", List.of(), profiles);
    }

    private void applyProfileSection(com.during.cityloader.worldgen.LostCityProfile profile,
                                     ConfigurationSection section) {
        if (profile == null || section == null) {
            return;
        }

        if (section.contains("description")) {
            profile.setDescription(section.getString("description", profile.getDescription()));
        }
        if (section.contains("extra-description")) {
            profile.setExtraDescription(section.getString("extra-description", profile.getExtraDescription()));
        }
        if (section.contains("warning")) {
            profile.setWarning(section.getString("warning", profile.getWarning()));
        }
        if (section.contains("world-style")) {
            profile.setWorldStyle(section.getString("world-style", profile.getWorldStyle()));
        }
        if (section.contains("icon")) {
            profile.setIconFile(section.getString("icon", profile.getIconFile()));
        }
        if (section.contains("highways-enabled")) {
            profile.setHighwaysEnabled(section.getBoolean("highways-enabled", profile.isHighwaysEnabled()));
        }
        if (section.contains("railways-enabled")) {
            profile.setRailwaysEnabled(section.getBoolean("railways-enabled", profile.isRailwaysEnabled()));
        }
        if (section.contains("scattered-enabled")) {
            profile.setScatteredEnabled(section.getBoolean("scattered-enabled", profile.isScatteredEnabled()));
        }
        if (section.contains("damage-enabled")) {
            profile.setDamageEnabled(section.getBoolean("damage-enabled", profile.isDamageEnabled()));
        }

        setString(section, "liquid-block", profile::setLiquidBlock);
        setString(section, "base-block", profile::setBaseBlock);

        setFloat(section, "vine-chance", profile::setVineChance);
        setFloat(section, "random-leaf-block-chance", profile::setChanceOfRandomLeafBlocks);
        setInt(section, "random-leaf-block-thickness", profile::setThicknessOfRandomLeafBlocks);
        setBoolean(section, "avoid-foliage", profile::setAvoidFoliage);

        setFloat(section, "scattered-chance-multiplier", profile::setScatteredChanceMultiplier);

        setBoolean(section, "rubble-layer", profile::setRubbleLayer);
        setFloat(section, "rubble-dirt-scale", profile::setRubbleDirtScale);
        setFloat(section, "rubble-leave-scale", profile::setRubbleLeaveScale);

        setFloat(section, "ruin-chance", profile::setRuinChance);
        setFloat(section, "ruin-minlevel-percent", profile::setRuinMinLevelPercent);
        setFloat(section, "ruin-maxlevel-percent", profile::setRuinMaxLevelPercent);

        setInt(section, "ground-level", profile::setGroundLevel);
        setInt(section, "sea-level", profile::setSeaLevel);

        setBoolean(section, "highway-requires-two-cities", profile::setHighwayRequiresTwoCities);
        setInt(section, "highway-level-from-cities", profile::setHighwayLevelFromCitiesMode);
        setFloat(section, "highway-main-perlin-scale", profile::setHighwayMainPerlinScale);
        setFloat(section, "highway-secondary-perlin-scale", profile::setHighwaySecondaryPerlinScale);
        setFloat(section, "highway-perlin-factor", profile::setHighwayPerlinFactor);
        setInt(section, "highway-distance-mask", profile::setHighwayDistanceMask);
        setBoolean(section, "highway-supports", profile::setHighwaySupports);

        setFloat(section, "railway-dungeon-chance", profile::setRailwayDungeonChance);
        setBoolean(section, "railways-can-end", profile::setRailwaysCanEnd);
        setBoolean(section, "railways-enabled-flag", profile::setRailwaysEnabledFlag);
        setBoolean(section, "railway-stations-enabled", profile::setRailwayStationsEnabled);
        setBoolean(section, "railway-surface-stations-enabled", profile::setRailwaySurfaceStationsEnabled);

        setBoolean(section, "explosions-in-cities-only", profile::setExplosionsInCitiesOnly);

        setBoolean(section, "edit-mode", profile::setEditMode);
        setBoolean(section, "generate-nether", profile::setGenerateNether);
        setBoolean(section, "generate-spawners", profile::setGenerateSpawners);
        setBoolean(section, "generate-loot", profile::setGenerateLoot);
        setBoolean(section, "generate-lighting", profile::setGenerateLighting);
        setBoolean(section, "avoid-water", profile::setAvoidWater);

        setFloat(section, "explosion-chance", profile::setExplosionChance);
        setInt(section, "explosion-min-radius", profile::setExplosionMinRadius);
        setInt(section, "explosion-max-radius", profile::setExplosionMaxRadius);
        setInt(section, "explosion-min-height", profile::setExplosionMinHeight);
        setInt(section, "explosion-max-height", profile::setExplosionMaxHeight);

        setFloat(section, "mini-explosion-chance", profile::setMiniExplosionChance);
        setInt(section, "mini-explosion-min-radius", profile::setMiniExplosionMinRadius);
        setInt(section, "mini-explosion-max-radius", profile::setMiniExplosionMaxRadius);
        setInt(section, "mini-explosion-min-height", profile::setMiniExplosionMinHeight);
        setInt(section, "mini-explosion-max-height", profile::setMiniExplosionMaxHeight);

        setDouble(section, "city-chance", profile::setCityChance);
        setInt(section, "city-min-radius", profile::setCityMinRadius);
        setInt(section, "city-max-radius", profile::setCityMaxRadius);
        setDouble(section, "city-perlin-scale", profile::setCityPerlinScale);
        setDouble(section, "city-perlin-inner-scale", profile::setCityPerlinInnerScale);
        setDouble(section, "city-perlin-offset", profile::setCityPerlinOffset);
        setFloat(section, "city-threshold", profile::setCityThreshold);
        setInt(section, "city-spawn-distance1", profile::setCitySpawnDistance1);
        setInt(section, "city-spawn-distance2", profile::setCitySpawnDistance2);
        setDouble(section, "city-spawn-multiplier1", profile::setCitySpawnMultiplier1);
        setDouble(section, "city-spawn-multiplier2", profile::setCitySpawnMultiplier2);
        setFloat(section, "city-style-threshold", profile::setCityStyleThreshold);
        setString(section, "city-style-alternative", profile::setCityStyleAlternative);
        setBoolean(section, "city-avoid-void", profile::setCityAvoidVoid);

        setBoolean(section, "citysphere-32grid", profile::setCitySphere32Grid);
        setFloat(section, "citysphere-factor", profile::setCitySphereFactor);
        setFloat(section, "citysphere-chance", profile::setCitySphereChance);
        setFloat(section, "citysphere-surface-variation", profile::setCitySphereSurfaceVariation);
        setFloat(section, "citysphere-outside-surface-variation", profile::setCitySphereOutsideSurfaceVariation);
        setFloat(section, "citysphere-monorail-chance", profile::setCitySphereMonorailChance);
        setInt(section, "citysphere-clear-above", profile::setCitySphereClearAbove);
        setInt(section, "citysphere-clear-below", profile::setCitySphereClearBelow);
        setBoolean(section, "citysphere-clear-above-until-air", profile::setCitySphereClearAboveUntilAir);
        setBoolean(section, "citysphere-clear-below-until-air", profile::setCitySphereClearBelowUntilAir);
        setInt(section, "citysphere-outside-groundlevel", profile::setCitySphereOutsideGroundLevel);
        setString(section, "citysphere-outside-profile", profile::setCitySphereOutsideProfile);
        setBoolean(section, "citysphere-only-predefined", profile::setCitySphereOnlyPredefined);
        setInt(section, "citysphere-monorail-height-offset", profile::setCitySphereMonorailHeightOffset);

        setInt(section, "city-level0-height", profile::setCityLevel0Height);
        setInt(section, "city-level1-height", profile::setCityLevel1Height);
        setInt(section, "city-level2-height", profile::setCityLevel2Height);
        setInt(section, "city-level3-height", profile::setCityLevel3Height);
        setInt(section, "city-level4-height", profile::setCityLevel4Height);
        setInt(section, "city-level5-height", profile::setCityLevel5Height);
        setInt(section, "city-level6-height", profile::setCityLevel6Height);
        setInt(section, "city-level7-height", profile::setCityLevel7Height);
        setInt(section, "city-min-height", profile::setCityMinHeight);
        setInt(section, "city-max-height", profile::setCityMaxHeight);

        setInt(section, "ocean-correction-border", profile::setOceanCorrectionBorder);
        setInt(section, "terrain-fix-lower-min-offset", profile::setTerrainFixLowerMinOffset);
        setInt(section, "terrain-fix-lower-max-offset", profile::setTerrainFixLowerMaxOffset);
        setInt(section, "terrain-fix-upper-min-offset", profile::setTerrainFixUpperMinOffset);
        setInt(section, "terrain-fix-upper-max-offset", profile::setTerrainFixUpperMaxOffset);

        setFloat(section, "chest-without-loot-chance", profile::setChestWithoutLootChance);
        setFloat(section, "building-without-loot-chance", profile::setBuildingWithoutLootChance);
        setFloat(section, "building-chance", profile::setBuildingChance);
        setInt(section, "building-min-floors", profile::setBuildingMinFloors);
        setInt(section, "building-max-floors", profile::setBuildingMaxFloors);
        setInt(section, "building-min-floors-chance", profile::setBuildingMinFloorsChance);
        setInt(section, "building-max-floors-chance", profile::setBuildingMaxFloorsChance);
        setInt(section, "building-min-cellars", profile::setBuildingMinCellars);
        setInt(section, "building-max-cellars", profile::setBuildingMaxCellars);
        setFloat(section, "building-doorway-chance", profile::setBuildingDoorwayChance);
        setFloat(section, "building-front-chance", profile::setBuildingFrontChance);
        setBoolean(section, "allow-cross-chunk-all-buildings", profile::setAllowCrossChunkAllBuildings);
        setFloat(section, "footprint-weight-1x1", profile::setFootprintWeight1x1);
        setFloat(section, "footprint-weight-2x2", profile::setFootprintWeight2x2);
        setFloat(section, "footprint-weight-3x2", profile::setFootprintWeight3x2);
        setFloat(section, "footprint-weight-4x4", profile::setFootprintWeight4x4);
        setFloat(section, "target-building-coverage", profile::setTargetBuildingCoverage);
        setBoolean(section, "street-connectivity-priority-high", profile::setStreetConnectivityPriorityHigh);
        setBoolean(section, "fallback-to-1x1-on-conflict", profile::setFallbackTo1x1OnConflict);
        setInt(section, "building-air-clearance-top-buffer", profile::setBuildingAirClearanceTopBuffer);
        setBoolean(section, "force-full-building-surface-smoothing", profile::setForceFullBuildingSurfaceSmoothing);
        setBoolean(section, "floor-zero-hole-enforcement", profile::setFloorZeroHoleEnforcement);
        setBoolean(section, "floor-moss-enabled", profile::setFloorMossEnabled);

        setFloat(section, "park-chance", profile::setParkChance);
        setFloat(section, "corridor-chance", profile::setCorridorChance);
        setFloat(section, "bridge-chance", profile::setBridgeChance);
        setFloat(section, "fountain-chance", profile::setFountainChance);
        setBoolean(section, "bridge-supports", profile::setBridgeSupports);
        setBoolean(section, "park-elevation", profile::setParkElevation);
        setBoolean(section, "park-border", profile::setParkBorder);
        setInt(section, "park-street-threshold", profile::setParkStreetThreshold);

        setBoolean(section, "multi-use-corner", profile::setMultiUseCorner);
        setBoolean(section, "use-avg-heightmap", profile::setUseAvgHeightmap);

        setInt(section, "bedrock-layer", profile::setBedrockLayer);

        setFloat(section, "horizon", profile::setHorizon);
        setFloat(section, "fog-red", profile::setFogRed);
        setFloat(section, "fog-green", profile::setFogGreen);
        setFloat(section, "fog-blue", profile::setFogBlue);
        setFloat(section, "fog-density", profile::setFogDensity);

        setString(section, "spawn-biome", profile::setSpawnBiome);
        setString(section, "spawn-city", profile::setSpawnCity);
        setString(section, "spawn-sphere", profile::setSpawnSphere);
        setBoolean(section, "spawn-not-in-building", profile::setSpawnNotInBuilding);
        setBoolean(section, "force-spawn-in-building", profile::setForceSpawnInBuilding);
        if (section.contains("force-spawn-buildings")) {
            profile.setForceSpawnBuildings(section.getStringList("force-spawn-buildings"));
        }
        if (section.contains("force-spawn-parts")) {
            profile.setForceSpawnParts(section.getStringList("force-spawn-parts"));
        }
        setInt(section, "spawn-check-radius", profile::setSpawnCheckRadius);
        setInt(section, "spawn-radius-increase", profile::setSpawnRadiusIncrease);
        setInt(section, "spawn-check-attempts", profile::setSpawnCheckAttempts);

        setString(section, "landscape-type", profile::setLandscapeType);
    }

    private void setString(ConfigurationSection section, String key, java.util.function.Consumer<String> setter) {
        if (section.contains(key)) {
            setter.accept(section.getString(key));
        }
    }

    private void setBoolean(ConfigurationSection section, String key, java.util.function.Consumer<Boolean> setter) {
        if (section.contains(key)) {
            setter.accept(section.getBoolean(key));
        }
    }

    private void setInt(ConfigurationSection section, String key, java.util.function.IntConsumer setter) {
        if (section.contains(key)) {
            setter.accept(section.getInt(key));
        }
    }

    private void setFloat(ConfigurationSection section, String key, java.util.function.Consumer<Float> setter) {
        if (section.contains(key)) {
            setter.accept((float) section.getDouble(key));
        }
    }

    private void setDouble(ConfigurationSection section, String key, java.util.function.DoubleConsumer setter) {
        if (section.contains(key)) {
            setter.accept(section.getDouble(key));
        }
    }
}
