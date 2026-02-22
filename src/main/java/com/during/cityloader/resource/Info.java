package com.during.cityloader.resource;

/**
 * 方块元数据记录
 * 用于存储战利品箱、刷怪笼、火把等特殊方块的附加信息
 * 
 * @author During
 * @since 1.5.0
 */
public class Info {

    private final String mobId; // 刷怪笼实体ID或Condition引用（如 "easymobs"）
    private final String loot; // 战利品表Condition引用（如 "chestloot"）
    private final boolean isTorch; // 是否为火把（需要后处理）
    private final String tag; // 自定义NBT数据（JSON字符串）

    /**
     * 完整构造函数
     * 
     * @param mobId   刷怪笼实体ID或Condition引用
     * @param loot    战利品表Condition引用
     * @param isTorch 是否为火把
     * @param tag     自定义NBT数据（JSON字符串）
     */
    public Info(String mobId, String loot, boolean isTorch, String tag) {
        this.mobId = mobId;
        this.loot = loot;
        this.isTorch = isTorch;
        this.tag = tag;
    }

    /**
     * 简化构造函数（无NBT）
     * 
     * @param mobId   刷怪笼实体ID或Condition引用
     * @param loot    战利品表Condition引用
     * @param isTorch 是否为火把
     */
    public Info(String mobId, String loot, boolean isTorch) {
        this(mobId, loot, isTorch, null);
    }

    /**
     * 检查是否有任何元数据
     * 
     * @return 如果有任何字段非空返回true
     */
    public boolean hasMetadata() {
        return mobId != null || loot != null || isTorch || tag != null;
    }

    /**
     * 检查是否为刷怪笼
     * 
     * @return 如果有mobId返回true
     */
    public boolean isSpawner() {
        return mobId != null;
    }

    /**
     * 检查是否为战利品箱
     * 
     * @return 如果有loot返回true
     */
    public boolean isLootContainer() {
        return loot != null;
    }

    // Getter方法

    public String getMobId() {
        return mobId;
    }

    public String getLoot() {
        return loot;
    }

    public boolean isTorch() {
        return isTorch;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Info{");
        boolean first = true;

        if (mobId != null) {
            sb.append("mobId='").append(mobId).append("'");
            first = false;
        }
        if (loot != null) {
            if (!first)
                sb.append(", ");
            sb.append("loot='").append(loot).append("'");
            first = false;
        }
        if (isTorch) {
            if (!first)
                sb.append(", ");
            sb.append("isTorch=true");
            first = false;
        }
        if (tag != null) {
            if (!first)
                sb.append(", ");
            sb.append("tag='").append(tag).append("'");
        }

        sb.append("}");
        return sb.toString();
    }
}
