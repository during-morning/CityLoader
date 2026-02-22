package com.during.cityloader.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CityLoader日志工具类
 * 提供统一的日志记录接口，所有日志消息使用中文
 * 
 * @author During
 * @since 1.4.0
 */
public class CityLoaderLogger {
    
    private final Logger logger;
    private final boolean debugEnabled;
    
    /**
     * 构造函数
     * 
     * @param logger Java日志记录器
     * @param debugEnabled 是否启用调试日志
     */
    public CityLoaderLogger(Logger logger, boolean debugEnabled) {
        this.logger = logger;
        this.debugEnabled = debugEnabled;
    }
    
    /**
     * 记录资产加载信息
     * 
     * @param assetType 资产类型
     * @param assetId 资产ID
     * @param filePath 文件路径
     */
    public void logAssetLoad(String assetType, String assetId, String filePath) {
        if (debugEnabled) {
            logger.info(String.format("✓ 加载资产: 类型=%s, ID=%s, 文件=%s", 
                    assetType, assetId, filePath));
        }
    }
    
    /**
     * 记录资产加载错误
     * 
     * @param assetType 资产类型
     * @param assetId 资产ID
     * @param filePath 文件路径
     * @param error 错误消息
     */
    public void logAssetError(String assetType, String assetId, String filePath, String error) {
        logger.warning(String.format("✗ 资产加载失败: 类型=%s, ID=%s, 文件=%s, 错误=%s", 
                assetType, assetId, filePath, error));
    }
    
    /**
     * 记录资产加载错误（带异常）
     * 
     * @param assetType 资产类型
     * @param assetId 资产ID
     * @param filePath 文件路径
     * @param error 错误消息
     * @param throwable 异常
     */
    public void logAssetError(String assetType, String assetId, String filePath, String error, Throwable throwable) {
        logger.log(Level.WARNING, 
                String.format("✗ 资产加载失败: 类型=%s, ID=%s, 文件=%s, 错误=%s", 
                        assetType, assetId, filePath, error), 
                throwable);
    }

    /**
     * 记录资产覆盖冲突
     *
     * @param assetType 资产类型
     * @param assetId 资产ID
     * @param overriddenSource 被覆盖来源
     * @param overridingSource 覆盖来源
     */
    public void logAssetConflict(String assetType, String assetId, String overriddenSource, String overridingSource) {
        logger.warning(String.format("⚠ 资产覆盖: 类型=%s, ID=%s, 被覆盖=%s, 覆盖=%s",
                assetType, assetId, overriddenSource, overridingSource));
    }

    /**
     * 记录资产覆盖冲突汇总
     *
     * @param assetType 资产类型
     * @param count 冲突数量
     */
    public void logAssetConflictSummary(String assetType, int count) {
        if (count > 0) {
            logger.warning(String.format("⚠ 资产覆盖汇总: 类型=%s, 冲突数量=%d", assetType, count));
        }
    }
    
    /**
     * 记录区块生成信息
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param buildingId 建筑ID
     */
    public void logChunkGeneration(int chunkX, int chunkZ, String buildingId) {
        if (debugEnabled) {
            logger.info(String.format("→ 生成区块: [%d, %d], 建筑=%s", 
                    chunkX, chunkZ, buildingId));
        }
    }
    
    /**
     * 记录区块生成详细信息
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param buildingId 建筑ID
     * @param floors 楼层数
     * @param cellars 地下室数
     */
    public void logChunkGenerationDetail(int chunkX, int chunkZ, String buildingId, int floors, int cellars) {
        if (debugEnabled) {
            logger.info(String.format("→ 生成区块: [%d, %d], 建筑=%s, 楼层=%d, 地下室=%d", 
                    chunkX, chunkZ, buildingId, floors, cellars));
        }
    }
    
    /**
     * 记录生成错误
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param error 错误消息
     */
    public void logGenerationError(int chunkX, int chunkZ, String error) {
        logger.warning(String.format("✗ 区块生成失败: [%d, %d], 错误=%s", 
                chunkX, chunkZ, error));
    }
    
    /**
     * 记录生成错误（带异常）
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param error 错误消息
     * @param throwable 异常
     */
    public void logGenerationError(int chunkX, int chunkZ, String error, Throwable throwable) {
        logger.log(Level.WARNING, 
                String.format("✗ 区块生成失败: [%d, %d], 错误=%s", chunkX, chunkZ, error), 
                throwable);
    }
    
    /**
     * 记录缓存统计信息
     * 
     * @param cacheName 缓存名称
     * @param size 缓存大小
     * @param hits 命中次数
     * @param misses 未命中次数
     */
    public void logCacheStats(String cacheName, int size, long hits, long misses) {
        if (debugEnabled) {
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
            logger.info(String.format("📊 缓存统计 [%s]: 大小=%d, 命中=%d, 未命中=%d, 命中率=%.2f%%", 
                    cacheName, size, hits, misses, hitRate));
        }
    }
    
    /**
     * 记录调试信息
     * 
     * @param message 消息
     */
    public void debug(String message) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    /**
     * 记录信息
     * 
     * @param message 消息
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * 记录警告
     * 
     * @param message 消息
     */
    public void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * 记录错误
     * 
     * @param message 消息
     */
    public void error(String message) {
        logger.severe(message);
    }
    
    /**
     * 记录错误（带异常）
     * 
     * @param message 消息
     * @param throwable 异常
     */
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
