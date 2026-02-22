package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 方块选择条目
 * 用于调色板中的随机方块选择
 * 
 * @author During
 * @since 1.4.0
 */
public class BlockChoice {
    
    @SerializedName("random")
    private int random;
    
    @SerializedName("block")
    private String block;
    
    /**
     * 默认构造函数（用于Gson反序列化）
     */
    public BlockChoice() {
    }
    
    /**
     * 构造函数
     * 
     * @param random 随机权重（相对于128）
     * @param block 方块ID
     */
    public BlockChoice(int random, String block) {
        this.random = random;
        this.block = block;
    }
    
    /**
     * 获取随机权重
     * 
     * @return 随机权重
     */
    public int getRandom() {
        return random;
    }
    
    /**
     * 设置随机权重
     * 
     * @param random 随机权重
     */
    public void setRandom(int random) {
        this.random = random;
    }
    
    /**
     * 获取方块ID
     * 
     * @return 方块ID
     */
    public String getBlock() {
        return block;
    }
    
    /**
     * 设置方块ID
     * 
     * @param block 方块ID
     */
    public void setBlock(String block) {
        this.block = block;
    }
    
    @Override
    public String toString() {
        return "BlockChoice{" +
                "random=" + random +
                ", block='" + block + '\'' +
                '}';
    }
}
