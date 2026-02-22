package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 城市风格中的建筑设置
 */
public class BuildingSettings {

    @SerializedName(value = "minfloors", alternate = { "minFloors" })
    private Integer minFloors;

    @SerializedName(value = "maxfloors", alternate = { "maxFloors" })
    private Integer maxFloors;

    @SerializedName(value = "mincellars", alternate = { "minCellars" })
    private Integer minCellars;

    @SerializedName(value = "maxcellars", alternate = { "maxCellars" })
    private Integer maxCellars;

    @SerializedName(value = "buildingchance", alternate = { "buildingChance" })
    private Float buildingChance;

    public Integer getMinFloors() {
        return minFloors;
    }

    public Integer getMaxFloors() {
        return maxFloors;
    }

    public Integer getMinCellars() {
        return minCellars;
    }

    public Integer getMaxCellars() {
        return maxCellars;
    }

    public Float getBuildingChance() {
        return buildingChance;
    }
}
