package com.during.cityloader.exception;

/**
 * 无效资产异常
 * 当资产数据不符合预期格式或包含无效值时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class InvalidAssetException extends RuntimeException {
    
    private final String assetName;
    private final String fieldName;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param assetName 资产名称
     */
    public InvalidAssetException(String message, String assetName) {
        super(String.format("资产数据无效 [%s]: %s", assetName, message));
        this.assetName = assetName;
        this.fieldName = null;
    }
    
    /**
     * 构造函数（带字段名）
     * 
     * @param message 错误消息
     * @param assetName 资产名称
     * @param fieldName 字段名
     */
    public InvalidAssetException(String message, String assetName, String fieldName) {
        super(String.format("资产数据无效 [%s.%s]: %s", assetName, fieldName, message));
        this.assetName = assetName;
        this.fieldName = fieldName;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param message 错误消息
     * @param assetName 资产名称
     * @param cause 原因
     */
    public InvalidAssetException(String message, String assetName, Throwable cause) {
        super(String.format("资产数据无效 [%s]: %s", assetName, message), cause);
        this.assetName = assetName;
        this.fieldName = null;
    }
    
    /**
     * 获取资产名称
     * 
     * @return 资产名称
     */
    public String getAssetName() {
        return assetName;
    }
    
    /**
     * 获取字段名
     * 
     * @return 字段名，如果未指定则返回null
     */
    public String getFieldName() {
        return fieldName;
    }
}
