package com.during.cityloader.resource.style;

/**
 * Represents a palette selection with a probability factor.
 */
public class PaletteSelection {
    private double factor;
    private String palette;

    public PaletteSelection(double factor, String palette) {
        this.factor = factor;
        this.palette = palette;
    }

    public double getFactor() {
        return factor;
    }

    public String getPalette() {
        return palette;
    }
}
