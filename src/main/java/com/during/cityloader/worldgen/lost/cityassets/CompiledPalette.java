package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 编译后的调色板类
 */
public class CompiledPalette {

    private static final int RANDOM_TABLE_SIZE = 128;

    private final Map<Character, CompiledEntry> palette = new HashMap<>();

    public CompiledPalette() {
    }

    public CompiledPalette(CompiledPalette other, Palette... palettes) {
        if (other != null) {
            this.palette.putAll(other.palette);
        }
        addPalettes(palettes);
    }

    public CompiledPalette(Palette... palettes) {
        addPalettes(palettes);
    }

    private void addPalettes(Palette[] palettes) {
        if (palettes == null) {
            return;
        }
        for (Palette p : palettes) {
            if (p == null) {
                continue;
            }
            for (Map.Entry<Character, Palette.Entry> entry : p.getEntries().entrySet()) {
                Palette.Entry resolved = resolveEntry(p, entry.getValue(), new LinkedHashSet<>());
                if (resolved == null || resolved.blocks() == null || resolved.blocks().isEmpty()) {
                    continue;
                }

                CompiledEntry compiled = new CompiledEntry(
                        resolved.blocks(),
                        resolved.damaged(),
                        toInformation(resolved.info()));
                palette.put(entry.getKey(), compiled);
            }
        }
    }

    private Palette.Entry resolveEntry(Palette owner, Palette.Entry entry, Set<String> stack) {
        if (entry == null) {
            return null;
        }

        char c = entry.character();
        String stackKey = owner.getName() + "#" + c;
        if (!stack.add(stackKey)) {
            return entry;
        }

        List<Palette.WeightedBlock> localBlocks = normalizeBlocks(entry.blocks());
        boolean hasLocalBlocks = !localBlocks.isEmpty() || (entry.variant() != null && !entry.variant().isBlank());

        Palette.Entry inherited = null;
        if (entry.fromPalette() != null && !entry.fromPalette().isBlank()) {
            String from = entry.fromPalette().trim();
            if (isCharacterReference(from)) {
                char sourceChar = from.charAt(0);
                if (sourceChar != c) {
                    Palette.Entry source = owner.getEntry(sourceChar);
                    if (source != null) {
                        inherited = resolveEntry(owner, source, stack);
                    } else {
                        CompiledEntry compiledInherited = palette.get(sourceChar);
                        if (compiledInherited != null) {
                            inherited = toEntryFromCompiled(sourceChar, compiledInherited);
                        }
                    }
                }
            } else {
                Palette referencedPalette = findReferencedPalette(owner.getId(), from);
                if (referencedPalette != null) {
                    Palette.Entry source = referencedPalette.getEntry(c);
                    inherited = resolveEntry(referencedPalette, source, stack);
                }
            }
        }

        List<Palette.WeightedBlock> resolvedBlocks = new ArrayList<>();
        if (!hasLocalBlocks && inherited != null && inherited.blocks() != null) {
            resolvedBlocks.addAll(inherited.blocks());
        }

        if (!localBlocks.isEmpty()) {
            resolvedBlocks.clear();
            resolvedBlocks.addAll(localBlocks);
        } else if (entry.variant() != null && !entry.variant().isBlank()) {
            Variant variant = AssetRegistries.VARIANTS.get(null, resolveLocation(owner.getId(), entry.variant()));
            if (variant != null && !variant.getBlocks().isEmpty()) {
                resolvedBlocks.clear();
                for (Variant.WeightedBlock weighted : variant.getBlocks()) {
                    resolvedBlocks.add(new Palette.WeightedBlock(weighted.block(), Math.max(1, weighted.weight())));
                }
            }
        }

        String damaged = entry.damaged() != null ? entry.damaged() : (inherited == null ? null : inherited.damaged());
        Palette.Info info = mergeInfo(inherited == null ? null : inherited.info(), entry.info());

        stack.remove(stackKey);
        return new Palette.Entry(
                c,
                Collections.unmodifiableList(resolvedBlocks),
                entry.variant(),
                entry.fromPalette(),
                damaged,
                info);
    }

    private List<Palette.WeightedBlock> normalizeBlocks(List<Palette.WeightedBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Palette.WeightedBlock> normalized = new ArrayList<>();
        for (Palette.WeightedBlock block : blocks) {
            if (block == null || block.block() == null || block.block().isBlank()) {
                continue;
            }
            normalized.add(new Palette.WeightedBlock(block.block(), Math.max(1, block.weight())));
        }
        return normalized;
    }

    private Palette.Info mergeInfo(Palette.Info base, Palette.Info local) {
        if (base == null) {
            return local;
        }
        if (local == null) {
            return base;
        }

        String loot = local.loot() != null ? local.loot() : base.loot();
        String mob = local.mob() != null ? local.mob() : base.mob();
        boolean torch = local.torch() || base.torch();

        Map<String, Object> mergedTag = new HashMap<>();
        if (base.tag() != null) {
            mergedTag.putAll(base.tag());
        }
        if (local.tag() != null) {
            mergedTag.putAll(local.tag());
        }

        return new Palette.Info(loot, mob, torch, mergedTag.isEmpty() ? null : Collections.unmodifiableMap(mergedTag));
    }

    private Information toInformation(Palette.Info info) {
        if (info == null) {
            return null;
        }
        return new Information(info.loot(), info.mob(), info.torch(), info.tag());
    }

    private ResourceLocation resolveLocation(ResourceLocation owner, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return new ResourceLocation(normalized);
        }
        return new ResourceLocation(owner.getNamespace(), normalized);
    }

    private boolean isCharacterReference(String value) {
        return value != null && value.length() == 1;
    }

    private Palette findReferencedPalette(ResourceLocation owner, String raw) {
        ResourceLocation location = resolveLocation(owner, raw);
        if (location == null) {
            return null;
        }

        Palette referenced = AssetRegistries.PALETTES.get(null, location);
        if (referenced != null) {
            return referenced;
        }

        if (!raw.contains(":") && !"lostcities".equals(location.getNamespace())) {
            Palette fallback = AssetRegistries.PALETTES.get(null, new ResourceLocation("lostcities", raw.toLowerCase(Locale.ROOT)));
            if (fallback != null) {
                return fallback;
            }
        }

        if (!raw.contains(":")) {
            String prefixed = raw.toLowerCase(Locale.ROOT);
            if (!prefixed.startsWith("palette_")) {
                prefixed = "palette_" + prefixed;
                Palette namespaced = AssetRegistries.PALETTES.get(null, new ResourceLocation(location.getNamespace(), prefixed));
                if (namespaced != null) {
                    return namespaced;
                }
                if (!"lostcities".equals(location.getNamespace())) {
                    return AssetRegistries.PALETTES.get(null, new ResourceLocation("lostcities", prefixed));
                }
            }
        }
        return null;
    }

    private Palette.Entry toEntryFromCompiled(char character, CompiledEntry compiled) {
        if (compiled == null || compiled.randomTable == null) {
            return null;
        }

        Map<String, Integer> weights = new LinkedHashMap<>();
        for (String block : compiled.randomTable) {
            if (block == null || block.isBlank()) {
                continue;
            }
            weights.merge(block, 1, Integer::sum);
        }

        List<Palette.WeightedBlock> blocks = new ArrayList<>();
        for (Map.Entry<String, Integer> weight : weights.entrySet()) {
            blocks.add(new Palette.WeightedBlock(weight.getKey(), Math.max(1, weight.getValue())));
        }

        return new Palette.Entry(
                character,
                Collections.unmodifiableList(blocks),
                null,
                null,
                compiled.damaged,
                fromInformation(compiled.information));
    }

    private Palette.Info fromInformation(Information information) {
        if (information == null) {
            return null;
        }
        return new Palette.Info(
                information.loot(),
                information.mob(),
                information.torch(),
                information.tag());
    }

    public Set<Character> getCharacters() {
        return palette.keySet();
    }

    public boolean isDefined(Character c) {
        return c != null && palette.containsKey(c);
    }

    public String get(char c) {
        CompiledEntry entry = palette.get(c);
        if (entry == null) {
            return null;
        }
        return entry.lookup(0);
    }

    public String get(char c, Random rand) {
        CompiledEntry entry = palette.get(c);
        if (entry == null) {
            return null;
        }
        int index = rand == null ? 0 : rand.nextInt(RANDOM_TABLE_SIZE);
        return entry.lookup(index);
    }

    public Set<String> getAll(char c) {
        CompiledEntry entry = palette.get(c);
        if (entry == null) {
            return Collections.emptySet();
        }
        Set<String> unique = new LinkedHashSet<>();
        Collections.addAll(unique, entry.randomTable);
        return Collections.unmodifiableSet(unique);
    }

    public String getDamaged(char c) {
        CompiledEntry entry = palette.get(c);
        return entry == null ? null : entry.damaged;
    }

    public Information getInformation(char c) {
        CompiledEntry entry = palette.get(c);
        return entry == null ? null : entry.information;
    }

    public record Information(String loot, String mob, boolean torch, Map<String, Object> tag) {
    }

    private static final class CompiledEntry {
        private final String[] randomTable;
        private final String damaged;
        private final Information information;

        private CompiledEntry(List<Palette.WeightedBlock> blocks, String damaged, Information information) {
            this.randomTable = buildRandomTable(blocks);
            this.damaged = damaged;
            this.information = information;
        }

        private String lookup(int index) {
            if (index < 0 || index >= randomTable.length) {
                return randomTable[0];
            }
            return randomTable[index];
        }

        private static String[] buildRandomTable(List<Palette.WeightedBlock> blocks) {
            String[] table = new String[RANDOM_TABLE_SIZE];
            if (blocks == null || blocks.isEmpty()) {
                return table;
            }

            if (blocks.size() == 1) {
                String block = blocks.get(0).block();
                for (int i = 0; i < table.length; i++) {
                    table[i] = block;
                }
                return table;
            }

            int total = 0;
            List<Palette.WeightedBlock> normalized = new ArrayList<>();
            for (Palette.WeightedBlock block : blocks) {
                if (block == null || block.block() == null || block.block().isBlank()) {
                    continue;
                }
                int weight = Math.max(1, block.weight());
                total += weight;
                normalized.add(new Palette.WeightedBlock(block.block(), weight));
            }
            if (normalized.isEmpty()) {
                return table;
            }
            if (total <= 0) {
                total = normalized.size();
            }

            for (int i = 0; i < RANDOM_TABLE_SIZE; i++) {
                int target = (int) Math.floor(((double) i / RANDOM_TABLE_SIZE) * total);
                int cursor = 0;
                String selected = normalized.get(normalized.size() - 1).block();
                for (Palette.WeightedBlock block : normalized) {
                    cursor += block.weight();
                    if (target < cursor) {
                        selected = block.block();
                        break;
                    }
                }
                table[i] = selected;
            }
            return table;
        }
    }
}
