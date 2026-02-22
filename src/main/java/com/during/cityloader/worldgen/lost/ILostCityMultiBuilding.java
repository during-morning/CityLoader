package com.during.cityloader.worldgen.lost;

/**
 * 多区块建筑接口
 * 定义多区块建筑的基本属性
 * 
 * @author During
 * @since 1.4.0
 */
public interface ILostCityMultiBuilding {
    
    /**
     * 获取建筑名称
     * 
     * @return 建筑名称
     */
    String getName();
    
    /**
     * 获取宽度（区块数）
     * 
     * @return 宽度
     */
    int getDimX();
    
    /**
     * 获取高度（区块数）
     * 
     * @return 高度
     */
    int getDimZ();
}
