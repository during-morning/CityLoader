package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.PartRef;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 建筑注册实体
 * 用于从JSON反序列化建筑数据
 * 
 * @author During
 * @since 1.4.0
 */
public class BuildingRE implements IAsset {
    
    @SerializedName("minfloors")
    private int minFloors = 1;
    
    @SerializedName("maxfloors")
    private int maxFloors = 10;
    
    @SerializedName("mincellars")
    private int minCellars = 0;
    
    @SerializedName("maxcellars")
    private int maxCellars = 0;
    
    @SerializedName("filler")
    private String filler = "#";
    
    @SerializedName("rubble")
    private String rubble;
    
    @SerializedName("refpalette")
    private String refPalette;
    
    @SerializedName("palette")
    private PaletteRE localPalette;
    
    @SerializedName("allowDoors")
    private Boolean allowDoors = true;
    
    @SerializedName("allowFillers")
    private Boolean allowFillers = true;
    
    @SerializedName("overrideFloors")
    private Boolean overrideFloors = false;
    
    @SerializedName("preferslonely")
    private Float prefersLonely = 0.0f;
    
    @SerializedName("parts")
    private List<PartRef> parts = new ArrayList<>();
    
    @SerializedName("parts2")
    private List<PartRef> parts2 = new ArrayList<>();
    
    private transient ResourceLocation registryName;
    
    public int getMinFloors() {
        return minFloors;
    }
    
    public void setMinFloors(int minFloors) {
        this.minFloors = minFloors;
    }
    
    public int getMaxFloors() {
        return maxFloors;
    }
    
    public void setMaxFloors(int maxFloors) {
        this.maxFloors = maxFloors;
    }
    
    public int getMinCellars() {
        return minCellars;
    }
    
    public void setMinCellars(int minCellars) {
        this.minCellars = minCellars;
    }
    
    public int getMaxCellars() {
        return maxCellars;
    }
    
    public void setMaxCellars(int maxCellars) {
        this.maxCellars = maxCellars;
    }
    
    public List<PartRef> getParts() {
        return parts;
    }
    
    public void setParts(List<PartRef> parts) {
        this.parts = parts;
    }
    
    public List<PartRef> getParts2() {
        return parts2;
    }
    
    public void setParts2(List<PartRef> parts2) {
        this.parts2 = parts2;
    }
    
    public String getRefPalette() {
        return refPalette;
    }
    
    public void setRefPalette(String refPalette) {
        this.refPalette = refPalette;
    }
    
    public PaletteRE getLocalPalette() {
        return localPalette;
    }
    
    public void setLocalPalette(PaletteRE localPalette) {
        this.localPalette = localPalette;
    }
    
    public String getFiller() {
        return filler;
    }
    
    public void setFiller(String filler) {
        this.filler = filler;
    }
    
    public String getRubble() {
        return rubble;
    }
    
    public void setRubble(String rubble) {
        this.rubble = rubble;
    }
    
    public Boolean getAllowDoors() {
        return allowDoors;
    }
    
    public void setAllowDoors(Boolean allowDoors) {
        this.allowDoors = allowDoors;
    }
    
    public Boolean getAllowFillers() {
        return allowFillers;
    }
    
    public void setAllowFillers(Boolean allowFillers) {
        this.allowFillers = allowFillers;
    }
    
    public Boolean getOverrideFloors() {
        return overrideFloors;
    }
    
    public void setOverrideFloors(Boolean overrideFloors) {
        this.overrideFloors = overrideFloors;
    }
    
    public Float getPrefersLonely() {
        return prefersLonely;
    }
    
    public void setPrefersLonely(Float prefersLonely) {
        this.prefersLonely = prefersLonely;
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
