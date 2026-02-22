package com.during.cityloader.resource;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapping逻辑完整性测试
 * 
 * @author During
 * @since 1.5.0
 */
public class MappingLogicTest {

    @Test
    public void testBlockMappingWithInfo() {
        Info info = new Info(null, "chestloot", false, null);
        BlockMapping mapping = new BlockMapping(Material.CHEST, null, null, null, 1.0, info);

        assertNotNull(mapping.getInfo());
        assertTrue(mapping.hasInfo());
        assertEquals("chestloot", mapping.getInfo().getLoot());
    }

    @Test
    public void testBlockMappingWithoutInfo() {
        BlockMapping mapping = new BlockMapping(Material.STONE, null, null, null, 1.0);

        assertNull(mapping.getInfo());
        assertFalse(mapping.hasInfo());
    }

    @Test
    public void testBlockMappingWithVariant() {
        BlockMapping mapping = new BlockMapping(null, null, "brick_mix", null, 1.0);

        assertTrue(mapping.isVariant());
        assertEquals("brick_mix", mapping.getVariantId());
    }

    @Test
    public void testBlockMappingWithFromPalette() {
        BlockMapping mapping = new BlockMapping(null, null, null, 'x', 1.0);

        assertTrue(mapping.isReference());
        assertEquals('x', mapping.getFromPaletteChar());
    }

    @Test
    public void testBlockMappingWeight() {
        BlockMapping mapping1 = new BlockMapping(Material.STONE, null, null, null, 5.0);
        assertEquals(5.0, mapping1.getWeight(), 0.001);

        BlockMapping mapping2 = new BlockMapping(Material.DIRT, null, null, null, 1.0);
        assertEquals(1.0, mapping2.getWeight(), 0.001);
    }

    @Test
    public void testSpawnerInfo() {
        Info spawnerInfo = new Info("easymobs", null, false, null);
        BlockMapping spawner = new BlockMapping(Material.SPAWNER, null, null, null, 1.0, spawnerInfo);

        assertTrue(spawner.hasInfo());
        assertTrue(spawner.getInfo().isSpawner());
        assertEquals("easymobs", spawner.getInfo().getMobId());
    }

    @Test
    public void testChestInfo() {
        Info chestInfo = new Info(null, "chestloot", false, null);
        BlockMapping chest = new BlockMapping(Material.CHEST, null, null, null, 1.0, chestInfo);

        assertTrue(chest.hasInfo());
        assertTrue(chest.getInfo().isLootContainer());
        assertEquals("chestloot", chest.getInfo().getLoot());
    }

    @Test
    public void testTorchInfo() {
        Info torchInfo = new Info(null, null, true, null);
        BlockMapping torch = new BlockMapping(Material.TORCH, null, null, null, 1.0, torchInfo);

        assertTrue(torch.hasInfo());
        assertTrue(torch.getInfo().isTorch());
    }
}
