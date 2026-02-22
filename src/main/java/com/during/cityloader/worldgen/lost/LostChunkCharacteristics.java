package com.during.cityloader.worldgen.lost;

import com.during.cityloader.worldgen.lost.cityassets.CityStyle;

/**
 * 区块特征数据类
 * 存储区块的城市生成特征信息
 * 
 * @author During
 * @since 1.4.0
 */
public class LostChunkCharacteristics {
    
    /**
     * 是否为城市区块
     */
    public boolean isCity;
    
    /**
     * 是否可能有建筑
     */
    public boolean couldHaveBuilding;
    
    /**
     * 城市层级
     */
    public int cityLevel;
    
    /**
     * 多区块建筑位置
     */
    public MultiPos multiPos;
    
    /**
     * 多区块建筑类型
     */
    public ILostCityMultiBuilding multiBuilding;
    
    /**
     * 建筑类型
     */
    public ILostCityBuilding buildingType;
    
    /**
     * 城市风格
     */
    public CityStyle cityStyle;
    
    /**
     * 构造区块特征
     */
    public LostChunkCharacteristics() {
        this.isCity = false;
        this.couldHaveBuilding = false;
        this.cityLevel = 0;
        this.multiPos = null;
        this.multiBuilding = null;
        this.buildingType = null;
        this.cityStyle = null;
    }
}
