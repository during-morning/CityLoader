package com.during.cityloader.worldgen.gen;

/**
 * 区块生成阶段接口
 */
public interface GenerationStage {

    void generate(GenerationContext context);

    default String name() {
        return getClass().getSimpleName();
    }
}
