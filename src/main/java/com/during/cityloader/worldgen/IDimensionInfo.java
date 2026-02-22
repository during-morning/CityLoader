package com.during.cityloader.worldgen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.Random;

/**
 * 维度信息接口
 * 提供世界生成所需的维度特定信息
 * 
 * @author During
 * @since 1.4.0
 */
public interface IDimensionInfo {
    
    /**
     * 设置世界
     * 
     * @param world 世界对象
     */
    void setWorld(World world);
    
    /**
     * 获取世界种子
     * 
     * @return 世界种子值
     */
    long getSeed();
    
    /**
     * 获取世界对象
     * 
     * @return Bukkit世界对象
     */
    World getWorld();
    
    /**
     * 获取维度类型
     * 
     * @return 维度类型字符串
     */
    String getType();
    
    /**
     * 获取配置文件
     * 
     * @return 城市配置
     */
    LostCityProfile getProfile();
    
    /**
     * 获取外部配置文件
     * 
     * @return 外部区域配置
     */
    LostCityProfile getOutsideProfile();
    
    /**
     * 获取世界样式
     * 
     * @return 世界样式对象
     */
    WorldStyle getWorldStyle();
    
    /**
     * 获取随机数生成器
     * 
     * @return 随机数生成器
     */
    Random getRandom();
    
    /**
     * 获取地形特征
     * 
     * @return 地形特征类型
     */
    LostCityTerrainFeature getFeature();
    
    /**
     * 获取区块高度图
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 区块高度图
     */
    ChunkHeightmap getHeightmap(int chunkX, int chunkZ);
    
    /**
     * 获取区块高度图
     * 
     * @param coord 区块坐标
     * @return 区块高度图
     */
    ChunkHeightmap getHeightmap(ChunkCoord coord);
    
    /**
     * 获取指定位置的生物群系
     * 
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 生物群系
     */
    Biome getBiome(int x, int y, int z);
    
    /**
     * 获取维度标识符
     * 
     * @return 维度标识符字符串，如果不可用则返回null
     */
    String dimension();
}
