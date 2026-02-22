package com.during.cityloader.resource;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Info系统单元测试
 * 
 * @author During
 * @since 1.5.0
 */
public class InfoTest {

    @Test
    public void testInfoCreation() {
        Info info = new Info("minecraft:zombie", null, false, null);

        assertEquals("minecraft:zombie", info.getMobId());
        assertNull(info.getLoot());
        assertFalse(info.isTorch());
        assertNull(info.getTag());
    }

    @Test
    public void testInfoWithLoot() {
        Info info = new Info(null, "chestloot", false, null);

        assertNull(info.getMobId());
        assertEquals("chestloot", info.getLoot());
        assertFalse(info.isTorch());
    }

    @Test
    public void testInfoWithTorch() {
        Info info = new Info(null, null, true, null);

        assertNull(info.getMobId());
        assertNull(info.getLoot());
        assertTrue(info.isTorch());
    }

    @Test
    public void testInfoWithTag() {
        String tagJson = "{\"key\":\"value\"}";
        Info info = new Info(null, null, false, tagJson);

        assertEquals(tagJson, info.getTag());
    }

    @Test
    public void testInfoHasMetadata() {
        Info infoWithMob = new Info("zombie", null, false, null);
        assertTrue(infoWithMob.hasMetadata());

        Info infoWithLoot = new Info(null, "chestloot", false, null);
        assertTrue(infoWithLoot.hasMetadata());

        Info infoWithTorch = new Info(null, null, true, null);
        assertTrue(infoWithTorch.hasMetadata());

        Info infoEmpty = new Info(null, null, false, null);
        assertFalse(infoEmpty.hasMetadata());
    }

    @Test
    public void testInfoIsSpawner() {
        Info spawner = new Info("easymobs", null, false, null);
        assertTrue(spawner.isSpawner());

        Info chest = new Info(null, "chestloot", false, null);
        assertFalse(chest.isSpawner());
    }

    @Test
    public void testInfoIsLootContainer() {
        Info chest = new Info(null, "chestloot", false, null);
        assertTrue(chest.isLootContainer());

        Info spawner = new Info("easymobs", null, false, null);
        assertFalse(spawner.isLootContainer());
    }

    @Test
    public void testInfoToString() {
        Info info = new Info("zombie", "chestloot", true, null);
        String str = info.toString();

        assertTrue(str.contains("mobId='zombie'"));
        assertTrue(str.contains("loot='chestloot'"));
        assertTrue(str.contains("isTorch=true"));
    }
}
