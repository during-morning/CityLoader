package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 选择器集合
 */
public class Selectors {

    @SerializedName(value = "buildings", alternate = { "building" })
    private List<SelectorEntry> buildings = new ArrayList<>();

    @SerializedName(value = "multibuildings", alternate = { "multibuilding", "multiBuilding", "multiBuildings" })
    private List<SelectorEntry> multiBuildings = new ArrayList<>();

    @SerializedName(value = "parts", alternate = { "part" })
    private List<SelectorEntry> parts = new ArrayList<>();

    @SerializedName(value = "palettes", alternate = { "palette" })
    private List<SelectorEntry> palettes = new ArrayList<>();

    @SerializedName("bridges")
    private List<SelectorEntry> bridges = new ArrayList<>();

    @SerializedName("fronts")
    private List<SelectorEntry> fronts = new ArrayList<>();

    @SerializedName("stairs")
    private List<SelectorEntry> stairs = new ArrayList<>();

    @SerializedName("fountains")
    private List<SelectorEntry> fountains = new ArrayList<>();

    @SerializedName("parks")
    private List<SelectorEntry> parks = new ArrayList<>();

    @SerializedName("raildungeons")
    private List<SelectorEntry> railDungeons = new ArrayList<>();

    public List<SelectorEntry> getBuildings() {
        return safe(buildings);
    }

    public void setBuildings(List<SelectorEntry> buildings) {
        this.buildings = safeMutable(buildings);
    }

    public List<SelectorEntry> getMultiBuildings() {
        return safe(multiBuildings);
    }

    public void setMultiBuildings(List<SelectorEntry> multiBuildings) {
        this.multiBuildings = safeMutable(multiBuildings);
    }

    public List<SelectorEntry> getParts() {
        return safe(parts);
    }

    public void setParts(List<SelectorEntry> parts) {
        this.parts = safeMutable(parts);
    }

    public List<SelectorEntry> getPalettes() {
        return safe(palettes);
    }

    public void setPalettes(List<SelectorEntry> palettes) {
        this.palettes = safeMutable(palettes);
    }

    public List<SelectorEntry> getBridges() {
        return safe(bridges);
    }

    public List<SelectorEntry> getFronts() {
        return safe(fronts);
    }

    public List<SelectorEntry> getStairs() {
        return safe(stairs);
    }

    public List<SelectorEntry> getFountains() {
        return safe(fountains);
    }

    public List<SelectorEntry> getParks() {
        return safe(parks);
    }

    public List<SelectorEntry> getRailDungeons() {
        return safe(railDungeons);
    }

    // 向后兼容方法
    public List<SelectorEntry> getBuilding() {
        return getBuildings();
    }

    public void setBuilding(List<SelectorEntry> building) {
        setBuildings(building);
    }

    public List<SelectorEntry> getMultiBuilding() {
        return getMultiBuildings();
    }

    public void setMultiBuilding(List<SelectorEntry> multiBuilding) {
        setMultiBuildings(multiBuilding);
    }

    public List<SelectorEntry> getPart() {
        return getParts();
    }

    public void setPart(List<SelectorEntry> part) {
        setParts(part);
    }

    public List<SelectorEntry> getPalette() {
        return getPalettes();
    }

    public void setPalette(List<SelectorEntry> palette) {
        setPalettes(palette);
    }

    private static List<SelectorEntry> safe(List<SelectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(entries);
    }

    private static List<SelectorEntry> safeMutable(List<SelectorEntry> entries) {
        return entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }
}
