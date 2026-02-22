package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 方块条目
 * 用于变体与调色板中的加权方块定义
 *
 * @author During
 * @since 1.4.0
 */
public class BlockEntry {

    @SerializedName("random")
    private int random = 1;

    @SerializedName("block")
    private String block;

    /**
     * 默认构造函数（用于 Gson 反序列化）
     */
    public BlockEntry() {
    }

    /**
     * 构造函数
     *
     * @param random 权重
     * @param block 方块 ID
     */
    public BlockEntry(int random, String block) {
        this.random = random;
        this.block = block;
    }

    public int random() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }

    public String block() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }
}
