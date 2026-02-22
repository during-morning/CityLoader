package com.during.cityloader.resource.stuff;

import java.util.List;

/**
 * Represents a "stuff" configuration (decorations, hazards, etc.).
 */
public class Stuff {
    private String id;
    private List<String> tags;

    // JSON: "column": "\\" (string) or "column": "c"
    private String column;

    private int mincount;
    private int maxcount;
    private int attempts;
    private boolean inbuilding;
    private BlockFilter upperblocks;

    // For Gson
    public Stuff() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getColumn() {
        return column;
    }

    public int getMinCount() {
        return mincount;
    }

    public int getMaxCount() {
        return maxcount;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isInBuilding() {
        return inbuilding;
    }

    public BlockFilter getUpperBlocks() {
        return upperblocks;
    }
}
