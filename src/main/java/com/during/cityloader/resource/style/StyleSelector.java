package com.during.cityloader.resource.style;

/**
 * A selector used in styles to choose building/element values with a
 * weight/factor.
 */
public class StyleSelector {
    private double factor;
    private String value;

    public StyleSelector(double factor, String value) {
        this.factor = factor;
        this.value = value;
    }

    public double getFactor() {
        return factor;
    }

    public String getValue() {
        return value;
    }
}
