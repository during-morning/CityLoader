package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 散布系统设置
 */
public class ScatteredSettings {

    @SerializedName("areasize")
    private Integer areaSize;

    @SerializedName("chance")
    private Float chance;

    @SerializedName("weightnone")
    private Integer weightNone;

    @SerializedName("list")
    private List<ScatteredSelector> list = new ArrayList<>();

    public Integer getAreaSize() {
        return areaSize;
    }

    public void setAreaSize(Integer areaSize) {
        this.areaSize = areaSize;
    }

    public Float getChance() {
        return chance;
    }

    public void setChance(Float chance) {
        this.chance = chance;
    }

    public Integer getWeightNone() {
        return weightNone;
    }

    public void setWeightNone(Integer weightNone) {
        this.weightNone = weightNone;
    }

    public List<ScatteredSelector> getList() {
        return list;
    }

    public void setList(List<ScatteredSelector> list) {
        this.list = list == null ? new ArrayList<>() : list;
    }
}
