package com.during.cityloader.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NBT转换工具测试
 * 
 * @author During
 * @since 1.5.0
 */
public class NBTConverterTest {

    @Test
    public void testJsonObjectToNBT() {
        String json = "{\"key\":\"value\",\"number\":42}";
        Object result = NBTConverter.jsonToNBT(json);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("value", map.get("key"));
        assertEquals(42, map.get("number"));
    }

    @Test
    public void testJsonArrayToNBT() {
        String json = "[1,2,3,\"test\"]";
        Object result = NBTConverter.jsonToNBT(json);

        assertNotNull(result);
        assertTrue(result instanceof List);

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals("test", list.get(3));
    }

    @Test
    public void testPrimitiveTypes() {
        // String
        Object str = NBTConverter.jsonToNBT("\"hello\"");
        assertEquals("hello", str);

        // Integer
        Object num = NBTConverter.jsonToNBT("42");
        assertEquals(42, num);

        // Double
        Object dbl = NBTConverter.jsonToNBT("3.14");
        assertEquals(3.14, dbl);

        // Boolean
        Object bool = NBTConverter.jsonToNBT("true");
        assertEquals(true, bool);
    }

    @Test
    public void testNestedStructures() {
        String json = "{\"player\":{\"name\":\"Steve\",\"level\":10,\"inventory\":[\"sword\",\"pickaxe\"]}}";
        Object result = NBTConverter.jsonToNBT(json);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) result;

        @SuppressWarnings("unchecked")
        Map<String, Object> player = (Map<String, Object>) root.get("player");
        assertNotNull(player);
        assertEquals("Steve", player.get("name"));
        assertEquals(10, player.get("level"));

        @SuppressWarnings("unchecked")
        List<Object> inventory = (List<Object>) player.get("inventory");
        assertNotNull(inventory);
        assertEquals(2, inventory.size());
        assertEquals("sword", inventory.get(0));
        assertEquals("pickaxe", inventory.get(1));
    }

    @Test
    public void testNullAndEmpty() {
        assertNull(NBTConverter.jsonToNBT(null));
        assertNull(NBTConverter.jsonToNBT(""));
        assertNull(NBTConverter.jsonToNBT("   "));
        assertNull(NBTConverter.jsonToNBT("null"));
    }

    @Test
    public void testInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> {
            NBTConverter.jsonToNBT("{invalid json}");
        });
    }

    @Test
    public void testNBTToJson() {
        // Map → JSON
        String json1 = "{\"key\":\"value\",\"number\":42}";
        Object nbt1 = NBTConverter.jsonToNBT(json1);
        String restored1 = NBTConverter.nbtToJson(nbt1);
        assertTrue(restored1.contains("\"key\":\"value\""));
        assertTrue(restored1.contains("\"number\":42"));

        // List → JSON
        String json2 = "[1,2,3]";
        Object nbt2 = NBTConverter.jsonToNBT(json2);
        String restored2 = NBTConverter.nbtToJson(nbt2);
        assertEquals("[1,2,3]", restored2);

        // Primitive → JSON
        assertEquals("\"test\"", NBTConverter.nbtToJson("test"));
        assertEquals("42", NBTConverter.nbtToJson(42));
        assertEquals("true", NBTConverter.nbtToJson(true));
    }

    @Test
    public void testSpawnerTag() {
        // 真实刷怪笼NBT示例
        String spawnerTag = "{\"SpawnData\":{\"entity\":{\"id\":\"minecraft:zombie\"}},\"SpawnCount\":4,\"SpawnRange\":4}";
        Object nbt = NBTConverter.jsonToNBT(spawnerTag);

        assertNotNull(nbt);
        assertTrue(nbt instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) nbt;
        assertEquals(4, root.get("SpawnCount"));
        assertEquals(4, root.get("SpawnRange"));

        @SuppressWarnings("unchecked")
        Map<String, Object> spawnData = (Map<String, Object>) root.get("SpawnData");
        assertNotNull(spawnData);

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) spawnData.get("entity");
        assertEquals("minecraft:zombie", entity.get("id"));
    }
}
