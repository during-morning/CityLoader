package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 散布建筑选择条目
 */
public class ScatteredSelector {

    @SerializedName("name")
    private String name;

    @SerializedName("weight")
    private int weight = 1;

    @SerializedName(value = "maxheightdiff", alternate = { "maxHeightDiff" })
    private Integer maxHeightDiff;

    @SerializedName("biomes")
    private BiomeMatcher biomes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Integer getMaxHeightDiff() {
        return maxHeightDiff;
    }

    public void setMaxHeightDiff(Integer maxHeightDiff) {
        this.maxHeightDiff = maxHeightDiff;
    }

    public BiomeMatcher getBiomes() {
        return biomes;
    }

    public void setBiomes(BiomeMatcher biomes) {
        this.biomes = biomes;
    }
}
