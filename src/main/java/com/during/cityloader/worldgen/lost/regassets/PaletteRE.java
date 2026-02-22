package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.PaletteEntry;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调色板注册实体
 * 用于从JSON反序列化调色板数据
 * 支持新的PaletteEntry格式和旧的Map格式（向后兼容）
 * 
 * @author During
 * @since 1.4.0
 */
public class PaletteRE implements IAsset {
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("palette")
    private List<PaletteEntry> paletteEntries = new ArrayList<>();
    
    private transient ResourceLocation registryName;
    
    // 缓存的字符到条目的映射，用于快速查找
    private transient Map<String, PaletteEntry> characterMap;
    
    /**
     * 获取名称
     * 
     * @return 名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置名称
     * 
     * @param name 名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取调色板条目列表
     * 
     * @return 调色板条目列表
     */
    public List<PaletteEntry> getPaletteEntries() {
        return paletteEntries;
    }
    
    /**
     * 设置调色板条目列表
     * 
     * @param paletteEntries 调色板条目列表
     */
    public void setPaletteEntries(List<PaletteEntry> paletteEntries) {
        this.paletteEntries = paletteEntries;
        this.characterMap = null; // 清除缓存
    }
    
    /**
     * 获取字符映射（向后兼容）
     * 将新格式转换为旧的Map格式
     * 
     * @return 字符到方块ID的映射
     */
    @Deprecated
    public Map<String, String> getPalette() {
        Map<String, String> result = new HashMap<>();
        for (PaletteEntry entry : paletteEntries) {
            if (entry.getCharacter() != null && entry.getBlock() != null) {
                result.put(entry.getCharacter(), entry.getBlock());
            }
        }
        return result;
    }
    
    /**
     * 设置调色板（向后兼容）
     * 将旧的Map格式转换为新的PaletteEntry格式
     * 
     * @param palette 字符到方块ID的映射
     */
    @Deprecated
    public void setPalette(Map<String, String> palette) {
        this.paletteEntries.clear();
        for (Map.Entry<String, String> entry : palette.entrySet()) {
            this.paletteEntries.add(PaletteEntry.simpleBlock(entry.getKey(), entry.getValue()));
        }
        this.characterMap = null; // 清除缓存
    }
    
    /**
     * 根据字符获取调色板条目
     * 
     * @param character 字符
     * @return 调色板条目，如果不存在则返回null
     */
    public PaletteEntry getEntry(String character) {
        if (characterMap == null) {
            buildCharacterMap();
        }
        return characterMap.get(character);
    }
    
    /**
     * 根据字符获取方块ID（向后兼容）
     * 
     * @param character 字符
     * @return 方块ID，如果不存在则返回null
     */
    @Deprecated
    public String getBlock(String character) {
        PaletteEntry entry = getEntry(character);
        return entry != null ? entry.getBlock() : null;
    }
    
    /**
     * 构建字符到条目的映射缓存
     */
    private void buildCharacterMap() {
        characterMap = new HashMap<>();
        for (PaletteEntry entry : paletteEntries) {
            if (entry.getCharacter() != null) {
                characterMap.put(entry.getCharacter(), entry);
            }
        }
    }
    
    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }
    
    @Override
    public void setRegistryName(ResourceLocation name) {
        this.registryName = name;
    }
}
