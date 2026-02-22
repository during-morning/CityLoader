package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 散布建筑注册实体
 * 用于从JSON反序列化散布建筑数据
 * 
 * @author During
 * @since 1.4.0
 */
public class ScatteredRE implements IAsset {

    // 兼容旧字段：单建筑
    @SerializedName("building")
    private String building;

    @SerializedName("buildings")
    private List<String> buildings = new ArrayList<>();

    @SerializedName("multibuilding")
    private String multiBuilding;

    @SerializedName(value = "terrainheight", alternate = { "terrainHeight" })
    private String terrainHeight;

    @SerializedName(value = "terrainfix", alternate = { "terrainFix" })
    private String terrainFix;

    @SerializedName(value = "heightoffset", alternate = { "heightOffset" })
    private Integer heightOffset;

    // 兼容旧字段：个体出现概率
    @SerializedName("chance")
    private Float chance;

    private transient ResourceLocation registryName;

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public List<String> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<String> buildings) {
        this.buildings = buildings == null ? new ArrayList<>() : new ArrayList<>(buildings);
    }

    public String getMultiBuilding() {
        return multiBuilding;
    }

    public void setMultiBuilding(String multiBuilding) {
        this.multiBuilding = multiBuilding;
    }

    public String getTerrainHeight() {
        return terrainHeight;
    }

    public void setTerrainHeight(String terrainHeight) {
        this.terrainHeight = terrainHeight;
    }

    public String getTerrainFix() {
        return terrainFix;
    }

    public void setTerrainFix(String terrainFix) {
        this.terrainFix = terrainFix;
    }

    public Integer getHeightOffset() {
        return heightOffset;
    }

    public void setHeightOffset(Integer heightOffset) {
        this.heightOffset = heightOffset;
    }

    public Float getChance() {
        return chance;
    }

    public void setChance(Float chance) {
        this.chance = chance;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public void setRegistryName(ResourceLocation name) {
        this.registryName = name;
    }
}
