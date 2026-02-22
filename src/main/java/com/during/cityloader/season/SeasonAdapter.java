package com.during.cityloader.season;

import org.bukkit.World;

/**
 * 季节适配器接口
 * 定义获取季节的通用接口
 * 
 * @author During
 * @since 1.4.0
 */
public interface SeasonAdapter {

    /**
     * 获取指定世界的当前季节
     * 
     * @param world 世界对象
     * @return 当前季节
     */
    Season getCurrentSeason(World world);

    /**
     * 适配器是否可用
     * 
     * @return 如果可用返回true
     */
    boolean isAvailable();
}
