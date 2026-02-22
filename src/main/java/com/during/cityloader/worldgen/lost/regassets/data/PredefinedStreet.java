package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 预定义街道记录
 * 兼容基础坐标字段，并支持可选连接方向提示（用于街道路型/旋转对齐）。
 */
public class PredefinedStreet {

    @SerializedName(value = "rel_chunk_x", alternate = {"relChunkX", "chunkx", "chunkX", "x"})
    private int relChunkX;

    @SerializedName(value = "rel_chunk_z", alternate = {"relChunkZ", "chunkz", "chunkZ", "z"})
    private int relChunkZ;

    @SerializedName(value = "north", alternate = {"n"})
    private Boolean north;

    @SerializedName(value = "south", alternate = {"s"})
    private Boolean south;

    @SerializedName(value = "west", alternate = {"w"})
    private Boolean west;

    @SerializedName(value = "east", alternate = {"e"})
    private Boolean east;

    @SerializedName(value = "connections", alternate = {"connect", "dirs", "direction"})
    private String connections;

    @SerializedName(value = "type", alternate = {"part", "streetpart", "streetPart"})
    private String type;

    public int getRelChunkX() {
        return relChunkX;
    }

    public int getRelChunkZ() {
        return relChunkZ;
    }

    // 兼容 record 风格调用
    public int relChunkX() {
        return relChunkX;
    }

    public int relChunkZ() {
        return relChunkZ;
    }

    public Boolean getNorth() {
        return north;
    }

    public Boolean getSouth() {
        return south;
    }

    public Boolean getWest() {
        return west;
    }

    public Boolean getEast() {
        return east;
    }

    public String getConnections() {
        return connections;
    }

    public String getType() {
        return type;
    }
}
