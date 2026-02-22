package com.during.cityloader.season;

/**
 * 季节枚举
 * 定义四个季节：春、夏、秋、冬
 * 
 * @author During
 * @since 1.4.0
 */
public enum Season {
    
    /**
     * 春季
     */
    SPRING("春季"),
    
    /**
     * 夏季
     */
    SUMMER("夏季"),
    
    /**
     * 秋季
     */
    AUTUMN("秋季"),
    
    /**
     * 冬季
     */
    WINTER("冬季");
    
    // 季节的显示名称
    private final String displayName;
    
    /**
     * 构造函数
     * 
     * @param displayName 显示名称
     */
    Season(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * 获取季节的显示名称
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 从字符串解析季节
     * 
     * @param name 季节名称
     * @return 对应的Season枚举，如果无法解析则返回SPRING
     */
    public static Season fromString(String name) {
        if (name == null) {
            return SPRING;
        }
        
        try {
            return Season.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SPRING;
        }
    }
}
