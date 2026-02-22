package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.Building;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 建筑注册表
 * 线程安全的建筑存储和管理，支持命名空间隔离
 * 
 * @author During
 * @since 1.4.0
 */
public class BuildingRegistry extends NamespacedRegistry<Building> {

    /**
     * 构造函数
     */
    public BuildingRegistry() {
        super(true); // 需要线程安全
    }

    /**
     * 注册建筑
     * 
     * @param building 建筑对象
     * @return 如果成功注册返回true，如果ID已存在返回false
     */
    public boolean register(Building building) {
        if (building == null || building.getId() == null) {
            return false;
        }

        try {
            // 使用父类的register方法
            register(building.getId(), building);
            return true;
        } catch (IllegalArgumentException e) {
            // ID已存在
            return false;
        }
    }

    /**
     * 根据类型获取建筑
     * 
     * @param type 建筑类型
     * @return 指定类型的建筑列表
     */
    public List<Building> getByType(String type) {
        if (type == null) {
            return new ArrayList<>();
        }

        return getAll().stream()
                .filter(building -> type.equals(building.getType()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有建筑类型
     * 
     * @return 建筑类型列表
     */
    public List<String> getAllTypes() {
        return getAll().stream()
                .map(Building::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    protected String getRegistryName() {
        return "建筑注册表";
    }

    /**
     * 获取注册的建筑数量（别名方法，保持向后兼容）
     * 
     * @return 建筑数量
     */
    public int getCount() {
        return size();
    }
}
