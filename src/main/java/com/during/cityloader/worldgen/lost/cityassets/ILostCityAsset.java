package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;

/**
 * 失落城市资产接口
 * 所有城市生成资产的基础接口
 * 
 * @author During
 * @since 1.4.0
 */
public interface ILostCityAsset {
    
    /**
     * 获取资产的名称（字符串形式）
     * 
     * @return 资产名称
     */
    String getName();
    
    /**
     * 获取资产的资源位置ID
     * 
     * @return 资源位置
     */
    ResourceLocation getId();
}
