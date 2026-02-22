package com.during.cityloader.util;

/**
 * 区块坐标记录类
 * 用于表示一个区块的坐标（维度 + X + Z）
 * 
 * @param dimension 维度标识符
 * @param chunkX 区块X坐标
 * @param chunkZ 区块Z坐标
 * 
 * @author During
 * @since 1.4.0
 */
public record ChunkCoord(String dimension, int chunkX, int chunkZ) {

    /**
     * 偏移指定距离
     * 
     * @param dx X方向偏移
     * @param dz Z方向偏移
     * @return 新的区块坐标
     */
    public ChunkCoord offset(int dx, int dz) {
        return new ChunkCoord(dimension, chunkX + dx, chunkZ + dz);
    }

    /**
     * 向东偏移一个区块
     * 
     * @return 东侧区块坐标
     */
    public ChunkCoord east() {
        return new ChunkCoord(dimension, chunkX + 1, chunkZ);
    }

    /**
     * 向西偏移一个区块
     * 
     * @return 西侧区块坐标
     */
    public ChunkCoord west() {
        return new ChunkCoord(dimension, chunkX - 1, chunkZ);
    }

    /**
     * 向北偏移一个区块
     * 
     * @return 北侧区块坐标
     */
    public ChunkCoord north() {
        return new ChunkCoord(dimension, chunkX, chunkZ - 1);
    }

    /**
     * 向南偏移一个区块
     * 
     * @return 南侧区块坐标
     */
    public ChunkCoord south() {
        return new ChunkCoord(dimension, chunkX, chunkZ + 1);
    }

    /**
     * 向西北偏移一个区块
     * 
     * @return 西北侧区块坐标
     */
    public ChunkCoord northWest() {
        return new ChunkCoord(dimension, chunkX - 1, chunkZ - 1);
    }

    /**
     * 向东北偏移一个区块
     * 
     * @return 东北侧区块坐标
     */
    public ChunkCoord northEast() {
        return new ChunkCoord(dimension, chunkX + 1, chunkZ - 1);
    }

    /**
     * 向西南偏移一个区块
     * 
     * @return 西南侧区块坐标
     */
    public ChunkCoord southWest() {
        return new ChunkCoord(dimension, chunkX - 1, chunkZ + 1);
    }

    /**
     * 向东南偏移一个区块
     * 
     * @return 东南侧区块坐标
     */
    public ChunkCoord southEast() {
        return new ChunkCoord(dimension, chunkX + 1, chunkZ + 1);
    }

    @Override
    public String toString() {
        return "ChunkCoord{" +
                "dimension=" + dimension +
                ", chunkX=" + chunkX +
                ", chunkZ=" + chunkZ +
                '}';
    }
}
