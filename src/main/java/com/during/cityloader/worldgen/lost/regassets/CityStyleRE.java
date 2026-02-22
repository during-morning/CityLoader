package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.BuildingSettings;
import com.during.cityloader.worldgen.lost.regassets.data.RailSettings;
import com.during.cityloader.worldgen.lost.regassets.data.Selectors;
import com.during.cityloader.worldgen.lost.regassets.data.StreetSettings;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 城市风格注册实体
 */
public class CityStyleRE implements IAsset {

    @SerializedName("style")
    private String style;

    @SerializedName("inherit")
    private String inherit;

    @SerializedName(value = "buildingchance", alternate = { "buildingChance" })
    private Float buildingChance;

    @SerializedName(value = "explosionchance", alternate = { "explosionChance" })
    private Float explosionChance;

    @SerializedName(value = "stuff_tags", alternate = { "stuffTags" })
    private List<String> stuffTags = new ArrayList<>();

    @SerializedName("selectors")
    private Selectors selectors = new Selectors();

    @SerializedName("buildingsettings")
    private BuildingSettings buildingSettings;

    @SerializedName("streetblocks")
    private StreetSettings streetBlocks;

    @SerializedName("parkblocks")
    private Map<String, String> parkBlocks = new LinkedHashMap<>();

    @SerializedName("corridorblocks")
    private Map<String, String> corridorBlocks = new LinkedHashMap<>();

    @SerializedName("railblocks")
    private RailSettings railBlocks;

    @SerializedName("sphereblocks")
    private Map<String, String> sphereBlocks = new LinkedHashMap<>();

    // 旧兼容字段
    @SerializedName("buildings")
    private List<String> buildings = new ArrayList<>();

    @SerializedName("buildingWeights")
    private List<Float> buildingWeights = new ArrayList<>();

    @SerializedName("multiBuildings")
    private List<String> multiBuildings = new ArrayList<>();

    @SerializedName("multiBuildingWeights")
    private List<Float> multiBuildingWeights = new ArrayList<>();

    private transient ResourceLocation registryName;

    public String getStyle() {
        return style;
    }

    public String getInherit() {
        return inherit;
    }

    public Float getBuildingChance() {
        return buildingChance;
    }

    public Float getExplosionChance() {
        return explosionChance;
    }

    public List<String> getStuffTags() {
        return stuffTags;
    }

    public Selectors getSelectors() {
        return selectors;
    }

    public BuildingSettings getBuildingSettings() {
        return buildingSettings;
    }

    public StreetSettings getStreetBlocks() {
        return streetBlocks;
    }

    public Map<String, String> getParkBlocks() {
        return parkBlocks;
    }

    public Map<String, String> getCorridorBlocks() {
        return corridorBlocks;
    }

    public RailSettings getRailBlocks() {
        return railBlocks;
    }

    public Map<String, String> getSphereBlocks() {
        return sphereBlocks;
    }

    public List<String> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<String> buildings) {
        this.buildings = buildings == null ? new ArrayList<>() : new ArrayList<>(buildings);
    }

    public List<Float> getBuildingWeights() {
        return buildingWeights;
    }

    public void setBuildingWeights(List<Float> buildingWeights) {
        this.buildingWeights = buildingWeights == null ? new ArrayList<>() : new ArrayList<>(buildingWeights);
    }

    public List<String> getMultiBuildings() {
        return multiBuildings;
    }

    public void setMultiBuildings(List<String> multiBuildings) {
        this.multiBuildings = multiBuildings == null ? new ArrayList<>() : new ArrayList<>(multiBuildings);
    }

    public List<Float> getMultiBuildingWeights() {
        return multiBuildingWeights;
    }

    public void setMultiBuildingWeights(List<Float> multiBuildingWeights) {
        this.multiBuildingWeights = multiBuildingWeights == null ? new ArrayList<>() : new ArrayList<>(multiBuildingWeights);
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
