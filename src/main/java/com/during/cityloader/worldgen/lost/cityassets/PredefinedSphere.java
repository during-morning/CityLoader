package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.PredefinedSphereRE;

/**
 * 预定义球体类
 * 用于表示预先定义的球体结构
 * 
 * @author During
 * @since 1.4.0
 */
public class PredefinedSphere implements ILostCityAsset {

    private final ResourceLocation name;

    /**
     * 从PredefinedSphereRE构造PredefinedSphere对象
     * 
     * @param object PredefinedSphereRE注册实体
     */
    public PredefinedSphere(PredefinedSphereRE object) {
        this.name = object.getRegistryName();
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }
}
