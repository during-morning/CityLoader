package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;

/**
 * 资产注册实体接口
 * 用于JSON反序列化的资产基础接口
 * 
 * @author During
 * @since 1.4.0
 */
public interface IAsset {
    
    /**
     * 获取注册名称
     * 
     * @return 资源位置
     */
    ResourceLocation getRegistryName();
    
    /**
     * 设置注册名称
     * 
     * @param name 资源位置
     */
    void setRegistryName(ResourceLocation name);
}
