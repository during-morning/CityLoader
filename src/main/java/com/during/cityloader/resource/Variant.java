package com.during.cityloader.resource;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 方块变体类
 * 定义加权随机方块列表，用于创建混合材质效果
 * 
 * 示例：混合砖块墙（石砖:5, 裂纹石砖:2, 苔石砖:1）
 * 
 * @author During
 * @since 1.5.0
 */
public class Variant {

    /**
     * 加权方块条目
     */
    public static class WeightedBlock {
        private final Material material;
        private final int weight;

        public WeightedBlock(Material material, int weight) {
            this.material = material;
            this.weight = weight;
        }

        public Material getMaterial() {
            return material;
        }

        public int getWeight() {
            return weight;
        }
    }

    // 变体ID
    private final String id;

    // 加权方块列表
    private final List<WeightedBlock> blocks;

    // 总权重（缓存）
    private final int totalWeight;

    /**
     * 构造函数
     * 
     * @param id 变体ID
     */
    public Variant(String id) {
        this.id = id;
        this.blocks = new ArrayList<>();
        this.totalWeight = 0;
    }

    /**
     * 构造函数（带方块列表）
     * 
     * @param id     变体ID
     * @param blocks 加权方块列表
     */
    public Variant(String id, List<WeightedBlock> blocks) {
        this.id = id;
        this.blocks = new ArrayList<>(blocks);

        // 计算总权重
        int total = 0;
        for (WeightedBlock block : blocks) {
            total += block.getWeight();
        }
        this.totalWeight = total;
    }

    /**
     * 添加加权方块
     * 
     * @param material 方块材质
     * @param weight   权重
     */
    public void addBlock(Material material, int weight) {
        blocks.add(new WeightedBlock(material, weight));
    }

    /**
     * 根据权重随机选择一个方块
     * 
     * 算法：
     * 1. 生成 [0, totalWeight) 范围内的随机数
     * 2. 遍历方块列表，累加权重
     * 3. 当累加权重 >= 随机数时，返回该方块
     * 
     * @param random 随机数生成器
     * @return 随机选择的方块材质
     */
    public Material getRandomBlock(Random random) {
        if (blocks.isEmpty()) {
            return Material.AIR;
        }

        if (blocks.size() == 1) {
            return blocks.get(0).getMaterial();
        }

        // 生成随机数
        int randomWeight = random.nextInt(getTotalWeight());

        // 遍历方块列表，找到对应的方块
        int currentWeight = 0;
        for (WeightedBlock block : blocks) {
            currentWeight += block.getWeight();
            if (currentWeight > randomWeight) {
                return block.getMaterial();
            }
        }

        // 理论上不会到达这里，但为了安全起见返回最后一个方块
        return blocks.get(blocks.size() - 1).getMaterial();
    }

    /**
     * 获取总权重
     * 如果totalWeight未初始化（使用无参构造函数），则动态计算
     * 
     * @return 总权重
     */
    public int getTotalWeight() {
        if (totalWeight > 0) {
            return totalWeight;
        }

        int total = 0;
        for (WeightedBlock block : blocks) {
            total += block.getWeight();
        }
        return total;
    }

    /**
     * 验证变体的有效性
     * 
     * @return 如果有效返回true
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) {
            return false;
        }

        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        // 检查所有方块材质是否有效
        for (WeightedBlock block : blocks) {
            if (block.getMaterial() == null) {
                return false;
            }
            if (block.getWeight() <= 0) {
                return false;
            }
        }

        return true;
    }

    // Getter方法

    public String getId() {
        return id;
    }

    public List<WeightedBlock> getBlocks() {
        return new ArrayList<>(blocks);
    }

    /**
     * 获取方块数量
     * 
     * @return 方块数量
     */
    public int getBlockCount() {
        return blocks.size();
    }

    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Variant: ").append(id).append("\n");
        stats.append("Total Weight: ").append(getTotalWeight()).append("\n");
        stats.append("Blocks:\n");

        for (WeightedBlock block : blocks) {
            double percentage = (double) block.getWeight() / getTotalWeight() * 100;
            stats.append("  - ").append(block.getMaterial().name())
                    .append(": ").append(block.getWeight())
                    .append(" (").append(String.format("%.1f", percentage)).append("%)\n");
        }

        return stats.toString();
    }
}
