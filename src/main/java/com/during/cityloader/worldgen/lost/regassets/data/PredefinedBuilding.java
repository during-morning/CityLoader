package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 预定义建筑记录
 * 移植自 LostCities，表示预定义城市中指定位置的建筑。
 *
 * 兼容多种偏移字段命名：
 * - chunkx/chunkz（原版常用）
 * - rel_chunk_x/rel_chunk_z
 * - relChunkX/relChunkZ
 */
public class PredefinedBuilding {

    @SerializedName("building")
    private String building;

    @SerializedName(value = "rel_chunk_x", alternate = {"relChunkX", "chunkx", "chunkX", "x"})
    private int relChunkX;

    @SerializedName(value = "rel_chunk_z", alternate = {"relChunkZ", "chunkz", "chunkZ", "z"})
    private int relChunkZ;

    @SerializedName(value = "multi")
    private boolean multi;

    @SerializedName(value = "prevent_ruins", alternate = {"preventRuins", "preventruins"})
    private boolean preventRuins;

    public PredefinedBuilding() {
    }

    public PredefinedBuilding(String building, int relChunkX, int relChunkZ) {
        this(building, relChunkX, relChunkZ, false, false);
    }

    public PredefinedBuilding(String building, int relChunkX, int relChunkZ, boolean multi, boolean preventRuins) {
        this.building = building;
        this.relChunkX = relChunkX;
        this.relChunkZ = relChunkZ;
        this.multi = multi;
        this.preventRuins = preventRuins;
    }

    public String getBuilding() {
        return building;
    }

    public int getRelChunkX() {
        return relChunkX;
    }

    public int getRelChunkZ() {
        return relChunkZ;
    }

    public boolean isMulti() {
        return multi;
    }

    public boolean isPreventRuins() {
        return preventRuins;
    }

    // 保持与 record 访问风格兼容
    public int relChunkX() {
        return relChunkX;
    }

    public int relChunkZ() {
        return relChunkZ;
    }

    public boolean multi() {
        return multi;
    }

    public boolean preventRuins() {
        return preventRuins;
    }
}
