package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 世界风格中的城市风格选择器
 */
public class CityStyleSelector {

    @SerializedName("factor")
    private float factor = 1.0f;

    @SerializedName(value = "citystyle", alternate = { "cityStyle" })
    private String cityStyle;

    @SerializedName("biomes")
    private BiomeMatcher biomes;

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    public String getCityStyle() {
        return cityStyle;
    }

    public void setCityStyle(String cityStyle) {
        this.cityStyle = cityStyle;
    }

    public BiomeMatcher getBiomes() {
        return biomes;
    }

    public void setBiomes(BiomeMatcher biomes) {
        this.biomes = biomes;
    }
}
