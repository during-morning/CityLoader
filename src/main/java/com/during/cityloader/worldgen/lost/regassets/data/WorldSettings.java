package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 世界级设置
 */
public class WorldSettings {

    @SerializedName(value = "railwayavoidance", alternate = { "railwayAvoidance" })
    private String railwayAvoidance;

    @SerializedName(value = "railpartheight6", alternate = { "railPartHeight6" })
    private Integer railPartHeight6;

    @SerializedName(value = "citychance", alternate = { "cityChance" })
    private Float cityChance;

    @SerializedName("highways")
    private Boolean highways;

    @SerializedName("railways")
    private Boolean railways;

    public String getRailwayAvoidance() {
        return railwayAvoidance;
    }

    public void setRailwayAvoidance(String railwayAvoidance) {
        this.railwayAvoidance = railwayAvoidance;
    }

    public Integer getRailPartHeight6() {
        return railPartHeight6;
    }

    public void setRailPartHeight6(Integer railPartHeight6) {
        this.railPartHeight6 = railPartHeight6;
    }

    public Float getCityChance() {
        return cityChance;
    }

    public void setCityChance(Float cityChance) {
        this.cityChance = cityChance;
    }

    public Boolean getHighways() {
        return highways;
    }

    public void setHighways(Boolean highways) {
        this.highways = highways;
    }

    public Boolean getRailways() {
        return railways;
    }

    public void setRailways(Boolean railways) {
        this.railways = railways;
    }
}
