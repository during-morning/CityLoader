package com.during.cityloader.resource.stuff;

import java.util.List;

/**
 * Filter for blocks, supporting inclusion and exclusion lists.
 */
public class BlockFilter {
    private List<String> if_any;
    private List<String> excluding;

    public List<String> getIfAny() {
        return if_any;
    }

    public List<String> getExcluding() {
        return excluding;
    }
}
