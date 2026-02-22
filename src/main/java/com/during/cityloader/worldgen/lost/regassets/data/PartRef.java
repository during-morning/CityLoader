package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 部件引用数据类
 */
public class PartRef {

    private static final Gson GSON = new Gson();

    @SerializedName("part")
    private String part;

    @SerializedName(value = "weight", alternate = { "factor" })
    private float weight = 1.0f;

    @SerializedName(value = "transform", alternate = { "xform" })
    private JsonElement transform;

    @SerializedName("condition")
    private ConditionTest condition;

    @SerializedName("conditions")
    private List<ConditionWrapper> conditions = new ArrayList<>();

    // 兼容 LostCities 的“扁平条件字段”写法（直接写在 part 节点上）
    @SerializedName("top")
    private Boolean top;

    @SerializedName("ground")
    private Boolean ground;

    @SerializedName("cellar")
    private Boolean cellar;

    @SerializedName("isbuilding")
    private Boolean isBuilding;

    @SerializedName("issphere")
    private Boolean isSphere;

    @SerializedName("floor")
    private Integer floor;

    @SerializedName("chunkx")
    private Integer chunkX;

    @SerializedName("chunkz")
    private Integer chunkZ;

    @SerializedName("belowPart")
    private JsonElement belowPart;

    @SerializedName("inpart")
    private JsonElement inPart;

    @SerializedName("inbuilding")
    private JsonElement inBuilding;

    @SerializedName("inbiome")
    private JsonElement inBiome;

    @SerializedName("range")
    private String range;

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public Integer getTransformCode() {
        return parseTransformCode(transform);
    }

    public void setTransformCode(Integer transformCode) {
        this.transform = transformCode == null ? null : new JsonPrimitive(transformCode);
    }

    public ConditionTest getCondition() {
        if (condition != null) {
            return condition;
        }
        ConditionTest inline = parseInlineCondition();
        if (inline != null) {
            return inline;
        }
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        for (ConditionWrapper wrapper : conditions) {
            ConditionTest parsed = wrapper.asConditionTest();
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private ConditionTest parseInlineCondition() {
        JsonObject json = new JsonObject();
        if (top != null) {
            json.addProperty("top", top);
        }
        if (ground != null) {
            json.addProperty("ground", ground);
        }
        if (cellar != null) {
            json.addProperty("cellar", cellar);
        }
        if (isBuilding != null) {
            json.addProperty("isbuilding", isBuilding);
        }
        if (isSphere != null) {
            json.addProperty("issphere", isSphere);
        }
        if (floor != null) {
            json.addProperty("floor", floor);
        }
        if (chunkX != null) {
            json.addProperty("chunkx", chunkX);
        }
        if (chunkZ != null) {
            json.addProperty("chunkz", chunkZ);
        }
        if (belowPart != null) {
            json.add("belowPart", belowPart.deepCopy());
        }
        if (inPart != null) {
            json.add("inpart", inPart.deepCopy());
        }
        if (inBuilding != null) {
            json.add("inbuilding", inBuilding.deepCopy());
        }
        if (inBiome != null) {
            json.add("inbiome", inBiome.deepCopy());
        }
        if (range != null && !range.isBlank()) {
            json.addProperty("range", range);
        }
        if (json.entrySet().isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, ConditionTest.class);
    }

    private Integer parseTransformCode(JsonElement raw) {
        if (raw == null || raw.isJsonNull() || !raw.isJsonPrimitive()) {
            return null;
        }

        JsonPrimitive primitive = raw.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return primitive.getAsInt();
        }
        if (!primitive.isString()) {
            return null;
        }

        String text = primitive.getAsString();
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            // continue with named transform aliases
        }

        return switch (normalized) {
            case "rotate_none", "rot0", "none" -> 0;
            case "rotate_90", "rot90", "rotate90" -> 1;
            case "rotate_180", "rot180", "rotate180" -> 2;
            case "rotate_270", "rot270", "rotate270" -> 3;
            case "mirror_x", "mirrorx" -> 4;
            case "mirror_z", "mirrorz" -> 5;
            case "mirror_90_x", "mirror90x", "mirror_90x", "mirror90_x" -> 6;
            default -> null;
        };
    }

    public void setCondition(ConditionTest condition) {
        this.condition = condition;
    }

    public List<ConditionWrapper> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionWrapper> conditions) {
        this.conditions = conditions == null ? new ArrayList<>() : new ArrayList<>(conditions);
    }

    public static class ConditionWrapper {

        @SerializedName("condition")
        private JsonElement condition;

        public ConditionTest asConditionTest() {
            if (condition == null || !condition.isJsonObject()) {
                return null;
            }
            try {
                return GSON.fromJson(condition, ConditionTest.class);
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
}
