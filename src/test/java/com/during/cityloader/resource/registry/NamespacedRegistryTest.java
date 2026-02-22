package com.during.cityloader.resource.registry;

import com.during.cityloader.util.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NamespacedRegistry测试
 * 
 * @author During
 * @since 1.5.0
 */
public class NamespacedRegistryTest {

    // 测试用的简单注册表实现
    private static class TestRegistry extends NamespacedRegistry<String> {
        public TestRegistry(boolean threadSafe) {
            super(threadSafe);
        }

        @Override
        protected String getRegistryName() {
            return "测试注册表";
        }
    }

    private TestRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new TestRegistry(false);
    }

    @Test
    public void testRegisterWithNamespace() {
        ResourceLocation id = new ResourceLocation("minecraft:stone");
        registry.register(id, "Stone Resource");

        assertNotNull(registry.get(id));
        assertEquals("Stone Resource", registry.get(id));
    }

    @Test
    public void testRegisterDefaultNamespace() {
        // 不带命名空间的ID应该使用默认命名空间 "lostcities"
        registry.register("brick_mix", "Brick Mix Resource");

        // 使用默认命名空间查询
        assertEquals("Brick Mix Resource", registry.get("brick_mix"));
        assertEquals("Brick Mix Resource", registry.get("lostcities:brick_mix"));
        assertEquals("Brick Mix Resource", registry.get(new ResourceLocation("lostcities:brick_mix")));
    }

    @Test
    public void testNamespaceIsolation() {
        // 注册minecraft:stone
        registry.register("minecraft:stone", "Minecraft Stone");
        // 注册lostcities:stone
        registry.register("lostcities:stone", "LostCities Stone");

        // 两者应该互不冲突
        assertEquals("Minecraft Stone", registry.get("minecraft:stone"));
        assertEquals("LostCities Stone", registry.get("lostcities:stone"));

        // 默认命名空间下的stone
        assertEquals("LostCities Stone", registry.get("stone"));
    }

    @Test
    public void testDuplicateRegistration() {
        registry.register("minecraft:stone", "First");

        // 重复注册应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register("minecraft:stone", "Second");
        });
    }

    @Test
    public void testGetNonExistent() {
        assertNull(registry.get("nonexistent:resource"));
        assertNull(registry.get("nonexistent"));
        assertNull(registry.get(new ResourceLocation("test:missing")));
    }

    @Test
    public void testContains() {
        registry.register("minecraft:diamond", "Diamond");

        assertTrue(registry.contains("minecraft:diamond"));
        assertTrue(registry.contains(new ResourceLocation("minecraft:diamond")));
        assertTrue(registry.contains("minecraft", "diamond"));
        assertTrue(registry.exists("minecraft:diamond"));

        assertFalse(registry.contains("minecraft:gold"));
        assertFalse(registry.contains("gold"));
    }

    @Test
    public void testClear() {
        registry.register("minecraft:stone", "Stone");
        registry.register("lostcities:brick", "Brick");

        assertEquals(2, registry.size());

        registry.clear();

        assertEquals(0, registry.size());
        assertNull(registry.get("minecraft:stone"));
        assertNull(registry.get("lostcities:brick"));
    }

    @Test
    public void testClearNamespace() {
        registry.register("minecraft:stone", "Stone");
        registry.register("minecraft:diamond", "Diamond");
        registry.register("lostcities:brick", "Brick");

        assertEquals(3, registry.size());
        assertEquals(2, registry.sizeOf("minecraft"));

        // 清空minecraft命名空间
        registry.clearNamespace("minecraft");

        assertEquals(1, registry.size());
        assertEquals(0, registry.sizeOf("minecraft"));
        assertEquals(1, registry.sizeOf("lostcities"));

        assertNull(registry.get("minecraft:stone"));
        assertNotNull(registry.get("lostcities:brick"));
    }

    @Test
    public void testSize() {
        assertEquals(0, registry.size());

        registry.register("minecraft:a", "A");
        assertEquals(1, registry.size());

        registry.register("minecraft:b", "B");
        assertEquals(2, registry.size());

        registry.register("lostcities:c", "C");
        assertEquals(3, registry.size());
    }

    @Test
    public void testSizeOf() {
        registry.register("minecraft:a", "A");
        registry.register("minecraft:b", "B");
        registry.register("lostcities:c", "C");

        assertEquals(2, registry.sizeOf("minecraft"));
        assertEquals(1, registry.sizeOf("lostcities"));
        assertEquals(0, registry.sizeOf("nonexistent"));
    }

    @Test
    public void testGetNamespaces() {
        registry.register("minecraft:stone", "Stone");
        registry.register("lostcities:brick", "Brick");
        registry.register("custom:wood", "Wood");

        Set<String> namespaces = registry.getNamespaces();
        assertEquals(3, namespaces.size());
        assertTrue(namespaces.contains("minecraft"));
        assertTrue(namespaces.contains("lostcities"));
        assertTrue(namespaces.contains("custom"));
    }

    @Test
    public void testGetAll() {
        registry.register("minecraft:stone", "Stone");
        registry.register("lostcities:brick", "Brick");
        registry.register("custom:wood", "Wood");

        Collection<String> all = registry.getAll();
        assertEquals(3, all.size());
        assertTrue(all.contains("Stone"));
        assertTrue(all.contains("Brick"));
        assertTrue(all.contains("Wood"));
    }

    @Test
    public void testGetAllFrom() {
        registry.register("minecraft:stone", "Stone");
        registry.register("minecraft:diamond", "Diamond");
        registry.register("lostcities:brick", "Brick");

        Collection<String> minecraftResources = registry.getAllFrom("minecraft");
        assertEquals(2, minecraftResources.size());
        assertTrue(minecraftResources.contains("Stone"));
        assertTrue(minecraftResources.contains("Diamond"));

        Collection<String> lostcitiesResources = registry.getAllFrom("lostcities");
        assertEquals(1, lostcitiesResources.size());
        assertTrue(lostcitiesResources.contains("Brick"));

        Collection<String> emptyResources = registry.getAllFrom("nonexistent");
        assertEquals(0, emptyResources.size());
    }

    @Test
    public void testRemove() {
        registry.register("minecraft:stone", "Stone");
        registry.register("lostcities:brick", "Brick");

        String removed = registry.remove("minecraft:stone");
        assertEquals("Stone", removed);
        assertNull(registry.get("minecraft:stone"));
        assertEquals(1, registry.size());

        // 移除不存在的资源
        String notRemoved = registry.remove("nonexistent:resource");
        assertNull(notRemoved);
    }

    @Test
    public void testRemoveWithResourceLocation() {
        registry.register("minecraft:diamond", "Diamond");

        ResourceLocation id = new ResourceLocation("minecraft:diamond");
        String removed = registry.remove(id);

        assertEquals("Diamond", removed);
        assertNull(registry.get(id));
    }

    @Test
    public void testStatistics() {
        registry.register("minecraft:stone", "Stone");
        registry.register("minecraft:diamond", "Diamond");
        registry.register("lostcities:brick", "Brick");

        String stats = registry.getStatistics();

        assertTrue(stats.contains("测试注册表"));
        assertTrue(stats.contains("总资源数: 3"));
        assertTrue(stats.contains("命名空间数: 2"));
        assertTrue(stats.contains("[minecraft]: 2 个资源"));
        assertTrue(stats.contains("[lostcities]: 1 个资源"));
    }

    @Test
    public void testThreadSafeRegistry() {
        TestRegistry threadSafeRegistry = new TestRegistry(true);

        // 基本功能应该相同
        threadSafeRegistry.register("minecraft:stone", "Stone");
        assertEquals("Stone", threadSafeRegistry.get("minecraft:stone"));
    }

    @Test
    public void testNullHandling() {
        // null ResourceLocation
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register((ResourceLocation) null, "Resource");
        });

        // null resource
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register("minecraft:stone", null);
        });

        // null查询应该返回null而不是抛异常
        assertNull(registry.get((ResourceLocation) null));
        assertNull(registry.get((String) null));
    }
}
