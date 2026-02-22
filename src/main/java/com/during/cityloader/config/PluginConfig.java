package com.during.cityloader.config;

import com.during.cityloader.season.Season;
import java.util.List;

/**
 * 插件配置数据模型
 * 包含所有可配置的参数
 * 
 * @author During
 * @since 1.4.0
 */
public class PluginConfig {

    // 默认季节
    private final Season defaultSeason;

    // 城市密度 (0.0-1.0)
    private final double cityDensity;

    // 最小建筑高度
    private final int minBuildingHeight;

    // 最大建筑高度
    private final int maxBuildingHeight;

    // 街道宽度
    private final int streetWidth;

    // 是否生成地下结构
    private final boolean generateUnderground;

    // 是否生成街道
    private final boolean generateStreets;

    // 是否与原版地形兼容
    private final boolean vanillaCompatible;

    // 资源包路径列表
    private final List<String> resourcePacks;

    // 是否启用调试
    private final boolean debugEnabled;

    // 是否记录资源加载
    private final boolean logResourceLoading;

    // 是否记录生成详情
    private final boolean logGeneration;

    // 缓存大小
    private final int cacheSize;

    // 是否启用异步加载
    private final boolean asyncLoading;

    // Profile配置
    private final ProfileConfig profileConfig;

    /**
     * 构造函数
     * 
     * @param defaultSeason       默认季节
     * @param cityDensity         城市密度
     * @param minBuildingHeight   最小建筑高度
     * @param maxBuildingHeight   最大建筑高度
     * @param streetWidth         街道宽度
     * @param generateUnderground 是否生成地下结构
     * @param generateStreets     是否生成街道
     * @param vanillaCompatible   是否与原版兼容
     * @param resourcePacks       资源包路径列表
     * @param debugEnabled        是否启用调试
     * @param logResourceLoading  是否记录资源加载
     * @param logGeneration       是否记录生成详情
     * @param cacheSize           缓存大小
     * @param asyncLoading        是否启用异步加载
     * @param profileConfig       Profile配置
     */
    public PluginConfig(Season defaultSeason, double cityDensity, int minBuildingHeight,
            int maxBuildingHeight, int streetWidth, boolean generateUnderground,
            boolean generateStreets, boolean vanillaCompatible,
            List<String> resourcePacks, boolean debugEnabled,
            boolean logResourceLoading, boolean logGeneration,
            int cacheSize, boolean asyncLoading, ProfileConfig profileConfig) {
        this.defaultSeason = defaultSeason;
        this.cityDensity = cityDensity;
        this.minBuildingHeight = minBuildingHeight;
        this.maxBuildingHeight = maxBuildingHeight;
        this.streetWidth = streetWidth;
        this.generateUnderground = generateUnderground;
        this.generateStreets = generateStreets;
        this.vanillaCompatible = vanillaCompatible;
        this.resourcePacks = resourcePacks;
        this.debugEnabled = debugEnabled;
        this.logResourceLoading = logResourceLoading;
        this.logGeneration = logGeneration;
        this.cacheSize = cacheSize;
        this.asyncLoading = asyncLoading;
        this.profileConfig = profileConfig;
    }

    /**
     * 兼容旧代码的构造函数（不包含ProfileConfig）
     */
    public PluginConfig(Season defaultSeason, double cityDensity, int minBuildingHeight,
            int maxBuildingHeight, int streetWidth, boolean generateUnderground,
            boolean generateStreets, boolean vanillaCompatible,
            List<String> resourcePacks, boolean debugEnabled,
            boolean logResourceLoading, boolean logGeneration,
            int cacheSize, boolean asyncLoading) {
        this(defaultSeason, cityDensity, minBuildingHeight, maxBuildingHeight, streetWidth,
                generateUnderground, generateStreets, vanillaCompatible,
                resourcePacks, debugEnabled, logResourceLoading, logGeneration,
                cacheSize, asyncLoading, null);
    }

    /**
     * 验证配置的有效性
     * 
     * @return 如果配置有效返回true，否则返回false
     */
    public boolean validate() {
        // 验证城市密度范围
        if (cityDensity < 0.0 || cityDensity > 1.0) {
            return false;
        }

        // 验证建筑高度
        if (minBuildingHeight < 1 || maxBuildingHeight < minBuildingHeight) {
            return false;
        }

        // 验证街道宽度
        if (streetWidth < 1 || streetWidth > 16) {
            return false;
        }

        // 验证缓存大小
        if (cacheSize < 0) {
            return false;
        }

        // 资源包列表可以为空（空列表会触发从jar内加载内置资源）
        // 只验证不为null即可
        if (resourcePacks == null) {
            return false;
        }

        if (profileConfig == null) {
            return true;
        }

        return true;
    }

    // Getter方法

    public Season getDefaultSeason() {
        return defaultSeason;
    }

    public double getCityDensity() {
        return cityDensity;
    }

    public int getMinBuildingHeight() {
        return minBuildingHeight;
    }

    public int getMaxBuildingHeight() {
        return maxBuildingHeight;
    }

    public int getStreetWidth() {
        return streetWidth;
    }

    public boolean isGenerateUnderground() {
        return generateUnderground;
    }

    public boolean isGenerateStreets() {
        return generateStreets;
    }

    public boolean isVanillaCompatible() {
        return vanillaCompatible;
    }

    public List<String> getResourcePacks() {
        return resourcePacks;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isLogResourceLoading() {
        return logResourceLoading;
    }

    public boolean isLogGeneration() {
        return logGeneration;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public boolean isAsyncLoading() {
        return asyncLoading;
    }

    public ProfileConfig getProfileConfig() {
        return profileConfig;
    }

    /**
     * 获取基础高度（地面高度）
     * 
     * @return 基础高度
     */
    public int getBaseHeight() {
        return 64; // 默认地面高度
    }

    /**
     * 是否启用地下结构生成
     * 
     * @return 是否启用
     */
    public boolean isEnableUnderground() {
        return generateUnderground;
    }
}
