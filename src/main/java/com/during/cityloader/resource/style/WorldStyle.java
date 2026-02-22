package com.during.cityloader.resource.style;

import java.util.List;
import java.util.Map;

/**
 * Represents the global world generation settings, including city placement
 * rules and scattered structures.
 */
public class WorldStyle {
    private String id;
    private String outsidestyle;
    private Map<String, Object> settings;
    private Map<String, Object> multisettings;
    private ScatteredSettings scattered;
    private List<BiomeMultiplier> citybiomemultipliers;
    private List<CityStyleSelection> citystyles;

    public WorldStyle(String id) {
        this.id = id;
    }

    // For Gson deserialization
    public WorldStyle() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getOutsideStyle() {
        return outsidestyle;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public Map<String, Object> getMultiSettings() {
        return multisettings;
    }

    public ScatteredSettings getScattered() {
        return scattered;
    }

    public List<BiomeMultiplier> getCityBiomeMultipliers() {
        return citybiomemultipliers;
    }

    public List<CityStyleSelection> getCityStyles() {
        return citystyles;
    }
}
