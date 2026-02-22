package com.during.cityloader.resource.style;

/**
 * Represents a city style selection with a weight/factor and optional biome
 * restrictions.
 */
public class CityStyleSelection {
    private double factor;
    private String citystyle;
    private BiomeFilter biomes;

    public double getFactor() {
        return factor;
    }

    public String getCityStyle() {
        return citystyle;
    }

    public BiomeFilter getBiomes() {
        return biomes;
    }
}
