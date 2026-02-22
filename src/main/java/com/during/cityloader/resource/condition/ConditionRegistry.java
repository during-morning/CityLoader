package com.during.cityloader.resource.condition;

import com.during.cityloader.resource.registry.NamespacedRegistry;

import java.util.Collection;

/**
 * 条件注册表
 * 管理所有Condition的注册和查询，支持命名空间隔离
 * 
 * @author During
 * @since 1.5.0
 */
public class ConditionRegistry extends NamespacedRegistry<Condition<?>> {

    /**
     * 构造函数
     */
    public ConditionRegistry() {
        super(false); // 不需要线程安全
    }

    /**
     * 注册一个条件
     * 
     * @param condition 要注册的条件
     * @throws IllegalArgumentException 如果条件ID已存在或条件无效
     */
    public void register(Condition<?> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("条件不能为null");
        }

        if (!condition.validate()) {
            throw new IllegalArgumentException("条件无效: " + condition.getId());
        }

        // 使用父类的register方法
        register(condition.getId(), condition);
    }

    /**
     * 根据ID获取条件（带类型转换）
     * 
     * @param id  条件ID
     * @param <T> 值的类型
     * @return 对应的条件，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> Condition<T> getTyped(String id) {
        return (Condition<T>) get(id);
    }

    @Override
    protected String getRegistryName() {
        return "条件注册表";
    }

    /**
     * 获取注册表大小（别名方法，保持向后兼容）
     * 
     * @return 注册的条件数量
     */
    public int getCount() {
        return size();
    }

    /**
     * 获取统计信息（增强版）
     * 
     * @return 统计信息字符串
     */
    @Override
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 条件注册表统计 ===\n");
        stats.append("总条件数: ").append(size()).append("\n");
        stats.append("命名空间数: ").append(getNamespaces().size()).append("\n\n");

        for (String namespace : getNamespaces()) {
            stats.append("命名空间 [").append(namespace).append("]:\n");
            Collection<Condition<?>> conditions = getAllFrom(namespace);
            for (Condition<?> condition : conditions) {
                stats.append("  ").append(condition.getStatistics()).append("\n");
            }
        }

        return stats.toString();
    }
}
