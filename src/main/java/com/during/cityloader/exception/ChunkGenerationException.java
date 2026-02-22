package com.during.cityloader.exception;

/**
 * 区块生成异常
 * 当区块生成过程中发生错误时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class ChunkGenerationException extends RuntimeException {
    
    private final int chunkX;
    private final int chunkZ;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     */
    public ChunkGenerationException(String message, int chunkX, int chunkZ) {
        super(String.format("区块生成失败 [%d, %d]: %s", chunkX, chunkZ, message));
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param message 错误消息
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param cause 原因
     */
    public ChunkGenerationException(String message, int chunkX, int chunkZ, Throwable cause) {
        super(String.format("区块生成失败 [%d, %d]: %s", chunkX, chunkZ, message), cause);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    
    /**
     * 获取区块X坐标
     * 
     * @return 区块X坐标
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * 获取区块Z坐标
     * 
     * @return 区块Z坐标
     */
    public int getChunkZ() {
        return chunkZ;
    }
}
