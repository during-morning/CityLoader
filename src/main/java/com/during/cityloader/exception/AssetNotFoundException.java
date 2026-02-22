package com.during.cityloader.exception;

/**
 * 资产未找到异常
 * 当请求的资产（调色板、部件、建筑等）不存在时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class AssetNotFoundException extends RuntimeException {
    
    private final String assetType;
    private final String assetName;
    
    /**
     * 构造函数
     * 
     * @param assetType 资产类型（如"palette", "building", "part"）
     * @param assetName 资产名称
     */
    public AssetNotFoundException(String assetType, String assetName) {
        super(String.format("未找到%s资产: %s", assetType, assetName));
        this.assetType = assetType;
        this.assetName = assetName;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param assetType 资产类型
     * @param assetName 资产名称
     * @param cause 原因
     */
    public AssetNotFoundException(String assetType, String assetName, Throwable cause) {
        super(String.format("未找到%s资产: %s", assetType, assetName), cause);
        this.assetType = assetType;
        this.assetName = assetName;
    }
    
    /**
     * 获取资产类型
     * 
     * @return 资产类型
     */
    public String getAssetType() {
        return assetType;
    }
    
    /**
     * 获取资产名称
     * 
     * @return 资产名称
     */
    public String getAssetName() {
        return assetName;
    }
}
