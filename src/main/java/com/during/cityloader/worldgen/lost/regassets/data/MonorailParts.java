package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 单轨列车部件设置
 * 包含单轨、垂直单轨和车站的零件引用
 */
public class MonorailParts {

    @SerializedName(value = "both", alternate = {"monorails_both"})
    private String both = "monorails_both";

    @SerializedName(value = "vertical", alternate = {"monorails_vertical"})
    private String vertical = "monorails_vertical";

    @SerializedName(value = "station", alternate = {"monorails_station"})
    private String station = "monorails_station";

    public MonorailParts() {
    }

    public MonorailParts(String both, String vertical, String station) {
        this.both = both;
        this.vertical = vertical;
        this.station = station;
    }

    public String getBoth() {
        return both;
    }

    public void setBoth(String both) {
        this.both = both;
    }

    public String getVertical() {
        return vertical;
    }

    public void setVertical(String vertical) {
        this.vertical = vertical;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    /**
     * 默认单轨部件
     */
    public static final MonorailParts DEFAULT = new MonorailParts(
            "monorails_both",
            "monorails_vertical", 
            "monorails_station"
    );
}
