package com.during.cityloader.exception;

/**
 * 资产解析异常
 * 当JSON资产文件解析失败时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class AssetParseException extends RuntimeException {
    
    private final String filePath;
    private final int lineNumber;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param filePath 文件路径
     */
    public AssetParseException(String message, String filePath) {
        super(String.format("解析资产文件失败 [%s]: %s", filePath, message));
        this.filePath = filePath;
        this.lineNumber = -1;
    }
    
    /**
     * 构造函数（带行号）
     * 
     * @param message 错误消息
     * @param filePath 文件路径
     * @param lineNumber 行号
     */
    public AssetParseException(String message, String filePath, int lineNumber) {
        super(String.format("解析资产文件失败 [%s:%d]: %s", filePath, lineNumber, message));
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param message 错误消息
     * @param filePath 文件路径
     * @param cause 原因
     */
    public AssetParseException(String message, String filePath, Throwable cause) {
        super(String.format("解析资产文件失败 [%s]: %s", filePath, message), cause);
        this.filePath = filePath;
        this.lineNumber = -1;
    }
    
    /**
     * 构造函数（带行号和原因）
     * 
     * @param message 错误消息
     * @param filePath 文件路径
     * @param lineNumber 行号
     * @param cause 原因
     */
    public AssetParseException(String message, String filePath, int lineNumber, Throwable cause) {
        super(String.format("解析资产文件失败 [%s:%d]: %s", filePath, lineNumber, message), cause);
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }
    
    /**
     * 获取文件路径
     * 
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 获取行号
     * 
     * @return 行号，如果未知则返回-1
     */
    public int getLineNumber() {
        return lineNumber;
    }
}
