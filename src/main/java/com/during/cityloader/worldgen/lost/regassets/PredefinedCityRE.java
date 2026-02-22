package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedBuilding;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedStreet;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 预定义城市注册实体
 * 用于从JSON反序列化预定义城市数据
 * 移植自 LostCities PredefinedCityRE
 *
 * @author During
 * @since 1.4.0
 */
public class PredefinedCityRE implements IAsset {

    @SerializedName("dimension")
    private String dimension = "minecraft:overworld";

    @SerializedName("chunkx")
    private int chunkX;

    @SerializedName("chunkz")
    private int chunkZ;

    @SerializedName("radius")
    private int radius = 100;

    @SerializedName("citystyle")
    private String cityStyle;

    @SerializedName("buildings")
    private List<PredefinedBuilding> predefinedBuildings;

    @SerializedName("streets")
    private List<PredefinedStreet> predefinedStreets;

    private transient ResourceLocation registryName;

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public int getChunkX() {
        return chunkX;
    }

    public void setChunkX(int chunkX) {
        this.chunkX = chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public void setChunkZ(int chunkZ) {
        this.chunkZ = chunkZ;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getCityStyle() {
        return cityStyle;
    }

    public void setCityStyle(String cityStyle) {
        this.cityStyle = cityStyle;
    }

    public List<PredefinedBuilding> getPredefinedBuildings() {
        return predefinedBuildings;
    }

    public void setPredefinedBuildings(List<PredefinedBuilding> predefinedBuildings) {
        this.predefinedBuildings = predefinedBuildings;
    }

    public List<PredefinedStreet> getPredefinedStreets() {
        return predefinedStreets;
    }

    public void setPredefinedStreets(List<PredefinedStreet> predefinedStreets) {
        this.predefinedStreets = predefinedStreets;
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
