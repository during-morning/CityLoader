package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.PaletteSelector;
import com.during.cityloader.worldgen.lost.regassets.data.Selectors;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 样式注册实体
 */
public class StyleRE implements IAsset {

    /**
     * LostCities 主格式：随机调色板组
     */
    @SerializedName(value = "randompalettes", alternate = { "random_palettes", "randomPalettes" })
    private List<List<PaletteSelector>> randomPaletteChoices = new ArrayList<>();

    /**
     * 选择器集合
     */
    @SerializedName("selectors")
    private Selectors selectors = new Selectors();

    /**
     * 旧兼容字段（逐步废弃）
     */
    @SerializedName("palettes")
    private List<String> palettes = new ArrayList<>();

    /**
     * 旧兼容字段（逐步废弃）
     */
    @SerializedName("weights")
    private List<Float> weights = new ArrayList<>();

    private transient ResourceLocation registryName;

    /**
     * 获取随机调色板组。
     * 当仅存在旧字段 palettes/weights 时，按 1:1 转换成随机调色板组。
     */
    public List<List<PaletteSelector>> getRandomPaletteChoices() {
        if (randomPaletteChoices != null && !randomPaletteChoices.isEmpty()) {
            return randomPaletteChoices;
        }
        return convertLegacyPalettes();
    }

    public void setRandomPaletteChoices(List<List<PaletteSelector>> randomPaletteChoices) {
        this.randomPaletteChoices = randomPaletteChoices == null ? new ArrayList<>() : randomPaletteChoices;
    }

    public Selectors getSelectors() {
        return selectors;
    }

    public List<String> getPalettes() {
        return palettes;
    }

    public void setPalettes(List<String> palettes) {
        this.palettes = palettes;
    }

    public List<Float> getWeights() {
        return weights;
    }

    public void setWeights(List<Float> weights) {
        this.weights = weights;
    }

    public boolean isLegacyFormat() {
        return (randomPaletteChoices == null || randomPaletteChoices.isEmpty())
                && palettes != null && !palettes.isEmpty();
    }

    private List<List<PaletteSelector>> convertLegacyPalettes() {
        List<List<PaletteSelector>> converted = new ArrayList<>();
        if (palettes == null || palettes.isEmpty()) {
            return converted;
        }

        for (int i = 0; i < palettes.size(); i++) {
            String palette = palettes.get(i);
            float factor = 1.0f;
            if (weights != null && i < weights.size() && weights.get(i) != null) {
                factor = weights.get(i);
            }

            List<PaletteSelector> group = new ArrayList<>();
            group.add(new PaletteSelector(factor, palette));
            converted.add(group);
        }
        return converted;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public void setRegistryName(ResourceLocation name) {
        this.registryName = name;
    }
}
