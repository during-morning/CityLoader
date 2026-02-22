package com.during.cityloader.exception;

/**
 * CityLoader插件的基础异常类
 * 所有自定义异常都应该继承此类
 * 
 * @author During
 * @since 1.4.0
 */
public class SCGException extends Exception {
    
    /**
     * 构造一个新的SCGException
     * 
     * @param message 异常消息
     */
    public SCGException(String message) {
        super(message);
    }
    
    /**
     * 构造一个新的SCGException，包含原因
     * 
     * @param message 异常消息
     * @param cause 导致此异常的原因
     */
    public SCGException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造一个新的SCGException，仅包含原因
     * 
     * @param cause 导致此异常的原因
     */
    public SCGException(Throwable cause) {
        super(cause);
    }
}
