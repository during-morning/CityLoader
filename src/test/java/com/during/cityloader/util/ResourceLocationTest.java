package com.during.cityloader.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceLocation测试
 * 
 * @author During
 * @since 1.5.0
 */
public class ResourceLocationTest {

    @Test
    public void testParseValid() {
        ResourceLocation rl = new ResourceLocation("minecraft:stone");
        assertEquals("minecraft", rl.getNamespace());
        assertEquals("stone", rl.getPath());
        assertEquals("minecraft:stone", rl.toString());
    }

    @Test
    public void testParseDefaultNamespace() {
        ResourceLocation rl = new ResourceLocation("stone");
        assertEquals("lostcities", rl.getNamespace());
        assertEquals("stone", rl.getPath());
        assertEquals("lostcities:stone", rl.toString());
        assertTrue(rl.isDefaultNamespace());
    }

    @Test
    public void testParsePath() {
        ResourceLocation rl = new ResourceLocation("lostcities:chests/raildungeonchest");
        assertEquals("lostcities", rl.getNamespace());
        assertEquals("chests/raildungeonchest", rl.getPath());
    }

    @Test
    public void testConstructor() {
        ResourceLocation rl = new ResourceLocation("minecraft", "diamond_block");
        assertEquals("minecraft", rl.getNamespace());
        assertEquals("diamond_block", rl.getPath());
    }

    @Test
    public void testCaseInsensitive() {
        ResourceLocation rl1 = new ResourceLocation("Minecraft:Stone");
        ResourceLocation rl2 = new ResourceLocation("minecraft:stone");
        assertEquals(rl1, rl2);
        assertEquals(rl1.hashCode(), rl2.hashCode());
    }

    @Test
    public void testInvalidFormat() {
        // 空ID
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation(""));
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation(null));

        // 空命名空间
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation("", "path"));

        // 空路径
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation("namespace", ""));

        // 格式错误:path"
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation(":stone"));

        // 命名空间后无路径
        assertThrows(IllegalArgumentException.class, () -> new ResourceLocation("minecraft:"));
    }

    @Test
    public void testStaticParse() {
        ResourceLocation valid = ResourceLocation.parse("minecraft:stone");
        assertNotNull(valid);
        assertEquals("minecraft", valid.getNamespace());

        ResourceLocation invalid = ResourceLocation.parse(":invalid");
        assertNull(invalid);
    }

    @Test
    public void testIsValid() {
        assertTrue(ResourceLocation.isValid("minecraft:stone"));
        assertTrue(ResourceLocation.isValid("lostcities:chests/simple"));
        assertTrue(ResourceLocation.isValid("stone")); // 默认命名空间

        assertFalse(ResourceLocation.isValid(":invalid"));
        assertFalse(ResourceLocation.isValid(""));
        assertFalse(ResourceLocation.isValid(null));
        assertFalse(ResourceLocation.isValid("namespace:"));
    }

    @Test
    public void testEquals() {
        ResourceLocation rl1 = new ResourceLocation("minecraft:stone");
        ResourceLocation rl2 = new ResourceLocation("minecraft", "stone");
        ResourceLocation rl3 = new ResourceLocation("lostcities:stone");

        assertEquals(rl1, rl2);
        assertNotEquals(rl1, rl3);
        assertNotEquals(rl1, null);
        assertNotEquals(rl1, "not a ResourceLocation");
    }

    @Test
    public void testHashCode() {
        ResourceLocation rl1 = new ResourceLocation("minecraft:stone");
        ResourceLocation rl2 = new ResourceLocation("minecraft", "stone");

        assertEquals(rl1.hashCode(), rl2.hashCode());
    }
}
