package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 建筑部件注册实体
 */
public class BuildingPartRE implements IAsset {

    private static final Gson GSON = new Gson();

    @SerializedName(value = "xsize", alternate = { "width" })
    private int width = 16;

    @SerializedName(value = "zsize", alternate = { "height" })
    private int height = 16;

    @SerializedName(value = "ysize", alternate = { "depth" })
    private Integer depth;

    @SerializedName("slices")
    private JsonElement slices;

    @SerializedName("palette")
    private JsonElement palette;

    @SerializedName("refpalette")
    private String refPalette;

    @SerializedName("meta")
    private JsonElement metadata;

    private transient List<String> cachedSlices;
    private transient List<List<String>> cachedSliceLayers;
    private transient ResourceLocation registryName;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDepth() {
        if (depth != null && depth > 0) {
            return depth;
        }
        List<List<String>> layers = getSliceLayers();
        if (layers == null || layers.isEmpty()) {
            return 1;
        }
        return layers.size();
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<String> getSlices() {
        parseSlicesIfNeeded();
        return cachedSlices;
    }

    public void setSlices(List<String> slices) {
        JsonArray array = new JsonArray();
        if (slices != null) {
            for (String slice : slices) {
                array.add(slice);
            }
        }
        this.slices = array;
        this.cachedSlices = null;
        this.cachedSliceLayers = null;
    }

    public List<List<String>> getSliceLayers() {
        parseSlicesIfNeeded();
        return cachedSliceLayers;
    }

    public String getPalette() {
        if (palette != null && palette.isJsonPrimitive()) {
            return palette.getAsString();
        }
        return refPalette;
    }

    public void setPalette(String palette) {
        this.palette = palette == null ? null : new JsonPrimitive(palette);
    }

    public String getRefPalette() {
        return refPalette;
    }

    public void setRefPalette(String refPalette) {
        this.refPalette = refPalette;
    }

    public PaletteRE getLocalPalette() {
        if (palette == null || !palette.isJsonObject()) {
            return null;
        }
        try {
            return GSON.fromJson(palette, PaletteRE.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 兼容：将 meta 对象/数组归一化为 key->value map。
     */
    public Map<String, Object> getMetadata() {
        if (metadata == null || metadata.isJsonNull()) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (metadata.isJsonObject()) {
            result.putAll(GSON.fromJson(metadata, Map.class));
            return Collections.unmodifiableMap(result);
        }

        if (metadata.isJsonArray()) {
            for (JsonElement element : metadata.getAsJsonArray()) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                Map<String, Object> row = GSON.fromJson(element, Map.class);
                Object key = row.get("key");
                Object value = row.get("char");
                if (key instanceof String && value != null) {
                    result.put((String) key, value);
                } else {
                    result.putAll(row);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private void parseSlicesIfNeeded() {
        if (cachedSlices != null && cachedSliceLayers != null) {
            return;
        }

        List<String> flattened = new ArrayList<>();
        List<List<String>> layers = new ArrayList<>();

        if (slices != null && slices.isJsonArray()) {
            JsonArray root = slices.getAsJsonArray();
            for (JsonElement layerElement : root) {
                if (layerElement == null || layerElement.isJsonNull()) {
                    continue;
                }
                if (layerElement.isJsonPrimitive()) {
                    String row = layerElement.getAsString();
                    flattened.add(row);
                    layers.add(List.of(row));
                    continue;
                }
                if (layerElement.isJsonArray()) {
                    List<String> rows = new ArrayList<>();
                    for (JsonElement rowElement : layerElement.getAsJsonArray()) {
                        if (rowElement != null && rowElement.isJsonPrimitive()) {
                            rows.add(rowElement.getAsString());
                        }
                    }
                    if (!rows.isEmpty()) {
                        layers.add(Collections.unmodifiableList(rows));
                        flattened.add(String.join("\n", rows));
                    }
                }
            }
        }

        cachedSlices = Collections.unmodifiableList(flattened);
        cachedSliceLayers = Collections.unmodifiableList(layers);
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
