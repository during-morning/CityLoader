package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.PredefinedCityRE;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedBuilding;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedStreet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 预定义城市类
 * 用于表示预先定义的城市布局
 * 移植自 LostCities PredefinedCity
 *
 * @author During
 * @since 1.4.0
 */
public class PredefinedCity implements ILostCityAsset {

    private final ResourceLocation name;
    private final String dimension;
    private final int chunkX;
    private final int chunkZ;
    private final int radius;
    private final String cityStyle;
    private final List<PredefinedBuilding> predefinedBuildings;
    private final List<PredefinedStreet> predefinedStreets;

    /**
     * 从PredefinedCityRE构造PredefinedCity对象
     *
     * @param object PredefinedCityRE注册实体
     */
    public PredefinedCity(PredefinedCityRE object) {
        this.name = object.getRegistryName();
        this.dimension = object.getDimension() != null ? object.getDimension() : "minecraft:overworld";
        this.chunkX = object.getChunkX();
        this.chunkZ = object.getChunkZ();
        this.radius = object.getRadius();
        this.cityStyle = object.getCityStyle();

        List<PredefinedBuilding> buildings = new ArrayList<>();
        if (object.getPredefinedBuildings() != null) {
            buildings.addAll(object.getPredefinedBuildings());
        }
        this.predefinedBuildings = Collections.unmodifiableList(buildings);

        List<PredefinedStreet> streets = new ArrayList<>();
        if (object.getPredefinedStreets() != null) {
            streets.addAll(object.getPredefinedStreets());
        }
        this.predefinedStreets = Collections.unmodifiableList(streets);
    }

    /**
     * 获取维度标识符
     */
    public String getDimension() {
        return dimension;
    }

    /**
     * 获取城市中心区块X坐标
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * 获取城市中心区块Z坐标
     */
    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * 获取城市半径（方块单位）
     */
    public int getRadius() {
        return radius;
    }

    /**
     * 获取城市样式名
     */
    public String getCityStyle() {
        return cityStyle;
    }

    /**
     * 获取预定义建筑列表
     */
    public List<PredefinedBuilding> getPredefinedBuildings() {
        return predefinedBuildings;
    }

    /**
     * 获取预定义街道列表
     */
    public List<PredefinedStreet> getPredefinedStreets() {
        return predefinedStreets;
    }

    @Override
    public String getName() {
        return name != null ? name.toString() : "";
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }
}
