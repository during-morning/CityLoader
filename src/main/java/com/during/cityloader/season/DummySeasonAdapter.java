package com.during.cityloader.season;

import com.during.cityloader.config.PluginConfig;
import org.bukkit.World;
import java.util.logging.Logger;

/**
 * 默认季节适配器（Dummy）
 * 当RealisticSeasons插件不可用时使用
 * 使用配置中的默认季节，或者根据游戏时间模拟季节
 * 
 * @author During
 * @since 1.4.0
 */
public class DummySeasonAdapter implements SeasonAdapter {

    private final Logger logger;
    private final PluginConfig config;

    public DummySeasonAdapter(Logger logger, PluginConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public Season getCurrentSeason(World world) {
        // 尝试模拟季节（可选，或直接返回默认）
        // 这里保留之前的逻辑：如果有简单的模拟需求
        // 但简单起见，且为了稳定，通常返回默认配置

        // 如果想要模拟时间变化:
        /*
         * long time = world.getFullTime();
         * long dayTime = time % 24000;
         * int day = (int) (time / 24000);
         * int seasonDay = day % 96; // 假设96天一个循环
         * 
         * if (seasonDay < 24) return Season.SPRING;
         * else if (seasonDay < 48) return Season.SUMMER;
         * else if (seasonDay < 72) return Season.AUTUMN;
         * else return Season.WINTER;
         */

        return config.getDefaultSeason();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
