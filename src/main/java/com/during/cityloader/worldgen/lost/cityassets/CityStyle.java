package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.CityStyleRE;
import com.during.cityloader.worldgen.lost.regassets.data.BuildingSettings;
import com.during.cityloader.worldgen.lost.regassets.data.RailSettings;
import com.during.cityloader.worldgen.lost.regassets.data.SelectorEntry;
import com.during.cityloader.worldgen.lost.regassets.data.Selectors;
import com.during.cityloader.worldgen.lost.regassets.data.StreetSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 城市样式类
 */
public class CityStyle implements ILostCityAsset {

    private final ResourceLocation name;
    private final String style;
    private final String inherit;
    private final Float buildingChance;
    private final Float explosionChance;
    private final List<String> stuffTags;

    private final Selectors selectors;
    private final BuildingSettings buildingSettings;
    private final StreetSettings streetBlocks;
    private final Map<String, String> parkBlocks;
    private final Map<String, String> corridorBlocks;
    private final RailSettings railBlocks;
    private final Map<String, String> sphereBlocks;

    // 旧兼容字段
    private final List<String> legacyBuildings;
    private final List<Float> legacyBuildingWeights;
    private final List<String> legacyMultiBuildings;
    private final List<Float> legacyMultiBuildingWeights;

    public CityStyle(CityStyleRE object) {
        this.name = object.getRegistryName();
        this.style = object.getStyle();
        this.inherit = object.getInherit();
        this.buildingChance = object.getBuildingChance();
        this.explosionChance = object.getExplosionChance();
        this.stuffTags = object.getStuffTags() == null ? List.of() : List.copyOf(object.getStuffTags());

        this.selectors = object.getSelectors() == null ? new Selectors() : object.getSelectors();
        this.buildingSettings = object.getBuildingSettings();
        this.streetBlocks = object.getStreetBlocks();
        this.parkBlocks = object.getParkBlocks() == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(object.getParkBlocks()));
        this.corridorBlocks = object.getCorridorBlocks() == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(object.getCorridorBlocks()));
        this.railBlocks = object.getRailBlocks();
        this.sphereBlocks = object.getSphereBlocks() == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(object.getSphereBlocks()));

        this.legacyBuildings = object.getBuildings() == null ? List.of() : List.copyOf(object.getBuildings());
        this.legacyBuildingWeights = object.getBuildingWeights() == null ? List.of() : List.copyOf(object.getBuildingWeights());
        this.legacyMultiBuildings = object.getMultiBuildings() == null ? List.of() : List.copyOf(object.getMultiBuildings());
        this.legacyMultiBuildingWeights = object.getMultiBuildingWeights() == null ? List.of() : List.copyOf(object.getMultiBuildingWeights());
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public String getStyle() {
        return style;
    }

    public String getInherit() {
        return inherit;
    }

    public Float getBuildingChance() {
        return buildingChance;
    }

    public Float getExplosionChance() {
        return explosionChance;
    }

    public List<String> getStuffTags() {
        return stuffTags;
    }

    public Selectors getSelectors() {
        return selectors;
    }

    public BuildingSettings getBuildingSettings() {
        return buildingSettings;
    }

    public StreetSettings getStreetBlocks() {
        return streetBlocks;
    }

    public Map<String, String> getParkBlocks() {
        return parkBlocks;
    }

    public Map<String, String> getCorridorBlocks() {
        return corridorBlocks;
    }

    public RailSettings getRailBlocks() {
        return railBlocks;
    }

    public Map<String, String> getSphereBlocks() {
        return sphereBlocks;
    }

    public List<SelectorEntry> getSelector(String key) {
        if (key == null) {
            return List.of();
        }
        return switch (key) {
            case "buildings", "building" -> selectors.getBuildings();
            case "multibuildings", "multiBuildings", "multibuilding" -> selectors.getMultiBuildings();
            case "bridges" -> selectors.getBridges();
            case "fronts" -> selectors.getFronts();
            case "stairs" -> selectors.getStairs();
            case "fountains" -> selectors.getFountains();
            case "parks" -> selectors.getParks();
            case "raildungeons" -> selectors.getRailDungeons();
            case "parts", "part" -> selectors.getParts();
            case "palettes", "palette" -> selectors.getPalettes();
            default -> List.of();
        };
    }

    public String pickSelectorValue(String key, Random random) {
        List<SelectorEntry> candidates = getSelector(key);
        if (candidates.isEmpty()) {
            return null;
        }
        int total = 0;
        List<SelectorEntry> filtered = new ArrayList<>();
        for (SelectorEntry entry : candidates) {
            if (entry == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            int weight = Math.max(1, Math.round(entry.getFactor() * 100));
            total += weight;
            filtered.add(entry);
        }
        if (filtered.isEmpty()) {
            return null;
        }
        if (filtered.size() == 1 || total <= 0) {
            return filtered.get(0).getValue();
        }

        int roll = random.nextInt(total);
        int current = 0;
        for (SelectorEntry entry : filtered) {
            current += Math.max(1, Math.round(entry.getFactor() * 100));
            if (roll < current) {
                return entry.getValue();
            }
        }
        return filtered.get(filtered.size() - 1).getValue();
    }

    // 向后兼容：若旧字段为空，则回退到 selectors
    public List<String> getBuildings() {
        if (!legacyBuildings.isEmpty()) {
            return legacyBuildings;
        }
        List<String> values = new ArrayList<>();
        for (SelectorEntry entry : selectors.getBuildings()) {
            if (entry != null && entry.getValue() != null) {
                values.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(values);
    }

    public List<Float> getBuildingWeights() {
        if (!legacyBuildingWeights.isEmpty()) {
            return legacyBuildingWeights;
        }
        List<Float> values = new ArrayList<>();
        for (SelectorEntry entry : selectors.getBuildings()) {
            values.add(entry == null ? 1.0f : entry.getFactor());
        }
        return Collections.unmodifiableList(values);
    }

    public List<String> getMultiBuildings() {
        if (!legacyMultiBuildings.isEmpty()) {
            return legacyMultiBuildings;
        }
        List<String> values = new ArrayList<>();
        for (SelectorEntry entry : selectors.getMultiBuildings()) {
            if (entry != null && entry.getValue() != null) {
                values.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(values);
    }

    public List<Float> getMultiBuildingWeights() {
        if (!legacyMultiBuildingWeights.isEmpty()) {
            return legacyMultiBuildingWeights;
        }
        List<Float> values = new ArrayList<>();
        for (SelectorEntry entry : selectors.getMultiBuildings()) {
            values.add(entry == null ? 1.0f : entry.getFactor());
        }
        return Collections.unmodifiableList(values);
    }
}
