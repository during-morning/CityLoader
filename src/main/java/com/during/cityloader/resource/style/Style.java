package com.during.cityloader.resource.style;

import java.util.List;
import java.util.Map;

/**
 * Represents a generic style configuration used to define building palettes and
 * variant selections.
 */
public class Style {
    private String id;
    private List<List<PaletteSelection>> randompalettes;
    private Map<String, List<StyleSelector>> selectors;

    public Style(String id, List<List<PaletteSelection>> randompalettes, Map<String, List<StyleSelector>> selectors) {
        this.id = id;
        this.randompalettes = randompalettes;
        this.selectors = selectors;
    }

    // For Gson deserialization
    public Style() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<List<PaletteSelection>> getRandomPalettes() {
        return randompalettes;
    }

    public Map<String, List<StyleSelector>> getSelectors() {
        return selectors;
    }
}
