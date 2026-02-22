package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.ScatteredRE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 散布建筑类
 * 用于表示随机散布的建筑
 * 
 * @author During
 * @since 1.4.0
 */
public class ScatteredBuilding implements ILostCityAsset {

    private final ResourceLocation name;
    private final List<String> buildings;
    private final String multiBuilding;
    private final String terrainHeight;
    private final String terrainFix;
    private final int heightOffset;
    private final float chance;

    /**
     * 从ScatteredRE构造ScatteredBuilding对象
     * 
     * @param object ScatteredRE注册实体
     */
    public ScatteredBuilding(ScatteredRE object) {
        this.name = object.getRegistryName();

        List<String> normalizedBuildings = new ArrayList<>();
        if (object.getBuildings() != null) {
            for (String building : object.getBuildings()) {
                if (building != null && !building.isBlank()) {
                    normalizedBuildings.add(building);
                }
            }
        }
        if (normalizedBuildings.isEmpty() && object.getBuilding() != null && !object.getBuilding().isBlank()) {
            normalizedBuildings.add(object.getBuilding());
        }
        this.buildings = Collections.unmodifiableList(normalizedBuildings);

        this.multiBuilding = object.getMultiBuilding();
        this.terrainHeight = object.getTerrainHeight() == null
                ? "highest"
                : object.getTerrainHeight().toLowerCase(Locale.ROOT);
        this.terrainFix = object.getTerrainFix() == null
                ? "none"
                : object.getTerrainFix().toLowerCase(Locale.ROOT);
        this.heightOffset = object.getHeightOffset() == null ? 0 : object.getHeightOffset();
        this.chance = object.getChance() == null ? 1.0f : object.getChance();
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public List<String> getBuildings() {
        return buildings;
    }

    public String getMultiBuilding() {
        return multiBuilding;
    }

    public String getTerrainHeight() {
        return terrainHeight;
    }

    public String getTerrainFix() {
        return terrainFix;
    }

    public int getHeightOffset() {
        return heightOffset;
    }

    public float getChance() {
        return chance;
    }

    public boolean hasSingleBuilding() {
        return !buildings.isEmpty();
    }

    public boolean hasMultiBuilding() {
        return multiBuilding != null && !multiBuilding.isBlank();
    }

    public String pickBuilding(Random random) {
        if (buildings.isEmpty()) {
            return null;
        }
        if (buildings.size() == 1 || random == null) {
            return buildings.get(0);
        }
        return buildings.get(random.nextInt(buildings.size()));
    }
}
