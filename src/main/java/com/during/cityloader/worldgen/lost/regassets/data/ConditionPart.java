package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;
import java.util.Set;

/**
 * 条件部分数据类
 * 用于定义条件的值和权重
 * 继承自ConditionTest以支持条件测试
 * 
 * @author During
 * @since 1.4.0
 */
public class ConditionPart extends ConditionTest {
    
    @SerializedName("factor")
    private final float factor;
    
    @SerializedName("value")
    private final String value;

    /**
     * 构造条件部分对象
     */
    public ConditionPart(float factor, String value,
                         Optional<Boolean> top,
                         Optional<Boolean> ground,
                         Optional<Boolean> cellar,
                         Optional<Boolean> isbuilding,
                         Optional<Boolean> issphere,
                         Optional<Integer> floor,
                         Optional<Integer> chunkx,
                         Optional<Integer> chunkz,
                         Optional<Set<String>> belowPart,
                         Optional<Set<String>> inpart,
                         Optional<Set<String>> inbuilding,
                         Optional<Set<String>> inbiome,
                         Optional<String> range) {
        super(top, ground, cellar, isbuilding, issphere, floor, chunkx, chunkz, 
              belowPart, inpart, inbuilding, inbiome, range);
        this.factor = factor;
        this.value = value;
    }
    
    public float getFactor() {
        return factor;
    }
    
    public String getValue() {
        return value;
    }
}
