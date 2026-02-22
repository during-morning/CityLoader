package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 生物群系匹配规则
 */
public class BiomeMatcher {

    @SerializedName(value = "if_any", alternate = { "ifAny" })
    private List<String> ifAny = new ArrayList<>();

    @SerializedName(value = "if_all", alternate = { "ifAll" })
    private List<String> ifAll = new ArrayList<>();

    @SerializedName(value = "excluding", alternate = { "exclude" })
    private List<String> excluding = new ArrayList<>();

    public List<String> getIfAny() {
        return ifAny;
    }

    public void setIfAny(List<String> ifAny) {
        this.ifAny = ifAny == null ? new ArrayList<>() : ifAny;
    }

    public List<String> getIfAll() {
        return ifAll;
    }

    public void setIfAll(List<String> ifAll) {
        this.ifAll = ifAll == null ? new ArrayList<>() : ifAll;
    }

    public List<String> getExcluding() {
        return excluding;
    }

    public void setExcluding(List<String> excluding) {
        this.excluding = excluding == null ? new ArrayList<>() : excluding;
    }
}
