package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 多建筑注册实体
 * 用于从JSON反序列化多建筑数据
 * 
 * @author During
 * @since 1.4.0
 */
public class MultiBuildingRE implements IAsset {
    
    @SerializedName("dimx")
    private int dimX = 2;
    
    @SerializedName("dimz")
    private int dimZ = 2;
    
    @SerializedName("buildings")
    private List<List<String>> buildings = new ArrayList<>();
    
    private transient ResourceLocation registryName;
    
    public int getDimX() {
        return dimX;
    }
    
    public void setDimX(int dimX) {
        this.dimX = dimX;
    }
    
    public int getDimZ() {
        return dimZ;
    }
    
    public void setDimZ(int dimZ) {
        this.dimZ = dimZ;
    }
    
    public List<List<String>> getBuildings() {
        return buildings;
    }
    
    public void setBuildings(List<List<String>> buildings) {
        this.buildings = buildings;
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
