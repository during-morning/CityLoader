package com.during.cityloader.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 建筑类
 * 表示一个完整的建筑，由多个部件组成
 * 
 * @author During
 * @since 1.4.0
 */
public class Building {

    // 建筑ID（移除final以便后续设置）
    private String id;

    // 建筑类型（如residential, commercial, industrial）
    private final String type;

    // 建筑部件列表
    private final List<BuildingPart> parts;

    // 权重（用于随机选择）
    private final double weight;

    // 楼层数范围
    private final int minFloors;
    private final int maxFloors;

    // 调色板ID（用于方块材质映射）
    private final String palette;

    /**
     * 无参构造函数（用于Gson）
     */
    public Building() {
        this.id = null;
        this.type = "building";
        this.parts = new ArrayList<>();
        this.weight = 1.0;
        this.minFloors = 1;
        this.maxFloors = 1;
        this.palette = "default";
    }

    /**
     * 构造函数
     * 
     * @param id        建筑ID
     * @param type      建筑类型
     * @param parts     部件列表
     * @param weight    权重
     * @param minFloors 最小楼层数
     * @param maxFloors 最大楼层数
     * @param palette   调色板ID
     */
    public Building(String id, String type, List<BuildingPart> parts, double weight,
            int minFloors, int maxFloors, String palette) {
        this.id = id;
        this.type = type;
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
        this.weight = weight;
        this.minFloors = minFloors;
        this.maxFloors = maxFloors;
        this.palette = palette != null ? palette : "standard"; // 默认使用standard调色板
    }

    /**
     * 根据楼层数选择要使用的部件
     * 
     * @param floors 楼层数
     * @return 满足条件的部件列表
     */
    public List<BuildingPart> selectParts(int floors) {
        List<BuildingPart> selectedParts = new ArrayList<>();

        for (BuildingPart part : parts) {
            BuildingPart.BuildingContext context = new BuildingPart.BuildingContext(0, floors);

            // 检查部件条件
            if (part.checkCondition(context)) {
                selectedParts.add(part);
            }
        }

        return selectedParts;
    }

    /**
     * 根据楼层数和当前楼层选择部件
     * 
     * @param currentFloor 当前楼层
     * @param totalFloors  总楼层数
     * @return 满足条件的部件列表
     */
    public List<BuildingPart> selectPartsForFloor(int currentFloor, int totalFloors) {
        List<BuildingPart> selectedParts = new ArrayList<>();

        BuildingPart.BuildingContext context = new BuildingPart.BuildingContext(currentFloor, totalFloors);

        for (BuildingPart part : parts) {
            if (part.checkCondition(context)) {
                selectedParts.add(part);
            }
        }

        return selectedParts;
    }

    /**
     * 随机生成楼层数
     * 
     * @param random 随机数生成器
     * @return 楼层数
     */
    public int generateFloorCount(Random random) {
        if (minFloors == maxFloors) {
            return minFloors;
        }
        return minFloors + random.nextInt(maxFloors - minFloors + 1);
    }

    /**
     * 验证建筑的有效性
     * 
     * @return 如果有效返回true
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) {
            return false;
        }

        if (type == null || type.isEmpty()) {
            return false;
        }

        if (minFloors < 1 || maxFloors < minFloors) {
            return false;
        }

        if (weight < 0) {
            return false;
        }

        if (parts == null || parts.isEmpty()) {
            return false;
        }

        return true;
    }

    // Getter方法

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<BuildingPart> getParts() {
        return new ArrayList<>(parts);
    }

    public double getWeight() {
        return weight;
    }

    public int getMinFloors() {
        return minFloors;
    }

    public int getMaxFloors() {
        return maxFloors;
    }

    public String getPalette() {
        return palette;
    }

    /**
     * 测试建筑是否满足给定的条件上下文。
     *
     * @param context 条件上下文
     * @return 如果建筑满足条件返回 true，否则返回 false
     */
    public boolean testCondition(com.during.cityloader.resource.condition.ConditionContext context) {
        // 如果没有条件上下文，默认通过
        if (context == null) {
            return true;
        }

        // TODO: 未来可以为 Building 添加 condition 字段
        // 目前简单实现：所有建筑都通过条件测试
        // 条件过滤主要在 BuildingPart 层面进行

        // 可以在这里添加建筑级别的条件检查，例如：
        // - 检查生物群系是否适合此建筑类型
        // - 检查楼层数是否在合理范围内
        // - 检查是否满足特定的环境条件

        return true;
    }

    /**
     * 获取部件数量
     * 
     * @return 部件数量
     */
    public int getPartCount() {
        return parts.size();
    }

    /**
     * 获取指定类型的部件
     * 
     * @param partId 部件ID
     * @return 部件列表
     */
    public List<BuildingPart> getPartsByType(String partId) {
        return parts.stream()
                .filter(part -> part.getPartId().equals(partId))
                .collect(Collectors.toList());
    }
}
