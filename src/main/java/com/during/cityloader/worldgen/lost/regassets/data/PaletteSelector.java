package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 调色板选择器
 * 用于 style 中 randompalettes 的权重选择条目
 *
 * @author During
 * @since 1.4.0
 */
public class PaletteSelector {

    @SerializedName("factor")
    private float factor = 1.0f;

    @SerializedName("palette")
    private String palette;

    /**
     * 默认构造函数（用于 Gson 反序列化）
     */
    public PaletteSelector() {
    }

    /**
     * 构造函数
     *
     * @param factor 权重
     * @param palette 调色板 ID
     */
    public PaletteSelector(float factor, String palette) {
        this.factor = factor;
        this.palette = palette;
    }

    public float factor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    public String palette() {
        return palette;
    }

    public void setPalette(String palette) {
        this.palette = palette;
    }
}
