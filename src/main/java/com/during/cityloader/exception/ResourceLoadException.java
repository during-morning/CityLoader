package com.during.cityloader.exception;

/**
 * 资源加载相关的异常
 * 当调色板、部件或建筑资源加载失败时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class ResourceLoadException extends SCGException {
    
    /**
     * 构造一个新的ResourceLoadException
     * 
     * @param message 异常消息
     */
    public ResourceLoadException(String message) {
        super(message);
    }
    
    /**
     * 构造一个新的ResourceLoadException，包含原因
     * 
     * @param message 异常消息
     * @param cause 导致此异常的原因
     */
    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
