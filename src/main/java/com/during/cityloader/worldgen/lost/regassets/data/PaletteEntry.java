package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * 调色板条目
 * 表示调色板中的单个字符到方块的映射
 * 
 * @author During
 * @since 1.4.0
 */
public class PaletteEntry {
    
    @SerializedName("char")
    private String character;
    
    @SerializedName("block")
    private String block;
    
    @SerializedName("variant")
    private String variant;
    
    @SerializedName("frompalette")
    private String fromPalette;
    
    @SerializedName("damaged")
    private String damaged;
    
    @SerializedName("mob")
    private String mob;
    
    @SerializedName("loot")
    private String loot;
    
    @SerializedName("torch")
    private Boolean torch;
    
    @SerializedName("tag")
    private Map<String, Object> tag;
    
    @SerializedName("blocks")
    private List<BlockChoice> blocks;
    
    /**
     * 默认构造函数（用于Gson反序列化）
     */
    public PaletteEntry() {
    }
    
    /**
     * 获取调色板字符
     * 
     * @return 调色板字符
     */
    public String getCharacter() {
        return character;
    }
    
    /**
     * 设置调色板字符
     * 
     * @param character 调色板字符
     */
    public void setCharacter(String character) {
        this.character = character;
    }
    
    /**
     * 获取方块ID
     * 
     * @return 方块ID
     */
    public String getBlock() {
        return block;
    }
    
    /**
     * 设置方块ID
     * 
     * @param block 方块ID
     */
    public void setBlock(String block) {
        this.block = block;
    }
    
    /**
     * 获取变体引用
     * 
     * @return 变体引用
     */
    public String getVariant() {
        return variant;
    }
    
    /**
     * 设置变体引用
     * 
     * @param variant 变体引用
     */
    public void setVariant(String variant) {
        this.variant = variant;
    }
    
    /**
     * 获取来源调色板引用
     * 
     * @return 来源调色板引用
     */
    public String getFromPalette() {
        return fromPalette;
    }
    
    /**
     * 设置来源调色板引用
     * 
     * @param fromPalette 来源调色板引用
     */
    public void setFromPalette(String fromPalette) {
        this.fromPalette = fromPalette;
    }
    
    /**
     * 获取损坏状态方块
     * 
     * @return 损坏状态方块ID
     */
    public String getDamaged() {
        return damaged;
    }
    
    /**
     * 设置损坏状态方块
     * 
     * @param damaged 损坏状态方块ID
     */
    public void setDamaged(String damaged) {
        this.damaged = damaged;
    }
    
    /**
     * 获取生物类型
     * 
     * @return 生物类型
     */
    public String getMob() {
        return mob;
    }
    
    /**
     * 设置生物类型
     * 
     * @param mob 生物类型
     */
    public void setMob(String mob) {
        this.mob = mob;
    }
    
    /**
     * 获取战利品表
     * 
     * @return 战利品表ID
     */
    public String getLoot() {
        return loot;
    }
    
    /**
     * 设置战利品表
     * 
     * @param loot 战利品表ID
     */
    public void setLoot(String loot) {
        this.loot = loot;
    }
    
    /**
     * 是否放置火把
     * 
     * @return 是否放置火把
     */
    public Boolean getTorch() {
        return torch;
    }
    
    /**
     * 设置是否放置火把
     * 
     * @param torch 是否放置火把
     */
    public void setTorch(Boolean torch) {
        this.torch = torch;
    }
    
    /**
     * 获取NBT标签数据
     * 
     * @return NBT标签数据
     */
    public Map<String, Object> getTag() {
        return tag;
    }
    
    /**
     * 设置NBT标签数据
     * 
     * @param tag NBT标签数据
     */
    public void setTag(Map<String, Object> tag) {
        this.tag = tag;
    }
    
    /**
     * 获取随机方块选择列表
     * 
     * @return 随机方块选择列表
     */
    public List<BlockChoice> getBlocks() {
        return blocks;
    }
    
    /**
     * 设置随机方块选择列表
     * 
     * @param blocks 随机方块选择列表
     */
    public void setBlocks(List<BlockChoice> blocks) {
        this.blocks = blocks;
    }
    
    /**
     * 创建简单方块条目
     * 
     * @param character 字符
     * @param block 方块ID
     * @return 调色板条目
     */
    public static PaletteEntry simpleBlock(String character, String block) {
        PaletteEntry entry = new PaletteEntry();
        entry.character = character;
        entry.block = block;
        return entry;
    }
    
    /**
     * 创建变体引用条目
     * 
     * @param character 字符
     * @param variant 变体名称
     * @return 调色板条目
     */
    public static PaletteEntry variantRef(String character, String variant) {
        PaletteEntry entry = new PaletteEntry();
        entry.character = character;
        entry.variant = variant;
        return entry;
    }
    
    @Override
    public String toString() {
        return "PaletteEntry{" +
                "character='" + character + '\'' +
                ", block='" + block + '\'' +
                ", variant='" + variant + '\'' +
                ", fromPalette='" + fromPalette + '\'' +
                ", damaged='" + damaged + '\'' +
                ", mob='" + mob + '\'' +
                ", loot='" + loot + '\'' +
                ", torch=" + torch +
                ", tag=" + tag +
                ", blocks=" + blocks +
                '}';
    }
}
