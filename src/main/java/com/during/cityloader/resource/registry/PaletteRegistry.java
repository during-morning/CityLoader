package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.Palette;

/**
 * 调色板注册表
 * 线程安全的调色板存储和管理，支持命名空间隔离
 * 
 * @author During
 * @since 1.4.0
 */
public class PaletteRegistry extends NamespacedRegistry<Palette> {

    /**
     * 构造函数
     */
    public PaletteRegistry() {
        super(true); // 需要线程安全
    }

    /**
     * 注册调色板
     * 
     * @param palette 调色板对象
     * @return 如果成功注册返回true，如果ID已存在返回false
     */
    public boolean register(Palette palette) {
        if (palette == null || palette.getId() == null) {
            return false;
        }

        try {
            // 使用父类的register方法
            register(palette.getId(), palette);
            return true;
        } catch (IllegalArgumentException e) {
            // ID已存在
            return false;
        }
    }

    @Override
    protected String getRegistryName() {
        return "调色板注册表";
    }

    /**
     * 获取注册的调色板数量（别名方法，保持向后兼容）
     * 
     * @return 调色板数量
     */
    public int getCount() {
        return size();
    }
}
