package com.during.cityloader;

import com.during.cityloader.command.CommandHandler;
import com.during.cityloader.config.ConfigManager;
import com.during.cityloader.config.PluginConfig;
import com.during.cityloader.config.ProfileConfig;
import com.during.cityloader.exception.ConfigException;
import com.during.cityloader.generator.CityBlockPopulator;
import com.during.cityloader.listener.ChunkCompletionListener;
import com.during.cityloader.listener.WorldInitListener;
import com.during.cityloader.season.DummySeasonAdapter;
import com.during.cityloader.season.RealisticSeasonsAdapter;
import com.during.cityloader.season.SeasonAdapter;
import com.during.cityloader.util.CityLoaderLogger;
import com.during.cityloader.util.PaperResourceLoader;
import com.during.cityloader.version.VersionManager;
import com.during.cityloader.worldgen.gen.GlobalCompletionQueue;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * CityLoader 主插件类
 */
public class CityLoaderPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SeasonAdapter seasonAdapter;

    private VersionManager versionManager;
    private CityBlockPopulator cityBlockPopulator;
    private WorldInitListener worldInitListener;
    private ChunkCompletionListener chunkCompletionListener;
    private PluginConfig config;

    @Override
    public void onEnable() {
        try {
            getLogger().info("=================================");
            getLogger().info("  CityLoader v2026-02-18 正在启动...");
            getLogger().info("  版本: " + getDescription().getVersion());
            getLogger().info("=================================");

            getLogger().info("[0/5] 正在检查版本兼容性...");
            versionManager = new VersionManager(getLogger(), this);
            if (!versionManager.checkCompatibility()) {
                getLogger().severe("版本不兼容，插件将被禁用");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            versionManager.displayVersionInfo();
            getLogger().info("✓ 版本检查完成");

            getLogger().info("[1/5] 正在加载配置...");
            configManager = new ConfigManager(this);
            config = configManager.loadConfig();
            getLogger().info("✓ 配置加载完成");

            getLogger().info("[2/5] 正在初始化季节系统...");
            if (getServer().getPluginManager().isPluginEnabled("RealisticSeasons")) {
                seasonAdapter = new RealisticSeasonsAdapter(
                        getLogger(),
                        config,
                        getServer().getPluginManager().getPlugin("RealisticSeasons"));
            } else {
                seasonAdapter = new DummySeasonAdapter(getLogger(), config);
            }
            getLogger().info("✓ 季节系统初始化完成");

            getLogger().info("[3/5] 正在初始化新架构资产系统...");
            File dataRoot = new File(getDataFolder(), "data");
            if (!dataRoot.exists() && dataRoot.mkdirs()) {
                getLogger().info("  → 已创建外部数据目录: " + dataRoot.getPath());
            }
            applyResourceRoots(dataRoot, config);
            CityLoaderLogger assetLogger = new CityLoaderLogger(getLogger(),
                    config.isLogResourceLoading() || config.isDebugEnabled());
            AssetRegistries.setLogger(assetLogger);
            AssetRegistries.reset();
            getLogger().info("  → 资产将在首次世界生成时加载（延迟加载优化）");
            getLogger().info("✓ 新架构资产系统初始化完成");

            getLogger().info("[4/5] 正在注册生成链路与命令...");
            getLogger().info("  → 创建CityBlockPopulator...");
            cityBlockPopulator = new CityBlockPopulator(
                    getLogger(),
                    this::getPluginConfig,
                    seasonAdapter,
                    this::shouldEnableCityGeneration);
            getLogger().info("  → 创建WorldInitListener...");

            worldInitListener = new WorldInitListener(getLogger(), cityBlockPopulator, this::shouldEnableCityGeneration);
            chunkCompletionListener = new ChunkCompletionListener(cityBlockPopulator, this::shouldEnableCityGeneration);
            getLogger().info("  → 注册事件...");
            getServer().getPluginManager().registerEvents(worldInitListener, this);
            getServer().getPluginManager().registerEvents(chunkCompletionListener, this);

            getServer().getScheduler().runTaskTimer(this, () -> {
                final int budgetPerWorld = Math.max(8, Integer.getInteger("cityloader.globalCompletionBudget", 64));
                for (World world : Bukkit.getWorlds()) {
                    if (!shouldEnableCityGeneration(world)) {
                        continue;
                    }
                    GlobalCompletionQueue.drain(world, budgetPerWorld);
                }
            }, 1L, 1L);

            getLogger().info("  → 注册命令...");
            CommandHandler commandHandler = new CommandHandler(this, versionManager);
            PluginCommand command = getCommand("cityloader");
            if (command != null) {
                command.setExecutor(commandHandler);
                command.setTabCompleter(commandHandler);
            }

            getLogger().info("✓ 生成链路与命令注册完成");
            getLogger().info("=================================");
            getLogger().info("  CityLoader 启动成功！");
            getLogger().info("=================================");

        } catch (ConfigException e) {
            getLogger().log(Level.SEVERE, "配置加载失败，插件将被禁用", e);
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "插件初始化失败", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public boolean shouldEnableCityGeneration(World world) {
        if (world == null) {
            return false;
        }
        if (config == null) {
            return world.getEnvironment() == World.Environment.NORMAL;
        }
        ProfileConfig profileConfig = config.getProfileConfig();
        if (profileConfig == null) {
            return world.getEnvironment() == World.Environment.NORMAL;
        }
        return profileConfig.isGenerationEnabled(world);
    }

    public void refreshRuntimeConfig(PluginConfig newConfig) {
        if (newConfig != null) {
            this.config = newConfig;
        }
        applyResourceRoots(new File(getDataFolder(), "data"), this.config);
        if (cityBlockPopulator != null) {
            cityBlockPopulator.invalidateWorldCache();
        }
    }

    private void applyResourceRoots(File dataRootDir, PluginConfig runtimeConfig) {
        List<Path> roots = new ArrayList<>();
        if (dataRootDir != null) {
            roots.add(dataRootDir.toPath().toAbsolutePath().normalize());
        }
        if (runtimeConfig != null && runtimeConfig.getResourcePacks() != null) {
            for (String raw : runtimeConfig.getResourcePacks()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                File candidate = new File(raw);
                if (!candidate.isAbsolute()) {
                    candidate = new File(getDataFolder(), raw);
                }
                roots.add(candidate.toPath().toAbsolutePath().normalize());
            }
        }
        PaperResourceLoader.setExternalDataRoots(roots);
        if (!roots.isEmpty()) {
            getLogger().info("  → 外部资产叠加目录数: " + roots.size());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info("  CityLoader 正在关闭...");
        getLogger().info("=================================");

        try {
            AssetRegistries.reset();
            BuildingInfo.resetCache();
            GlobalCompletionQueue.clear();
            getLogger().info("✓ 新架构缓存清理完成");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "清理资源时发生错误", e);
        }

        getLogger().info("=================================");
        getLogger().info("  CityLoader 已关闭");
        getLogger().info("=================================");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SeasonAdapter getSeasonAdapter() {
        return seasonAdapter;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public CityBlockPopulator getCityBlockPopulator() {
        return cityBlockPopulator;
    }
}
