package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.data.BlockChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调色板类
 */
public class Palette implements ILostCityAsset {

    private final ResourceLocation name;
    private final Map<Character, Entry> entries = new LinkedHashMap<>();

    public Palette(PaletteRE object) {
        this.name = object.getRegistryName();
        if (object.getPaletteEntries() != null) {
            for (com.during.cityloader.worldgen.lost.regassets.data.PaletteEntry re : object.getPaletteEntries()) {
                if (re == null || re.getCharacter() == null || re.getCharacter().isEmpty()) {
                    continue;
                }
                char key = re.getCharacter().charAt(0);

                List<WeightedBlock> blocks = new ArrayList<>();
                if (re.getBlock() != null && !re.getBlock().isBlank()) {
                    blocks.add(new WeightedBlock(re.getBlock(), 1));
                }
                if (re.getBlocks() != null) {
                    for (BlockChoice choice : re.getBlocks()) {
                        if (choice == null || choice.getBlock() == null || choice.getBlock().isBlank()) {
                            continue;
                        }
                        blocks.add(new WeightedBlock(choice.getBlock(), Math.max(1, choice.getRandom())));
                    }
                }

                Entry entry = new Entry(
                        key,
                        Collections.unmodifiableList(blocks),
                        re.getVariant(),
                        re.getFromPalette(),
                        re.getDamaged(),
                        new Info(re.getLoot(), re.getMob(), Boolean.TRUE.equals(re.getTorch()), re.getTag()));
                entries.put(key, entry);
            }
        }
    }

    public Palette(String name) {
        this.name = new ResourceLocation("lostcities", name);
    }

    public void merge(Palette other) {
        if (other == null) {
            return;
        }
        this.entries.putAll(other.entries);
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    /**
     * 旧兼容方法：返回字符到主方块的映射。
     */
    public Map<Character, String> getPalette() {
        Map<Character, String> result = new HashMap<>();
        for (Entry entry : entries.values()) {
            result.put(entry.character(), entry.getPrimaryBlock());
        }
        return result;
    }

    public Map<Character, Entry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public Entry getEntry(char c) {
        return entries.get(c);
    }

    /**
     * 旧兼容方法。
     */
    public String get(char c) {
        Entry entry = entries.get(c);
        return entry == null ? null : entry.getPrimaryBlock();
    }

    public record WeightedBlock(String block, int weight) {
    }

    public record Info(String loot, String mob, boolean torch, Map<String, Object> tag) {
    }

    public record Entry(char character,
                        List<WeightedBlock> blocks,
                        String variant,
                        String fromPalette,
                        String damaged,
                        Info info) {
        public String getPrimaryBlock() {
            if (blocks != null && !blocks.isEmpty()) {
                return blocks.get(0).block();
            }
            if (variant != null && !variant.isBlank()) {
                return variant;
            }
            return null;
        }
    }
}
