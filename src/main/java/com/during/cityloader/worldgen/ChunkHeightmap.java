package com.during.cityloader.worldgen;

/**
 * 区块高度图
 * 存储区块中每个位置的地形高度
 * 
 * @author During
 * @since 1.4.0
 */
public class ChunkHeightmap {
    
    private final int[][] heights;
    
    /**
     * 构造区块高度图
     */
    public ChunkHeightmap() {
        this.heights = new int[16][16];
    }
    
    /**
     * 获取指定位置的高度
     * 
     * @param x 区块内X坐标（0-15）
     * @param z 区块内Z坐标（0-15）
     * @return 高度值
     */
    public int getHeight(int x, int z) {
        return heights[x][z];
    }
    
    /**
     * 获取区块的平均高度（用于城市高度限制判定）
     *
     * @return 区块中所有位置高度的平均值
     */
    public int getHeight() {
        int sum = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                sum += heights[x][z];
            }
        }
        return sum / 256;
    }

    /**
     * 设置指定位置的高度
     * 
     * @param x 区块内X坐标（0-15）
     * @param z 区块内Z坐标（0-15）
     * @param height 高度值
     */
    public void setHeight(int x, int z, int height) {
        heights[x][z] = height;
    }
}
