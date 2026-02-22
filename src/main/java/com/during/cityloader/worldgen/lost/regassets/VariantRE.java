package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.BlockEntry;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 变体注册实体
 * 用于从JSON反序列化变体数据
 * 
 * @author During
 * @since 1.4.0
 */
public class VariantRE implements IAsset {
    
    /**
     * LostCities 主格式：加权方块列表
     */
    @SerializedName("blocks")
    private List<BlockEntry> blocks = new ArrayList<>();

    /**
     * 旧兼容字段（逐步废弃）
     */
    @SerializedName("name")
    private String name;
    
    /**
     * 旧兼容字段（逐步废弃）
     */
    @SerializedName("weight")
    private float weight = 1.0f;
    
    private transient ResourceLocation registryName;
    
    public List<BlockEntry> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockEntry> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public float getWeight() {
        return weight;
    }
    
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * 是否使用旧格式（name/weight）
     */
    public boolean isLegacyFormat() {
        return (blocks == null || blocks.isEmpty()) && name != null && !name.isBlank();
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
