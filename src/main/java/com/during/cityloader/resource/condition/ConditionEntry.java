package com.during.cityloader.resource.condition;

/**
 * 条件条目
 * 表示单个条件化的值及其条件参数
 * 
 * @param <T> 值的类型（通常是String）
 * @author During
 * @since 1.5.0
 */
public class ConditionEntry<T> {

    private final double factor; // 权重因子
    private final T value; // 值（战利品表ID或实体类型）

    // 可选条件参数
    private final Boolean top; // 是否为楼顶
    private final Boolean ground; // 是否为地面
    private final Boolean cellar; // 是否为地下室
    private final String range; // 楼层范围 "min,max"
    private final String inpart; // 在特定部件内
    private final String inbiome; // 在特定生物群系

    /**
     * 完整构造函数
     * 
     * @param factor  权重因子
     * @param value   值
     * @param top     是否为楼顶
     * @param ground  是否为地面
     * @param cellar  是否为地下室
     * @param range   楼层范围
     * @param inpart  在特定部件内
     * @param inbiome 在特定生物群系
     */
    public ConditionEntry(double factor, T value, Boolean top, Boolean ground,
            Boolean cellar, String range, String inpart, String inbiome) {
        this.factor = factor;
        this.value = value;
        this.top = top;
        this.ground = ground;
        this.cellar = cellar;
        this.range = range;
        this.inpart = inpart;
        this.inbiome = inbiome;
    }

    /**
     * 简化构造函数（无条件）
     * 
     * @param factor 权重因子
     * @param value  值
     */
    public ConditionEntry(double factor, T value) {
        this(factor, value, null, null, null, null, null, null);
    }

    /**
     * 测试是否满足条件
     * 
     * @param context 条件上下文
     * @return 如果满足所有条件返回true
     */
    public boolean test(ConditionContext context) {
        // 测试楼顶条件
        if (top != null && top != context.isTop()) {
            return false;
        }

        // 测试地面条件
        if (ground != null && ground != context.isGround()) {
            return false;
        }

        // 测试地下室条件
        if (cellar != null && cellar != context.isCellar()) {
            return false;
        }

        // 测试楼层范围
        if (range != null) {
            String[] parts = range.split(",");
            if (parts.length != 2) {
                return false;
            }

            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                int floor = context.getFloor();

                if (floor < min || floor > max) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 测试部件条件
        if (inpart != null) {
            String partName = context.getPartName();
            if (partName == null || !partName.equals(inpart)) {
                return false;
            }
        }

        // 测试生物群系条件（简化实现）
        if (inbiome != null) {
            String biome = context.getBiome();
            if (biome == null || !biome.equals(inbiome)) {
                // TODO: 支持BiomeTag (以#开头的标签)
                return false;
            }
        }

        return true;
    }

    /**
     * 获取权重因子
     * 
     * @return 权重因子
     */
    public double getFactor() {
        return factor;
    }

    /**
     * 获取值
     * 
     * @return 值
     */
    public T getValue() {
        return value;
    }

    // Getter方法（用于测试）

    public Boolean getTop() {
        return top;
    }

    public Boolean getGround() {
        return ground;
    }

    public Boolean getCellar() {
        return cellar;
    }

    public String getRange() {
        return range;
    }

    public String getInpart() {
        return inpart;
    }

    public String getInbiome() {
        return inbiome;
    }
}
