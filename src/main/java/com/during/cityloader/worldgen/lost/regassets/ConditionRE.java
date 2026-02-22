package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.ConditionPart;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件注册实体
 * 用于从JSON反序列化条件数据
 * 
 * @author During
 * @since 1.4.0
 */
public class ConditionRE implements IAsset {
    
    @SerializedName("values")
    private List<ConditionPart> values = new ArrayList<>();
    
    private transient ResourceLocation registryName;
    
    public List<ConditionPart> getValues() {
        return values;
    }
    
    public void setValues(List<ConditionPart> values) {
        this.values = values;
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
