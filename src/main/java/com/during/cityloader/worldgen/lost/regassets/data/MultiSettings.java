package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

/**
 * 多建筑设置
 */
public class MultiSettings {

    @SerializedName("areasize")
    private Integer areaSize;

    @SerializedName("minimum")
    private Integer minimum;

    @SerializedName("maximum")
    private Integer maximum;

    @SerializedName("attempts")
    private Integer attempts;

    @SerializedName(value = "correctstylefactor", alternate = { "correctStyleFactor" })
    private Float correctStyleFactor;

    public Integer getAreaSize() {
        return areaSize;
    }

    public void setAreaSize(Integer areaSize) {
        this.areaSize = areaSize;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Float getCorrectStyleFactor() {
        return correctStyleFactor;
    }

    public void setCorrectStyleFactor(Float correctStyleFactor) {
        this.correctStyleFactor = correctStyleFactor;
    }
}
