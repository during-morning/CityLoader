package com.during.cityloader.resource;

/**
 * 建筑部件类
 * 表示建筑中使用的一个部件及其位置和条件
 * 
 * @author During
 * @since 1.4.0
 */
public class BuildingPart {
    
    // 部件ID
    private final String partId;
    
    // 偏移量
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    
    // 条件（可选，用于条件性放置）
    private final String condition;
    
    /**
     * 构造函数
     * 
     * @param partId 部件ID
     * @param offsetX X轴偏移
     * @param offsetY Y轴偏移
     * @param offsetZ Z轴偏移
     * @param condition 放置条件
     */
    public BuildingPart(String partId, int offsetX, int offsetY, int offsetZ, String condition) {
        this.partId = partId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.condition = condition;
    }
    
    /**
     * 简化构造函数（无条件）
     * 
     * @param partId 部件ID
     * @param offsetX X轴偏移
     * @param offsetY Y轴偏移
     * @param offsetZ Z轴偏移
     */
    public BuildingPart(String partId, int offsetX, int offsetY, int offsetZ) {
        this(partId, offsetX, offsetY, offsetZ, null);
    }
    
    /**
     * 检查条件是否满足
     * 
     * @param context 上下文信息（如楼层数等）
     * @return 如果条件满足或无条件返回true
     */
    public boolean checkCondition(BuildingContext context) {
        if (condition == null || condition.isEmpty()) {
            return true; // 无条件，总是满足
        }
        
        // 简化的条件检查实现
        // 实际应用中可以实现更复杂的条件表达式解析
        try {
            // 示例条件格式: "floor>5", "floor<=10", "floor==1"
            if (condition.contains(">")) {
                String[] parts = condition.split(">");
                if (parts[0].trim().equals("floor")) {
                    int threshold = Integer.parseInt(parts[1].trim());
                    return context.getCurrentFloor() > threshold;
                }
            } else if (condition.contains("<")) {
                String[] parts = condition.split("<");
                if (parts[0].trim().equals("floor")) {
                    int threshold = Integer.parseInt(parts[1].trim());
                    return context.getCurrentFloor() < threshold;
                }
            } else if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts[0].trim().equals("floor")) {
                    int value = Integer.parseInt(parts[1].trim());
                    return context.getCurrentFloor() == value;
                }
            }
        } catch (Exception e) {
            // 条件解析失败，默认返回true
            return true;
        }
        
        return true;
    }
    
    // Getter方法
    
    public String getPartId() {
        return partId;
    }
    
    public int getOffsetX() {
        return offsetX;
    }
    
    public int getOffsetY() {
        return offsetY;
    }
    
    public int getOffsetZ() {
        return offsetZ;
    }
    
    public String getCondition() {
        return condition;
    }
    
    /**
     * 建筑上下文类
     * 用于条件检查
     */
    public static class BuildingContext {
        private final int currentFloor;
        private final int totalFloors;
        
        public BuildingContext(int currentFloor, int totalFloors) {
            this.currentFloor = currentFloor;
            this.totalFloors = totalFloors;
        }
        
        public int getCurrentFloor() {
            return currentFloor;
        }
        
        public int getTotalFloors() {
            return totalFloors;
        }
    }
}
