package com.during.cityloader.resource;

import java.util.List;

/**
 * Data class representing a multi-chunk building structure.
 * Consists of a grid of individual buildings.
 */
public class MultiBuilding {
    private final String id;
    private final int dimX;
    private final int dimZ;
    private final List<List<String>> buildings;

    public MultiBuilding(String id, int dimX, int dimZ, List<List<String>> buildings) {
        this.id = id;
        this.dimX = dimX;
        this.dimZ = dimZ;
        this.buildings = buildings;
    }

    public String getId() {
        return id;
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimZ() {
        return dimZ;
    }

    /**
     * Get the 2D grid of building IDs.
     * 
     * @return List of rows, where each row is a list of building IDs.
     */
    public List<List<String>> getBuildings() {
        return buildings;
    }

    /**
     * Get the building ID at a specific grid position.
     * 
     * @param x Chunk X offset (0 to dimX-1)
     * @param z Chunk Z offset (0 to dimZ-1)
     * @return Building ID, or null if out of bounds.
     */
    public String getBuildingAt(int x, int z) {
        if (x < 0 || x >= dimX || z < 0 || z >= dimZ) {
            return null;
        }
        if (z >= buildings.size()) {
            return null;
        }
        List<String> row = buildings.get(z);
        if (x >= row.size()) {
            return null;
        }
        return row.get(x);
    }
}
