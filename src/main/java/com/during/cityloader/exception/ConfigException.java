package com.during.cityloader.exception;

/**
 * 配置相关的异常
 * 当配置文件加载、解析或验证失败时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class ConfigException extends SCGException {
    
    /**
     * 构造一个新的ConfigException
     * 
     * @param message 异常消息
     */
    public ConfigException(String message) {
        super(message);
    }
    
    /**
     * 构造一个新的ConfigException，包含原因
     * 
     * @param message 异常消息
     * @param cause 导致此异常的原因
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
