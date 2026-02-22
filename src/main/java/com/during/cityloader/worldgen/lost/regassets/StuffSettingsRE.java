package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 装饰物设置注册实体
 * 用于从JSON反序列化装饰物设置数据
 * 
 * @author During
 * @since 1.4.0
 */
public class StuffSettingsRE implements IAsset {
    
    @SerializedName("block")
    private String block;
    
    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();
    
    private transient ResourceLocation registryName;
    
    public String getBlock() {
        return block;
    }
    
    public void setBlock(String block) {
        this.block = block;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
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
