package com.during.cityloader.resource.scattered;

import java.util.List;

/**
 * Represents a scattered structure configuration.
 */
public class Scattered {
    private String id;
    private List<String> buildings;
    private String terrainheight;
    private String terrainfix;
    private int heightoffset;

    // For Gson
    public Scattered() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<String> getBuildings() {
        return buildings;
    }

    public String getTerrainHeight() {
        return terrainheight;
    }

    public String getTerrainFix() {
        return terrainfix;
    }

    public int getHeightOffset() {
        return heightoffset;
    }
}
