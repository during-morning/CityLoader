package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 条件测试数据类
 */
public class ConditionTest {

    @SerializedName("top")
    private Boolean top;

    @SerializedName("ground")
    private Boolean ground;

    @SerializedName("cellar")
    private Boolean cellar;

    @SerializedName("isbuilding")
    private Boolean isbuilding;

    @SerializedName("issphere")
    private Boolean issphere;

    @SerializedName("floor")
    private Integer floor;

    @SerializedName("chunkx")
    private Integer chunkx;

    @SerializedName("chunkz")
    private Integer chunkz;

    @SerializedName("belowPart")
    private JsonElement belowPart;

    @SerializedName("inpart")
    private JsonElement inpart;

    @SerializedName("inbuilding")
    private JsonElement inbuilding;

    @SerializedName("inbiome")
    private JsonElement inbiome;

    @SerializedName("range")
    private String range;

    public ConditionTest() {
    }

    public ConditionTest(Optional<Boolean> top,
                         Optional<Boolean> ground,
                         Optional<Boolean> cellar,
                         Optional<Boolean> isbuilding,
                         Optional<Boolean> issphere,
                         Optional<Integer> floor,
                         Optional<Integer> chunkx,
                         Optional<Integer> chunkz,
                         Optional<Set<String>> belowPart,
                         Optional<Set<String>> inpart,
                         Optional<Set<String>> inbuilding,
                         Optional<Set<String>> inbiome,
                         Optional<String> range) {
        this.top = top.orElse(null);
        this.ground = ground.orElse(null);
        this.cellar = cellar.orElse(null);
        this.isbuilding = isbuilding.orElse(null);
        this.issphere = issphere.orElse(null);
        this.floor = floor.orElse(null);
        this.chunkx = chunkx.orElse(null);
        this.chunkz = chunkz.orElse(null);
        this.belowPart = setToJson(belowPart.orElse(null));
        this.inpart = setToJson(inpart.orElse(null));
        this.inbuilding = setToJson(inbuilding.orElse(null));
        this.inbiome = setToJson(inbiome.orElse(null));
        this.range = range.orElse(null);
    }

    private JsonElement setToJson(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private Set<String> toStringSet(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        Set<String> result = new LinkedHashSet<>();
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                result.add(primitive.getAsString());
            }
            return result;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    result.add(child.getAsString());
                }
            }
            return result;
        }

        return null;
    }

    public Boolean getTop() {
        return top;
    }

    public Boolean getGround() {
        return ground;
    }

    public Boolean getCellar() {
        return cellar;
    }

    public Boolean getIsbuilding() {
        return isbuilding;
    }

    public Boolean getIssphere() {
        return issphere;
    }

    public Integer getFloor() {
        return floor;
    }

    public Integer getChunkx() {
        return chunkx;
    }

    public Integer getChunkz() {
        return chunkz;
    }

    public Set<String> getBelowPart() {
        return toStringSet(belowPart);
    }

    public Set<String> getInpart() {
        return toStringSet(inpart);
    }

    public Set<String> getInbuilding() {
        return toStringSet(inbuilding);
    }

    public Set<String> getInbiome() {
        return toStringSet(inbiome);
    }

    public String getRange() {
        return range;
    }
}
