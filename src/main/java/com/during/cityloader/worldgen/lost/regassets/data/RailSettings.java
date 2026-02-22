package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 铁路相关设置
 */
public class RailSettings {

    @SerializedName(value = "enabled", alternate = { "railEnabled" })
    private Boolean enabled;

    @SerializedName(value = "height", alternate = { "railHeight" })
    private Integer height;

    @SerializedName(value = "railmain", alternate = { "railMain" })
    private String railMain;

    @SerializedName("rails3split")
    private String rails3Split;

    @SerializedName("railsbend")
    private String railsBend;

    @SerializedName("railsdown1")
    private String railsDown1;

    @SerializedName("railsdown2")
    private String railsDown2;

    @SerializedName("railsflat")
    private String railsFlat;

    @SerializedName("railshorizontal")
    private String railsHorizontal;

    @SerializedName("railshorizontalend")
    private String railsHorizontalEnd;

    @SerializedName("railshorizontalwater")
    private String railsHorizontalWater;

    @SerializedName("railsvertical")
    private String railsVertical;

    @SerializedName("railsverticalwater")
    private String railsVerticalWater;

    @SerializedName("stationunderground")
    private String stationUnderground;

    @SerializedName("stationundergroundstairs")
    private String stationUndergroundStairs;

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getHeight() {
        return height;
    }

    public String getRailMain() {
        return railMain;
    }

    public String getRails3Split() {
        return rails3Split;
    }

    public String getRailsBend() {
        return railsBend;
    }

    public String getRailsDown1() {
        return railsDown1;
    }

    public String getRailsDown2() {
        return railsDown2;
    }

    public String getRailsFlat() {
        return railsFlat;
    }

    public String getRailsHorizontal() {
        return railsHorizontal;
    }

    public String getRailsHorizontalEnd() {
        return railsHorizontalEnd;
    }

    public String getRailsHorizontalWater() {
        return railsHorizontalWater;
    }

    public String getRailsVertical() {
        return railsVertical;
    }

    public String getRailsVerticalWater() {
        return railsVerticalWater;
    }

    public String getStationUnderground() {
        return stationUnderground;
    }

    public String getStationUndergroundStairs() {
        return stationUndergroundStairs;
    }
}
