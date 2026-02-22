package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.StyleRE;
import com.during.cityloader.worldgen.lost.regassets.data.PaletteSelector;
import com.during.cityloader.worldgen.lost.regassets.data.SelectorEntry;
import com.during.cityloader.worldgen.lost.regassets.data.Selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 样式类
 */
public class Style implements ILostCityAsset {

    private final ResourceLocation name;
    private final List<List<WeightedPalette>> randomPaletteChoices = new ArrayList<>();
    private final Selectors selectors;

    public Style(StyleRE object) {
        this.name = object.getRegistryName();
        this.selectors = object.getSelectors() == null ? new Selectors() : object.getSelectors();

        List<List<PaletteSelector>> choices = object.getRandomPaletteChoices();
        if (choices != null) {
            for (List<PaletteSelector> group : choices) {
                List<WeightedPalette> convertedGroup = new ArrayList<>();
                if (group != null) {
                    for (PaletteSelector selector : group) {
                        if (selector == null || selector.palette() == null || selector.palette().isBlank()) {
                            continue;
                        }
                        float factor = selector.factor();
                        if (factor <= 0) {
                            continue;
                        }
                        convertedGroup.add(new WeightedPalette(factor, selector.palette()));
                    }
                }
                if (!convertedGroup.isEmpty()) {
                    randomPaletteChoices.add(convertedGroup);
                }
            }
        }
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public Selectors getSelectors() {
        return selectors;
    }

    public List<List<WeightedPalette>> getRandomPaletteChoices() {
        List<List<WeightedPalette>> result = new ArrayList<>();
        for (List<WeightedPalette> group : randomPaletteChoices) {
            result.add(Collections.unmodifiableList(group));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 从每个随机组中按权重选出一个调色板 ID。
     */
    public List<String> pickRandomPalettes(Random random) {
        List<String> result = new ArrayList<>();
        for (List<WeightedPalette> group : randomPaletteChoices) {
            String picked = pickFromGroup(group, random);
            if (picked != null) {
                result.add(picked);
            }
        }
        return result;
    }

    public String pickSelectorValue(String selector, Random random) {
        List<SelectorEntry> entries = switch (selector) {
            case "building", "buildings" -> selectors.getBuildings();
            case "multibuilding", "multibuildings", "multiBuilding", "multiBuildings" -> selectors.getMultiBuildings();
            case "part", "parts" -> selectors.getParts();
            case "palette", "palettes" -> selectors.getPalettes();
            default -> List.of();
        };

        if (entries.isEmpty()) {
            return null;
        }

        int total = 0;
        List<SelectorEntry> filtered = new ArrayList<>();
        for (SelectorEntry entry : entries) {
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

    private String pickFromGroup(List<WeightedPalette> group, Random random) {
        if (group == null || group.isEmpty()) {
            return null;
        }
        if (group.size() == 1) {
            return group.get(0).palette();
        }

        float totalWeight = 0.0f;
        for (WeightedPalette entry : group) {
            totalWeight += entry.factor();
        }
        if (totalWeight <= 0.0f) {
            return group.get(0).palette();
        }

        float value = random.nextFloat() * totalWeight;
        float current = 0.0f;
        for (WeightedPalette entry : group) {
            current += entry.factor();
            if (value <= current) {
                return entry.palette();
            }
        }
        return group.get(group.size() - 1).palette();
    }

    /**
     * 旧兼容方法（逐步废弃）：返回每组第一个调色板。
     */
    public List<String> getPalettes() {
        List<String> palettes = new ArrayList<>();
        for (List<WeightedPalette> group : randomPaletteChoices) {
            if (!group.isEmpty()) {
                palettes.add(group.get(0).palette());
            }
        }
        return Collections.unmodifiableList(palettes);
    }

    /**
     * 旧兼容方法（逐步废弃）：返回每组第一个调色板权重。
     */
    public List<Float> getWeights() {
        List<Float> weights = new ArrayList<>();
        for (List<WeightedPalette> group : randomPaletteChoices) {
            if (!group.isEmpty()) {
                weights.add(group.get(0).factor());
            }
        }
        return Collections.unmodifiableList(weights);
    }

    public record WeightedPalette(float factor, String palette) {
    }
}
