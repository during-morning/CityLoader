package com.during.cityloader.resource.style;

/**
 * Encapsulates a biome multiplier configuration for city generation
 * probability.
 */
public class BiomeMultiplier {
    private double multiplier;
    private BiomeFilter biomes;

    public double getMultiplier() {
        return multiplier;
    }

    public BiomeFilter getBiomes() {
        return biomes;
    }
}
