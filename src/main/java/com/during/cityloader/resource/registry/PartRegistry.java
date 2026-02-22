package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.Part;

/**
 * 部件注册表
 * 线程安全的部件存储和管理，支持命名空间隔离
 * 
 * @author During
 * @since 1.4.0
 */
public class PartRegistry extends NamespacedRegistry<Part> {

    /**
     * 构造函数
     */
    public PartRegistry() {
        super(true); // 需要线程安全
    }

    /**
     * 注册部件
     * 
     * @param part 部件对象
     * @return 如果成功注册返回true，如果ID已存在返回false
     */
    public boolean register(Part part) {
        if (part == null || part.getId() == null) {
            return false;
        }

        try {
            // 使用父类的register方法
            register(part.getId(), part);
            return true;
        } catch (IllegalArgumentException e) {
            // ID已存在
            return false;
        }
    }

    @Override
    protected String getRegistryName() {
        return "部件注册表";
    }

    /**
     * 获取注册的部件数量（别名方法，保持向后兼容）
     * 
     * @return 部件数量
     */
    public int getCount() {
        return size();
    }
}
