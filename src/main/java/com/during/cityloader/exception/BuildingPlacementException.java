package com.during.cityloader.exception;

/**
 * 建筑放置异常
 * 当建筑放置过程中发生错误时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class BuildingPlacementException extends RuntimeException {
    
    private final String buildingName;
    private final int x;
    private final int y;
    private final int z;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param buildingName 建筑名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    public BuildingPlacementException(String message, String buildingName, int x, int y, int z) {
        super(String.format("建筑放置失败 [%s @ %d,%d,%d]: %s", buildingName, x, y, z, message));
        this.buildingName = buildingName;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param message 错误消息
     * @param buildingName 建筑名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param cause 原因
     */
    public BuildingPlacementException(String message, String buildingName, int x, int y, int z, Throwable cause) {
        super(String.format("建筑放置失败 [%s @ %d,%d,%d]: %s", buildingName, x, y, z, message), cause);
        this.buildingName = buildingName;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 获取建筑名称
     * 
     * @return 建筑名称
     */
    public String getBuildingName() {
        return buildingName;
    }
    
    /**
     * 获取X坐标
     * 
     * @return X坐标
     */
    public int getX() {
        return x;
    }
    
    /**
     * 获取Y坐标
     * 
     * @return Y坐标
     */
    public int getY() {
        return y;
    }
    
    /**
     * 获取Z坐标
     * 
     * @return Z坐标
     */
    public int getZ() {
        return z;
    }
}
