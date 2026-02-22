package com.during.cityloader.resource;

import com.during.cityloader.season.Season;
import org.bukkit.Material;

import com.during.cityloader.resource.registry.VariantRegistry;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 调色板类
 * 定义字符到方块的映射关系，支持季节变体
 * 
 * @author During
 * @since 1.4.0
 */
public class Palette {

    // 调色板ID
    private final String id;

    // 字符到方块映射的映射表
    private final Map<Character, BlockMapping> blockMappings;

    /**
     * 构造函数
     * 
     * @param id 调色板ID
     */
    public Palette(String id) {
        this.id = id;
        this.blockMappings = new HashMap<>();
    }

    /**
     * 添加方块映射
     * 
     * @param character 字符
     * @param mapping   方块映射
     */
    public void addBlockMapping(char character, BlockMapping mapping) {
        blockMappings.put(character, mapping);
    }

    /**
     * 根据字符和季节获取方块
     * 
     * @param character 字符
     * @param season    季节
     * @return 对应的方块材质，如果未找到返回AIR
     */
    public Material getBlock(char character, Season season) {
        return getBlock(character, season, null, null);
    }

    /**
     * 根据字符、季节和变体注册表获取方块
     * 支持变体解析
     * 
     * @param character       字符
     * @param season          季节
     * @param variantRegistry 变体注册表
     * @param random          随机数生成器
     * @return 对应的方块材质，如果未找到返回AIR
     */
    public Material getBlock(char character, Season season, VariantRegistry variantRegistry, Random random) {
        BlockMapping mapping = blockMappings.get(character);
        if (mapping == null) {
            // 如果字符未映射，返回空气方块
            return Material.AIR;
        }

        // 检查是否为变体引用
        if (mapping.isVariant() && variantRegistry != null && random != null) {
            Variant variant = variantRegistry.get(mapping.getVariantId());
            if (variant != null) {
                return variant.getRandomBlock(random);
            }
        }

        return mapping.getBlockForSeason(season);
    }

    /**
     * 根据字符获取方块（使用默认方块，不考虑季节）
     * 
     * @param character 字符
     * @return 对应的方块材质，如果未找到返回AIR
     */
    public Material getBlock(char character) {
        BlockMapping mapping = blockMappings.get(character);
        if (mapping == null) {
            return Material.AIR;
        }

        // 如果是变体引用且没有提供注册表，尝试返回默认方块
        // 注意：如果BlockMapping完全基于变体，defaultBlock可能为null
        Material defaultBlock = mapping.getDefaultBlock();
        if (defaultBlock == null) {
            return Material.AIR;
        }
        return defaultBlock;
    }

    /**
     * 检查字符是否有映射
     * 
     * @param character 字符
     * @return 如果有映射返回true
     */
    public boolean hasMapping(char character) {
        return blockMappings.containsKey(character);
    }

    /**
     * 获取方块映射
     * 
     * @param character 字符
     * @return 方块映射，如果不存在返回null
     */
    public BlockMapping getMapping(char character) {
        return blockMappings.get(character);
    }

    /**
     * 验证调色板的有效性
     * 检查所有映射的方块是否有效
     * 
     * @return 如果有效返回true
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) {
            return false;
        }

        // 检查所有映射的方块是否有效
        for (BlockMapping mapping : blockMappings.values()) {
            if (mapping.getDefaultBlock() == null && !mapping.isVariant()) {
                return false;
            }
        }

        return true;
    }

    // Getter方法

    public String getId() {
        return id;
    }

    public Map<Character, BlockMapping> getBlockMappings() {
        return new HashMap<>(blockMappings);
    }

    /**
     * 获取映射数量
     * 
     * @return 映射数量
     */
    public int getMappingCount() {
        return blockMappings.size();
    }

    /**
     * 编译调色板
     * 
     * @param variantRegistry 变体注册表
     * @return 编译后的调色板
     */
    public CompiledPalette compile(VariantRegistry variantRegistry) {
        return new CompiledPalette(this, variantRegistry);
    }
}
