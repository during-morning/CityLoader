package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.VariantRE;
import com.during.cityloader.worldgen.lost.regassets.data.BlockEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 变体类
 * 用于表示方块的变体
 * 
 * @author During
 * @since 1.4.0
 */
public class Variant implements ILostCityAsset {

    private final ResourceLocation name;
    private final List<WeightedBlock> blocks = new ArrayList<>();

    /**
     * 旧兼容字段（逐步废弃）
     */
    private final String legacyVariantName;

    /**
     * 旧兼容字段（逐步废弃）
     */
    private final float legacyWeight;

    /**
     * 从VariantRE构造Variant对象
     * 
     * @param object VariantRE注册实体
     */
    public Variant(VariantRE object) {
        this.name = object.getRegistryName();
        this.legacyVariantName = object.getName();
        this.legacyWeight = object.getWeight();

        if (object.getBlocks() != null) {
            for (BlockEntry entry : object.getBlocks()) {
                if (entry == null || entry.block() == null || entry.block().isBlank()) {
                    continue;
                }
                int random = entry.random();
                if (random <= 0) {
                    continue;
                }
                blocks.add(new WeightedBlock(entry.block(), random));
            }
        }

        // 兼容旧格式：将 name/weight 映射为单一方块选择
        if (blocks.isEmpty() && legacyVariantName != null && !legacyVariantName.isBlank()) {
            int random = Math.max(1, Math.round(legacyWeight));
            blocks.add(new WeightedBlock(legacyVariantName, random));
        }
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public List<WeightedBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public String getRandomBlock(Random random) {
        if (blocks.isEmpty()) {
            return null;
        }
        if (blocks.size() == 1) {
            return blocks.get(0).block();
        }

        int total = 0;
        for (WeightedBlock block : blocks) {
            total += block.weight();
        }
        if (total <= 0) {
            return blocks.get(0).block();
        }

        int value = random.nextInt(total);
        int current = 0;
        for (WeightedBlock block : blocks) {
            current += block.weight();
            if (value < current) {
                return block.block();
            }
        }
        return blocks.get(blocks.size() - 1).block();
    }

    /**
     * 旧兼容方法（逐步废弃）
     */
    public String getVariantName() {
        return legacyVariantName;
    }

    /**
     * 旧兼容方法（逐步废弃）
     */
    public float getWeight() {
        return legacyWeight;
    }

    /**
     * 加权方块条目
     */
    public record WeightedBlock(String block, int weight) {
    }
}
