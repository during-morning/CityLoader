package com.during.cityloader.resource.style;

import java.util.List;

/**
 * Configuration for scattered structures generation in a world style.
 */
public class ScatteredSettings {
    private int areasize;
    private double chance;
    private int weightnone;
    private List<ScatteredEntry> list;

    public int getAreaSize() {
        return areasize;
    }

    public double getChance() {
        return chance;
    }

    public int getWeightNone() {
        return weightnone;
    }

    public List<ScatteredEntry> getList() {
        return list;
    }
}
