package com.during.cityloader.worldgen.lost;

/**
 * 城市建筑接口
 * 定义建筑的基本属性
 * 
 * @author During
 * @since 1.4.0
 */
public interface ILostCityBuilding {
    
    /**
     * 获取建筑名称
     * 
     * @return 建筑名称
     */
    String getName();
    
    /**
     * 获取最小楼层数
     * 
     * @return 最小楼层数，-1表示使用默认值
     */
    int getMinFloors();
    
    /**
     * 获取最大楼层数
     * 
     * @return 最大楼层数，-1表示使用默认值
     */
    int getMaxFloors();
    
    /**
     * 获取最小地下室数
     * 
     * @return 最小地下室数，-1表示使用默认值
     */
    int getMinCellars();
    
    /**
     * 获取最大地下室数
     * 
     * @return 最大地下室数，-1表示使用默认值
     */
    int getMaxCellars();
}
