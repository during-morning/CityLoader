package com.during.cityloader.resource.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 条件列表容器
 * 管理一组条件化的值及其权重，支持基于上下文的加权随机选择
 * 
 * @param <T> 值的类型（通常是String）
 * @author During
 * @since 1.5.0
 */
public class Condition<T> {

    private final String id; // 条件ID (如 "chestloot")
    private final List<ConditionEntry<T>> entries; // 条件条目列表

    /**
     * 构造函数
     * 
     * @param id      条件ID
     * @param entries 条件条目列表
     */
    public Condition(String id, List<ConditionEntry<T>> entries) {
        this.id = id;
        this.entries = new ArrayList<>(entries);
    }

    /**
     * 根据上下文获取随机值
     * 
     * @param random  随机数生成器
     * @param context 条件上下文
     * @return 随机选择的值，如果没有满足条件的条目返回null
     */
    public T getRandomValue(Random random, ConditionContext context) {
        List<ConditionEntry<T>> matching = getMatchingEntries(context);

        if (matching.isEmpty()) {
            return null;
        }

        // 计算总权重
        double totalWeight = matching.stream()
                .mapToDouble(ConditionEntry::getFactor)
                .sum();

        if (totalWeight <= 0) {
            return null;
        }

        // 加权随机选择
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;

        for (ConditionEntry<T> entry : matching) {
            currentWeight += entry.getFactor();
            if (randomValue <= currentWeight) {
                return entry.getValue();
            }
        }

        // 备用：返回最后一个匹配的条目
        return matching.get(matching.size() - 1).getValue();
    }

    /**
     * 获取所有匹配条件的条目
     * 
     * @param context 条件上下文
     * @return 匹配的条目列表
     */
    public List<ConditionEntry<T>> getMatchingEntries(ConditionContext context) {
        List<ConditionEntry<T>> matching = new ArrayList<>();

        for (ConditionEntry<T> entry : entries) {
            if (entry.test(context)) {
                matching.add(entry);
            }
        }

        return matching;
    }

    /**
     * 验证条件有效性
     * 
     * @return 如果条件有效返回true
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) {
            return false;
        }

        if (entries == null || entries.isEmpty()) {
            return false;
        }

        // 验证所有条目的权重为正数
        for (ConditionEntry<T> entry : entries) {
            if (entry.getFactor() <= 0) {
                return false;
            }
            if (entry.getValue() == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取条目数量
     * 
     * @return 条目数量
     */
    public int size() {
        return entries.size();
    }

    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("条件ID: ").append(id).append("\n");
        stats.append("条目数: ").append(entries.size()).append("\n");

        double totalWeight = entries.stream()
                .mapToDouble(ConditionEntry::getFactor)
                .sum();
        stats.append("总权重: ").append(String.format("%.1f", totalWeight)).append("\n");

        return stats.toString();
    }

    // Getter方法

    public String getId() {
        return id;
    }

    public List<ConditionEntry<T>> getEntries() {
        return new ArrayList<>(entries);
    }
}
