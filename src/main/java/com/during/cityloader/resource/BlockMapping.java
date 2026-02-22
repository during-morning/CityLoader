package com.during.cityloader.resource;

import com.during.cityloader.season.Season;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 方块映射类
 * 定义一个字符到方块的映射，支持季节变体和权重随机
 * 
 * @author During
 * @since 1.4.0
 */
public class BlockMapping {

    // 默认方块
    private final Material defaultBlock;

    // 季节变体映射 (季节 -> 方块)
    private final Map<Season, Material> seasonalVariants;

    // 变体ID (如果设置，将使用变体系统而不是固定方块)
    private final String variantId;

    // 引用其他调色板的字符
    private final Character fromPaletteChar;

    // 权重（用于随机选择）
    private final double weight;

    // 元数据（可选）
    private final Info info;

    /**
     * 构造函数
     * 
     * @param defaultBlock     默认方块
     * @param seasonalVariants 季节变体映射
     * @param weight           权重
     */
    public BlockMapping(Material defaultBlock, Map<Season, Material> seasonalVariants, double weight) {
        this(defaultBlock, seasonalVariants, null, weight);
    }

    /**
     * 完整构造函数（带Info）
     * 
     * @param defaultBlock     默认方块
     * @param seasonalVariants 季节变体映射
     * @param variantId        变体ID
     * @param fromPaletteChar  引用字符
     * @param weight           权重
     * @param info             元数据
     */
    public BlockMapping(Material defaultBlock, Map<Season, Material> seasonalVariants, String variantId,
            Character fromPaletteChar, double weight, Info info) {
        this.defaultBlock = defaultBlock;
        this.seasonalVariants = seasonalVariants != null ? seasonalVariants : new HashMap<>();
        this.variantId = variantId;
        this.fromPaletteChar = fromPaletteChar;
        this.weight = weight;
        this.info = info;
    }

    /**
     * 完整构造函数（无Info）
     * 
     * @param defaultBlock     默认方块
     * @param seasonalVariants 季节变体映射
     * @param variantId        变体ID
     * @param fromPaletteChar  引用字符
     * @param weight           权重
     */
    public BlockMapping(Material defaultBlock, Map<Season, Material> seasonalVariants, String variantId,
            Character fromPaletteChar, double weight) {
        this(defaultBlock, seasonalVariants, variantId, fromPaletteChar, weight, null);
    }

    /**
     * 完整构造函数(旧版兼容)
     * 
     * @param defaultBlock     默认方块
     * @param seasonalVariants 季节变体映射
     * @param variantId        变体ID
     * @param weight           权重
     */
    public BlockMapping(Material defaultBlock, Map<Season, Material> seasonalVariants, String variantId,
            double weight) {
        this(defaultBlock, seasonalVariants, variantId, null, weight);
    }

    /**
     * 变体构造函数
     * 
     * @param variantId 变体ID
     * @param weight    权重
     */
    /**
     * 引用构造函数
     * 
     * @param fromPaletteChar 引用字符
     */
    public BlockMapping(Character fromPaletteChar) {
        this(null, null, null, fromPaletteChar, 1.0);
    }

    /**
     * 变体构造函数
     * 
     * @param variantId 变体ID
     * @param weight    权重
     */
    public BlockMapping(String variantId, double weight) {
        this(null, null, variantId, null, weight);
    }

    /**
     * 简化构造函数（无季节变体）
     * 
     * @param defaultBlock 默认方块
     * @param weight       权重
     */
    public BlockMapping(Material defaultBlock, double weight) {
        this(defaultBlock, new HashMap<>(), weight);
    }

    /**
     * 简化构造函数（无权重和季节变体）
     * 
     * @param defaultBlock 默认方块
     */
    public BlockMapping(Material defaultBlock) {
        this(defaultBlock, new HashMap<>(), 1.0);
    }

    /**
     * 根据季节获取方块
     * 如果该季节有特定变体，返回变体；否则返回默认方块
     * 
     * @param season 季节
     * @return 对应的方块材质
     */
    public Material getBlockForSeason(Season season) {
        if (season != null && seasonalVariants.containsKey(season)) {
            return seasonalVariants.get(season);
        }
        return defaultBlock;
    }

    /**
     * 根据季节和随机数获取方块
     * 支持权重随机选择
     * 
     * @param season 季节
     * @param random 随机数生成器
     * @return 对应的方块材质
     */
    public Material getBlockForSeason(Season season, Random random) {
        // 简化实现：直接返回季节对应的方块
        // 实际应用中可以根据权重进行随机选择
        return getBlockForSeason(season);
    }

    /**
     * 添加季节变体
     * 
     * @param season 季节
     * @param block  方块材质
     */
    public void addSeasonalVariant(Season season, Material block) {
        seasonalVariants.put(season, block);
    }

    // Getter方法

    public Material getDefaultBlock() {
        return defaultBlock;
    }

    public Map<Season, Material> getSeasonalVariants() {
        return new HashMap<>(seasonalVariants);
    }

    public double getWeight() {
        return weight;
    }

    /**
     * 检查是否有季节变体
     * 
     * @return 如果有季节变体返回true
     */
    public boolean hasSeasonalVariants() {
        return !seasonalVariants.isEmpty();
    }

    public String getVariantId() {
        return variantId;
    }

    /**
     * 检查是否为变体引用
     * 
     * @return 如果是变体引用返回true
     */
    public boolean isVariant() {
        return variantId != null && !variantId.isEmpty();
    }

    public Character getFromPaletteChar() {
        return fromPaletteChar;
    }

    public Info getInfo() {
        return info;
    }

    /**
     * 检查是否有元数据
     * 
     * @return 如果有Info返回true
     */
    public boolean hasInfo() {
        return info != null && info.hasMetadata();
    }

    /**
     * 检查是否为引用
     * 
     * @return 如果是引用返回true
     */
    public boolean isReference() {
        return fromPaletteChar != null;
    }
}
