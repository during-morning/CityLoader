package com.during.cityloader.resource.style;

import java.util.List;
import java.util.Map;

/**
 * Represents a city style configuration, defining specific block types for
 * streets, parks, and other city elements.
 */
public class CityStyle {
    private String id;
    private String inherit;
    private List<String> stuff_tags;

    // Block mappings (logical name -> palette char)
    private Map<String, String> streetblocks;
    private Map<String, String> parkblocks;
    private Map<String, String> corridorblocks;
    private Map<String, String> railblocks;
    private Map<String, String> sphereblocks;

    private Map<String, List<StyleSelector>> selectors;

    public CityStyle(String id) {
        this.id = id;
    }

    // For Gson deserialization
    public CityStyle() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getInherit() {
        return inherit;
    }

    public List<String> getStuffTags() {
        return stuff_tags;
    }

    public Map<String, String> getStreetBlocks() {
        return streetblocks;
    }

    public Map<String, String> getParkBlocks() {
        return parkblocks;
    }

    public Map<String, String> getCorridorBlocks() {
        return corridorblocks;
    }

    public Map<String, String> getRailBlocks() {
        return railblocks;
    }

    public Map<String, String> getSphereBlocks() {
        return sphereblocks;
    }

    public Map<String, List<StyleSelector>> getSelectors() {
        return selectors;
    }
}
