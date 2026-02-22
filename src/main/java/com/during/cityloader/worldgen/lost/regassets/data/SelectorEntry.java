package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 通用选择器条目
 */
public class SelectorEntry {

    @SerializedName("factor")
    private float factor = 1.0f;

    @SerializedName("value")
    private String value;

    @SerializedName("biomes")
    private BiomeMatcher biomes;

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BiomeMatcher getBiomes() {
        return biomes;
    }

    public void setBiomes(BiomeMatcher biomes) {
        this.biomes = biomes;
    }
}
