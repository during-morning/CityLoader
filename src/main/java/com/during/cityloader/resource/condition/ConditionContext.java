package com.during.cityloader.resource.condition;

/**
 * 条件上下文
 * 提供条件测试所需的所有信息
 * 
 * @author During
 * @since 1.5.0
 */
public class ConditionContext {

    private final int level; // 绝对层级（世界Y坐标）
    private final int floor; // 相对楼层（相对于建筑）
    private final int cellars; // 地下室层数
    private final int floors; // 总楼层数
    private final String partName; // 当前部件名称
    private final String belowPartName; // 下方部件名称
    private final String buildingName; // 建筑名称
    private final String biome; // 生物群系

    /**
     * 构造函数
     * 
     * @param level         绝对层级
     * @param floor         相对楼层
     * @param cellars       地下室层数
     * @param floors        总楼层数
     * @param partName      当前部件名称
     * @param belowPartName 下方部件名称
     * @param buildingName  建筑名称
     * @param biome         生物群系
     */
    public ConditionContext(int level, int floor, int cellars, int floors,
            String partName, String belowPartName,
            String buildingName, String biome) {
        this.level = level;
        this.floor = floor;
        this.cellars = cellars;
        this.floors = floors;
        this.partName = partName;
        this.belowPartName = belowPartName;
        this.buildingName = buildingName;
        this.biome = biome;
    }

    /**
     * 静态工厂方法：创建建筑上下文
     * 
     * @param level         绝对层级
     * @param floor         相对楼层
     * @param cellars       地下室层数
     * @param floors        总楼层数
     * @param partName      部件名称
     * @param belowPartName 下方部件名称
     * @param buildingName  建筑名称
     * @return 条件上下文
     */
    public static ConditionContext forBuilding(int level, int floor, int cellars,
            int floors, String partName,
            String belowPartName, String buildingName) {
        return new ConditionContext(level, floor, cellars, floors,
                partName, belowPartName, buildingName, null);
    }

    /**
     * 是否为楼顶
     * 
     * @return 如果是楼顶返回true
     */
    public boolean isTop() {
        return floor == floors - 1;
    }

    /**
     * 是否为地面层
     * 
     * @return 如果是地面返回true
     */
    public boolean isGround() {
        return floor == 0;
    }

    /**
     * 是否为地下室
     * 
     * @return 如果是地下室返回true
     */
    public boolean isCellar() {
        return floor < 0;
    }

    /**
     * 是否为建筑（非街道）
     * 
     * @return 如果是建筑返回true
     */
    public boolean isBuilding() {
        return buildingName != null && !buildingName.isEmpty();
    }

    // Getter方法

    public int getLevel() {
        return level;
    }

    public int getFloor() {
        return floor;
    }

    public int getCellars() {
        return cellars;
    }

    public int getFloors() {
        return floors;
    }

    public String getPartName() {
        return partName;
    }

    public String getBelowPartName() {
        return belowPartName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getBiome() {
        return biome;
    }
}
