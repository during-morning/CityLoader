package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.Variant;
import com.during.cityloader.util.ResourceLocation;

import java.util.Collection;

/**
 * 方块变体注册表
 * 管理所有方块变体的注册和查询，支持命名空间隔离
 * 线程安全版本
 * 
 * @author During
 * @since 1.5.0
 */
public class VariantRegistry extends NamespacedRegistry<Variant> {

    /**
     * 构造函数 - 线程安全版本
     */
    public VariantRegistry() {
        super(true); // 启用线程安全
    }

    /**
     * 注册一个变体
     * 
     * @param variant 要注册的变体
     * @throws IllegalArgumentException 如果变体ID已存在或变体无效
     */
    public void register(Variant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("变体不能为null");
        }

        if (!variant.validate()) {
            throw new IllegalArgumentException("变体无效: " + variant.getId());
        }

        // 使用父类的register方法
        register(variant.getId(), variant);
    }

    @Override
    protected String getRegistryName() {
        return "变体注册表";
    }

    /**
     * 获取所有变体（别名方法，保持向后兼容）
     * 
     * @return 所有变体的集合
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
        stats.append("=== 变体注册表统计 ===\n");
        stats.append("总变体数: ").append(size()).append("\n");
        stats.append("命名空间数: ").append(getNamespaces().size()).append("\n\n");

        for (String namespace : getNamespaces()) {
            stats.append("命名空间 [").append(namespace).append("]:\n");
            Collection<Variant> variants = getAllFrom(namespace);
            for (Variant variant : variants) {
                stats.append("  ").append(variant.getStatistics()).append("\n");
            }
        }

        return stats.toString();
    }
}
