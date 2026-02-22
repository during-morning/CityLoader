package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 街道/方块样式设置
 */
public class StreetSettings {

    @SerializedName("border")
    private String border;

    @SerializedName("wall")
    private String wall;

    @SerializedName("street")
    private String street;

    @SerializedName(value = "streetbase", alternate = { "street_base", "streetBase" })
    private String streetBase;

    @SerializedName(value = "streetvariant", alternate = { "street_variant", "streetVariant" })
    private String streetVariant;

    @SerializedName(value = "width", alternate = { "street_width", "streetwidth", "streetWidth" })
    private Integer width;

    @SerializedName(value = "chance", alternate = { "park_chance", "parkchance", "parkChance" })
    private Float chance;

    @SerializedName("elevation")
    private String elevation;

    @SerializedName("roof")
    private String roof;

    @SerializedName("glass")
    private String glass;

    @SerializedName("inner")
    private String inner;

    @SerializedName("parts")
    private Map<String, String> parts = new LinkedHashMap<>();

    public String getBorder() {
        return border;
    }

    public String getWall() {
        return wall;
    }

    public String getStreet() {
        return street;
    }

    public String getStreetBase() {
        return streetBase;
    }

    public String getStreetVariant() {
        return streetVariant;
    }

    public Integer getWidth() {
        return width;
    }

    public Float getChance() {
        return chance;
    }

    public String getElevation() {
        return elevation;
    }

    public String getRoof() {
        return roof;
    }

    public String getGlass() {
        return glass;
    }

    public String getInner() {
        return inner;
    }

    public Map<String, String> getParts() {
        return parts;
    }

    public void setParts(Map<String, String> parts) {
        this.parts = parts == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parts);
    }

    // 向后兼容方法
    public Integer getStreetWidth() {
        return width;
    }

    public void setStreetWidth(Integer streetWidth) {
        this.width = streetWidth;
    }

    public Float getParkChance() {
        return chance;
    }

    public void setParkChance(Float parkChance) {
        this.chance = parkChance;
    }
}
