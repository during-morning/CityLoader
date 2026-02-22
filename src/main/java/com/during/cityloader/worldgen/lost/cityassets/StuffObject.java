package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.StuffSettingsRE;

/**
 * 装饰物对象类
 * 用于表示城市中的装饰物
 * 
 * @author During
 * @since 1.4.0
 */
public class StuffObject implements ILostCityAsset {

    private final ResourceLocation name;
    private final StuffSettingsRE settings;

    /**
     * 从StuffSettingsRE构造StuffObject对象
     * 
     * @param object StuffSettingsRE注册实体
     */
    public StuffObject(StuffSettingsRE object) {
        this.name = object.getRegistryName();
        this.settings = object;
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    /**
     * 获取装饰物设置
     * 
     * @return 装饰物设置
     */
    public StuffSettingsRE getSettings() {
        return settings;
    }
}
