package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.VariantRE;
import com.during.cityloader.worldgen.lost.regassets.data.BlockChoice;
import com.during.cityloader.worldgen.lost.regassets.data.BlockEntry;
import com.during.cityloader.worldgen.lost.regassets.data.PaletteEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("新 CompiledPalette 测试")
class CompiledPaletteTest {

    @AfterEach
    void tearDown() {
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("应支持 variant 加权方块展开")
    void shouldResolveVariantBlocks() {
        AssetRegistries.reset();

        VariantRE variantRE = new VariantRE();
        variantRE.setBlocks(List.of(
                new BlockEntry(1, "minecraft:stone"),
                new BlockEntry(1, "minecraft:mossy_stone_bricks")
        ));
        AssetRegistries.VARIANTS.register(new ResourceLocation("test", "v1"), variantRE);

        PaletteRE paletteRE = new PaletteRE();
        PaletteEntry entry = PaletteEntry.variantRef("A", "test:v1");
        paletteRE.setPaletteEntries(List.of(entry));
        paletteRE.setRegistryName(new ResourceLocation("test", "p1"));

        Palette palette = new Palette(paletteRE);
        CompiledPalette compiled = new CompiledPalette(palette);

        assertTrue(compiled.isDefined('A'));
        assertEquals(2, compiled.getAll('A').size(), "variant 应展开为两个候选方块");
    }

    @Test
    @DisplayName("应支持 frompalette 继承与 damaged 元数据")
    void shouldResolveFromPaletteAndDamaged() {
        AssetRegistries.reset();

        PaletteRE baseRE = new PaletteRE();
        PaletteEntry baseEntry = PaletteEntry.simpleBlock("B", "minecraft:stone_bricks");
        baseEntry.setDamaged("minecraft:cracked_stone_bricks");
        baseRE.setPaletteEntries(List.of(baseEntry));
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "base"), baseRE);

        PaletteRE childRE = new PaletteRE();
        PaletteEntry childEntry = new PaletteEntry();
        childEntry.setCharacter("B");
        childEntry.setFromPalette("test:base");
        childEntry.setBlocks(List.of(
                new BlockChoice(1, "minecraft:stone_bricks"),
                new BlockChoice(1, "minecraft:mossy_stone_bricks")
        ));
        childEntry.setLoot("minecraft:chests/simple_dungeon");
        childEntry.setTag(Map.of("foo", "bar"));
        childRE.setPaletteEntries(List.of(childEntry));
        childRE.setRegistryName(new ResourceLocation("test", "child"));

        CompiledPalette compiled = new CompiledPalette(new Palette(childRE));

        assertTrue(compiled.isDefined('B'));
        assertEquals("minecraft:cracked_stone_bricks", compiled.getDamaged('B'));
        assertEquals(2, compiled.getAll('B').size());
        assertNotNull(compiled.getInformation('B'));
        assertEquals("minecraft:chests/simple_dungeon", compiled.getInformation('B').loot());
        assertEquals("bar", compiled.getInformation('B').tag().get("foo"));
    }

    @Test
    @DisplayName("随机访问应命中 128 槽预计算表")
    void shouldUsePrecomputedRandomTable() {
        AssetRegistries.reset();

        PaletteRE paletteRE = new PaletteRE();
        PaletteEntry entry = new PaletteEntry();
        entry.setCharacter("R");
        entry.setBlocks(List.of(
                new BlockChoice(1, "minecraft:stone"),
                new BlockChoice(1, "minecraft:diorite")
        ));
        paletteRE.setPaletteEntries(List.of(entry));
        paletteRE.setRegistryName(new ResourceLocation("test", "random"));

        CompiledPalette compiled = new CompiledPalette(new Palette(paletteRE));
        Random random = new Random(123L);

        for (int i = 0; i < 32; i++) {
            String block = compiled.get('R', random);
            assertTrue("minecraft:stone".equals(block) || "minecraft:diorite".equals(block));
        }
    }

    @Test
    @DisplayName("应支持 frompalette 字符别名继承（如 frompalette='#'）")
    void shouldResolveFromPaletteCharacterAlias() {
        AssetRegistries.reset();

        PaletteRE baseRE = new PaletteRE();
        PaletteEntry baseEntry = PaletteEntry.simpleBlock("#", "minecraft:stone_bricks");
        baseEntry.setDamaged("minecraft:cracked_stone_bricks");
        baseRE.setPaletteEntries(List.of(baseEntry));
        baseRE.setRegistryName(new ResourceLocation("lostcities", "base_char"));

        PaletteRE childRE = new PaletteRE();
        PaletteEntry childEntry = new PaletteEntry();
        childEntry.setCharacter("@");
        childEntry.setFromPalette("#");
        childRE.setPaletteEntries(List.of(childEntry));
        childRE.setRegistryName(new ResourceLocation("lostcities", "child_char"));

        CompiledPalette baseCompiled = new CompiledPalette(new Palette(baseRE));
        CompiledPalette merged = new CompiledPalette(baseCompiled, new Palette(childRE));

        assertTrue(merged.isDefined('@'));
        assertEquals("minecraft:stone_bricks", merged.get('@'));
        assertEquals("minecraft:cracked_stone_bricks", merged.getDamaged('@'));
    }
}
