package com.during.cityloader.exception;

/**
 * 城市生成相关的异常
 * 当区块生成、建筑放置或结构生成失败时抛出
 * 
 * @author During
 * @since 1.4.0
 */
public class GenerationException extends SCGException {
    
    /**
     * 构造一个新的GenerationException
     * 
     * @param message 异常消息
     */
    public GenerationException(String message) {
        super(message);
    }
    
    /**
     * 构造一个新的GenerationException，包含原因
     * 
     * @param message 异常消息
     * @param cause 导致此异常的原因
     */
    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
