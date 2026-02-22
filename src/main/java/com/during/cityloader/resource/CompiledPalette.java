package com.during.cityloader.resource;

import com.during.cityloader.resource.registry.VariantRegistry;
import com.during.cityloader.season.Season;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 编译后的调色板
 * 用于优化运行时性能，预先解析所有引用链
 * 
 * @author During
 * @since 1.5.0
 */
public class CompiledPalette {

    private final Palette sourcePalette;
    private final VariantRegistry variantRegistry;

    // 编译后的映射：字符 -> (BlockMapping 或 Variant)
    // 这里的BlockMapping已经不再包含引用，而是直接指向最终定义的BlockMapping
    private final Map<Character, Object> compiled = new HashMap<>();

    public CompiledPalette(Palette sourcePalette, VariantRegistry variantRegistry) {
        this.sourcePalette = sourcePalette;
        this.variantRegistry = variantRegistry;
        compile();
    }

    /**
     * 执行编译
     */
    private void compile() {
        if (sourcePalette == null)
            return;

        Map<Character, BlockMapping> mappings = sourcePalette.getBlockMappings();
        for (Character c : mappings.keySet()) {
            compileCharacter(c, new HashSet<>());
        }
    }

    /**
     * 编译单个字符
     * 
     * @param c        字符
     * @param visiting 访问过的字符集合（用于检测循环引用）
     * @return 编译后的对象（BlockMapping, Variant, Material等）
     */
    private Object compileCharacter(char c, Set<Character> visiting) {
        if (compiled.containsKey(c)) {
            return compiled.get(c);
        }

        if (visiting.contains(c)) {
            // 检测到循环引用，返回空气
            return Material.AIR;
        }

        visiting.add(c);

        BlockMapping mapping = sourcePalette.getMapping(c);
        if (mapping == null) {
            visiting.remove(c);
            return null;
        }

        Object result = null;

        if (mapping.isReference()) {
            // 引用类型，递归解析
            Character refChar = mapping.getFromPaletteChar();
            // 这里假设引用是在同一个调色板中（如果是跨调色板，需要ResourceLoader支持）
            // 目前简化为同调色板引用
            result = compileCharacter(refChar, visiting);
        } else if (mapping.isVariant()) {
            // 变体类型，解析变体
            if (variantRegistry != null) {
                Variant variant = variantRegistry.get(mapping.getVariantId());
                if (variant != null) {
                    result = variant;
                }
            }
        } else {
            // 普通方块映射
            result = mapping;
        }

        if (result != null) {
            compiled.put(c, result);
        }

        visiting.remove(c);
        return result;
    }

    /**
     * 获取编译后的结果
     * 
     * @param c 字符
     * @return 编译后的对象
     */
    public Object get(char c) {
        return compiled.get(c);
    }

    /**
     * 获取Material（解析Variant和随机性，支持季节）
     * 
     * @param c      字符
     * @param season 季节
     * @param random 随机数生成器
     * @return 最终的Material
     */
    public Material get(char c, Season season, Random random) {
        Object obj = compiled.get(c);
        if (obj == null)
            return Material.AIR;

        if (obj instanceof Variant) {
            return ((Variant) obj).getRandomBlock(random);
        } else if (obj instanceof BlockMapping) {
            return ((BlockMapping) obj).getBlockForSeason(season);
        } else if (obj instanceof Material) {
            return (Material) obj;
        }

        return Material.AIR;
    }

    /**
     * 获取Material（解析Variant和随机性，使用默认季节）
     * 
     * @param c      字符
     * @param random 随机数生成器
     * @return 最终的Material
     */
    public Material get(char c, Random random) {
        return get(c, null, random);
    }
}
