package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.CityBiomeMultiplier;
import com.during.cityloader.worldgen.lost.regassets.data.CityStyleSelector;
import com.during.cityloader.worldgen.lost.regassets.data.MultiSettings;
import com.during.cityloader.worldgen.lost.regassets.data.ScatteredSettings;
import com.during.cityloader.worldgen.lost.regassets.data.WorldPartSettings;
import com.during.cityloader.worldgen.lost.regassets.data.WorldSettings;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界风格注册实体
 */
public class WorldStyleRE implements IAsset {

    @SerializedName(value = "outsidestyle", alternate = { "outsideStyle" })
    private String outsideStyle;

    @SerializedName("citystyles")
    private List<CityStyleSelector> cityStyleSelectors = new ArrayList<>();

    @SerializedName("citybiomemultipliers")
    private List<CityBiomeMultiplier> cityBiomeMultipliers = new ArrayList<>();

    @SerializedName("multisettings")
    private MultiSettings multiSettings;

    @SerializedName("settings")
    private WorldSettings settings;

    @SerializedName("parts")
    private WorldPartSettings parts;

    @SerializedName("scattered")
    private ScatteredSettings scattered;

    @SerializedName(value = "cityspheres", alternate = { "citySpheres" })
    private JsonElement citySpheres;

    private transient ResourceLocation registryName;

    public String getOutsideStyle() {
        return outsideStyle;
    }

    public void setOutsideStyle(String outsideStyle) {
        this.outsideStyle = outsideStyle;
    }

    public List<CityStyleSelector> getCityStyleSelectors() {
        return cityStyleSelectors;
    }

    public void setCityStyleSelectors(List<CityStyleSelector> cityStyleSelectors) {
        this.cityStyleSelectors = cityStyleSelectors == null ? new ArrayList<>() : new ArrayList<>(cityStyleSelectors);
    }

    public List<CityBiomeMultiplier> getCityBiomeMultipliers() {
        return cityBiomeMultipliers;
    }

    public void setCityBiomeMultipliers(List<CityBiomeMultiplier> cityBiomeMultipliers) {
        this.cityBiomeMultipliers = cityBiomeMultipliers == null ? new ArrayList<>() : new ArrayList<>(cityBiomeMultipliers);
    }

    public MultiSettings getMultiSettings() {
        return multiSettings;
    }

    public WorldSettings getSettings() {
        return settings;
    }

    public WorldPartSettings getParts() {
        return parts;
    }

    public ScatteredSettings getScattered() {
        return scattered;
    }

    public JsonElement getCitySpheres() {
        return citySpheres;
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
