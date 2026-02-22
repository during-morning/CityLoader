package com.during.cityloader.resource.style;

import java.util.List;

/**
 * filter for biomes, supporting inclusion and exclusion lists.
 */
public class BiomeFilter {
    private List<String> if_any;
    private List<String> excluding;

    public BiomeFilter(List<String> if_any, List<String> excluding) {
        this.if_any = if_any;
        this.excluding = excluding;
    }

    public List<String> getIfAny() {
        return if_any;
    }

    public List<String> getExcluding() {
        return excluding;
    }
}
