package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.google.gson.annotations.SerializedName;

/**
 * 预定义球体注册实体
 * 用于从JSON反序列化预定义球体数据
 * 
 * @author During
 * @since 1.4.0
 */
public class PredefinedSphereRE implements IAsset {
    
    @SerializedName("x")
    private int x;
    
    @SerializedName("z")
    private int z;
    
    @SerializedName("radius")
    private int radius = 50;
    
    private transient ResourceLocation registryName;
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getZ() {
        return z;
    }
    
    public void setZ(int z) {
        this.z = z;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
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
