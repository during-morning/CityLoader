package com.during.cityloader.resource;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class VariantTest {

    @Test
    public void testWeightedSelection() {
        Variant variant = new Variant("test_variant");
        variant.addBlock(Material.STONE, 70);
        variant.addBlock(Material.COBBLESTONE, 30);

        assertEquals(100, variant.getTotalWeight());

        Random random = new Random(12345);
        int stoneCount = 0;
        int cobbleCount = 0;

        for (int i = 0; i < 1000; i++) {
            Material material = variant.getRandomBlock(random);
            if (material == Material.STONE) {
                stoneCount++;
            } else if (material == Material.COBBLESTONE) {
                cobbleCount++;
            }
        }

        // 验证大致比例 (70% vs 30%)
        assertTrue(stoneCount > 650 && stoneCount < 750);
        assertTrue(cobbleCount > 250 && cobbleCount < 350);
    }

    @Test
    public void testSingleBlock() {
        Variant variant = new Variant("single");
        variant.addBlock(Material.DIRT, 100);

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            assertEquals(Material.DIRT, variant.getRandomBlock(random));
        }
    }

    @Test
    public void testEmptyVariant() {
        Variant variant = new Variant("empty");
        assertEquals(Material.AIR, variant.getRandomBlock(new Random()));
    }

    @Test
    public void testValidation() {
        Variant v1 = new Variant(null);
        assertFalse(v1.validate());

        Variant v2 = new Variant("valid");
        assertFalse(v2.validate()); // Empty blocks

        v2.addBlock(Material.STONE, 1);
        assertTrue(v2.validate());

        Variant v3 = new Variant("invalid_weight");
        v3.addBlock(Material.STONE, 0);
        assertFalse(v3.validate());
    }
}
