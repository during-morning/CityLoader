package com.during.cityloader.resource.style;

/**
 * Represents a single scattered building entry in the configuration.
 */
public class ScatteredEntry {
    private String name;
    private int weight;
    private int maxheightdiff;
    private BiomeFilter biomes;

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public int getMaxHeightDiff() {
        return maxheightdiff;
    }

    public BiomeFilter getBiomes() {
        return biomes;
    }
}
