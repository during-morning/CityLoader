package com.during.cityloader.resource;

import com.during.cityloader.resource.registry.VariantRegistry;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class CompiledPaletteTest {

    private Palette palette;
    private VariantRegistry variantRegistry;
    private Random random;

    @BeforeEach
    public void setUp() {
        palette = new Palette("test_palette");
        variantRegistry = new VariantRegistry();
        random = new Random();
    }

    @Test
    public void testSimpleMapping() {
        palette.addBlockMapping('a', new BlockMapping(Material.STONE));
        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);

        assertEquals(Material.STONE, compiled.get('a', random));
    }

    @Test
    public void testReferenceMapping() {
        palette.addBlockMapping('a', new BlockMapping(Material.STONE));
        palette.addBlockMapping('b', new BlockMapping('a')); // b references a

        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);

        assertEquals(Material.STONE, compiled.get('b', random));
    }

    @Test
    public void testVariantMapping() {
        Variant variant = new Variant("test_variant");
        variant.addBlock(Material.DIRT, 1);
        variantRegistry.register(variant);

        palette.addBlockMapping('v', new BlockMapping("test_variant", 1.0));

        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);

        assertEquals(Material.DIRT, compiled.get('v', random));
    }

    @Test
    public void testReferenceToVariant() {
        Variant variant = new Variant("test_variant");
        variant.addBlock(Material.COBBLESTONE, 1);
        variantRegistry.register(variant);

        palette.addBlockMapping('v', new BlockMapping("test_variant", 1.0));
        palette.addBlockMapping('r', new BlockMapping('v')); // r references v

        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);

        assertEquals(Material.COBBLESTONE, compiled.get('r', random));
    }

    @Test
    public void testCircularReference() {
        palette.addBlockMapping('a', new BlockMapping('b'));
        palette.addBlockMapping('b', new BlockMapping('a'));

        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);

        // Circular reference should resolve to AIR (unresolved)
        assertEquals(Material.AIR, compiled.get('a', random));
    }

    @Test
    public void testMissingMapping() {
        CompiledPalette compiled = new CompiledPalette(palette, variantRegistry);
        assertEquals(Material.AIR, compiled.get('z', random));
    }
}
