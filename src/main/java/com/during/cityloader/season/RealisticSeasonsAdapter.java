package com.during.cityloader.season;

import com.during.cityloader.config.PluginConfig;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * RealisticSeasons适配器
 * 通过反射与RealisticSeasons插件交互
 * 使用懒加载模式确保在RealisticSeasons完全初始化后才获取API
 * 
 * @author During
 * @since 1.4.0
 */
public class RealisticSeasonsAdapter implements SeasonAdapter {

    private final Logger logger;
    private final PluginConfig config;
    private final Plugin plugin;

    // 反射缓存
    private Object seasonsApiInstance;
    private Method getSeasonMethod;
    private boolean reflectionInitialized = false;
    private boolean initializationAttempted = false;

    public RealisticSeasonsAdapter(Logger logger, PluginConfig config, Plugin plugin) {
        this.logger = logger;
        this.config = config;
        this.plugin = plugin;
        // 不在构造函数中初始化，改用懒加载
    }

    /**
     * 懒加载初始化反射
     * 只在首次需要时初始化，确保RealisticSeasons API已准备好
     */
    private synchronized void ensureInitialized() {
        if (initializationAttempted) {
            return;
        }
        initializationAttempted = true;

        try {
            // 尝试获取API实例
            Class<?> apiClass = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI");
            Method getInstanceMethod = apiClass.getMethod("getInstance");
            seasonsApiInstance = getInstanceMethod.invoke(null);

            if (seasonsApiInstance == null) {
                logger.warning("RealisticSeasons API实例为null，API可能尚未完全初始化");
                return;
            }

            // 获取getSeason方法
            getSeasonMethod = apiClass.getMethod("getSeason", World.class);

            reflectionInitialized = true;
            logger.info("成功连接到RealisticSeasons API");
        } catch (ClassNotFoundException e) {
            logger.warning("未找到RealisticSeasons API类: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.warning("RealisticSeasons API方法签名不匹配: " + e.getMessage());
        } catch (Exception e) {
            logger.warning("初始化RealisticSeasons反射失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Override
    public Season getCurrentSeason(World world) {
        // 懒加载初始化
        ensureInitialized();

        if (!reflectionInitialized || seasonsApiInstance == null) {
            return config.getDefaultSeason();
        }

        try {
            Object rsSeason = getSeasonMethod.invoke(seasonsApiInstance, world);

            // 将RS的Season枚举转换为我们的Season枚举
            if (rsSeason != null) {
                String seasonName = rsSeason.toString().toUpperCase();
                return Season.fromString(seasonName);
            }
        } catch (Exception e) {
            // 仅在调试模式下输出详细错误，避免刷屏
            if (config.isDebugEnabled()) {
                logger.warning("获取季节失败: " + e.getMessage());
            }
        }

        return config.getDefaultSeason();
    }

    @Override
    public boolean isAvailable() {
        // 懒加载初始化
        ensureInitialized();
        return reflectionInitialized && seasonsApiInstance != null;
    }

    /**
     * 强制重新尝试初始化
     * 可用于在RealisticSeasons延迟加载后重试连接
     */
    public void retryInitialization() {
        initializationAttempted = false;
        reflectionInitialized = false;
        seasonsApiInstance = null;
        getSeasonMethod = null;
        ensureInitialized();
    }
}
