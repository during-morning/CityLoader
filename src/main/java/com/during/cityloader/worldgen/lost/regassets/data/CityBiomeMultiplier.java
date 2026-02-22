package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 城市生物群系倍数
 */
public class CityBiomeMultiplier {

    @SerializedName("multiplier")
    private float multiplier = 1.0f;

    @SerializedName("biomes")
    private BiomeMatcher biomes;

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public BiomeMatcher getBiomes() {
        return biomes;
    }

    public void setBiomes(BiomeMatcher biomes) {
        this.biomes = biomes;
    }
}
