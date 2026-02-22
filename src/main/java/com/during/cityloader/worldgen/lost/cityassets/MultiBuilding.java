package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.ILostCityMultiBuilding;
import com.during.cityloader.worldgen.lost.regassets.MultiBuildingRE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多建筑类
 */
public class MultiBuilding implements ILostCityAsset, ILostCityMultiBuilding {

    private final ResourceLocation name;
    private final int dimX;
    private final int dimZ;
    private final List<List<String>> buildings;

    public MultiBuilding(MultiBuildingRE object) {
        this.name = object.getRegistryName();
        this.dimX = object.getDimX();
        this.dimZ = object.getDimZ();

        List<List<String>> copy = new ArrayList<>();
        if (object.getBuildings() != null) {
            for (List<String> row : object.getBuildings()) {
                copy.add(row == null ? List.of() : List.copyOf(row));
            }
        }
        this.buildings = Collections.unmodifiableList(copy);
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    @Override
    public int getDimX() {
        return dimX;
    }

    @Override
    public int getDimZ() {
        return dimZ;
    }

    public List<List<String>> getBuildings() {
        return buildings;
    }

    public String getBuildingAt(int x, int z) {
        if (z < 0 || z >= buildings.size()) {
            return null;
        }
        List<String> row = buildings.get(z);
        if (row == null || x < 0 || x >= row.size()) {
            return null;
        }
        return row.get(x);
    }
}
