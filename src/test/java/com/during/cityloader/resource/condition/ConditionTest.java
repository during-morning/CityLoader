package com.during.cityloader.resource.condition;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Condition系统单元测试
 * 
 * @author During
 * @since 1.5.0
 */
public class ConditionTest {

    @Test
    public void testConditionContextIsTop() {
        // 10层建筑，第9层（索引9）是楼顶
        ConditionContext context = ConditionContext.forBuilding(
                72, 9, 0, 10, "floor_9", null, "apartment");

        assertTrue(context.isTop());
        assertFalse(context.isGround());
        assertFalse(context.isCellar());
    }

    @Test
    public void testConditionContextIsGround() {
        ConditionContext context = ConditionContext.forBuilding(
                64, 0, 0, 10, "floor_0", null, "apartment");

        assertFalse(context.isTop());
        assertTrue(context.isGround());
        assertFalse(context.isCellar());
    }

    @Test
    public void testConditionContextIsCellar() {
        ConditionContext context = ConditionContext.forBuilding(
                60, -1, 2, 10, "cellar_1", null, "apartment");

        assertFalse(context.isTop());
        assertFalse(context.isGround());
        assertTrue(context.isCellar());
    }

    @Test
    public void testConditionEntryTopFilter() {
        ConditionEntry<String> entry = new ConditionEntry<>(
                10.0, "test_value", true, null, null, null, null, null);

        // 楼顶上下文
        ConditionContext topContext = ConditionContext.forBuilding(
                72, 9, 0, 10, null, null, null);
        assertTrue(entry.test(topContext));

        // 非楼顶上下文
        ConditionContext nonTopContext = ConditionContext.forBuilding(
                68, 5, 0, 10, null, null, null);
        assertFalse(entry.test(nonTopContext));
    }

    @Test
    public void testConditionEntryRangeFilter() {
        // 楼层范围 4-100
        ConditionEntry<String> entry = new ConditionEntry<>(
                8.0, "test_value", null, null, null, "4,100", null, null);

        // Floor 5应该匹配
        ConditionContext ctx1 = ConditionContext.forBuilding(69, 5, 0, 10, null, null, null);
        assertTrue(entry.test(ctx1));

        // Floor 3不匹配
        ConditionContext ctx2 = ConditionContext.forBuilding(67, 3, 0, 10, null, null, null);
        assertFalse(entry.test(ctx2));

        // Floor 4匹配（边界）
        ConditionContext ctx3 = ConditionContext.forBuilding(68, 4, 0, 10, null, null, null);
        assertTrue(entry.test(ctx3));
    }

    @Test
    public void testConditionEntryInpartFilter() {
        ConditionEntry<String> entry = new ConditionEntry<>(
                20.0, "rail_dungeon_loot", null, null, null, null, "rail_dungeon1", null);

        // 在rail_dungeon1部件中
        ConditionContext ctx1 = new ConditionContext(
                64, 0, 0, 1, "rail_dungeon1", null, "dungeon", null);
        assertTrue(entry.test(ctx1));

        // 不在rail_dungeon1部件中
        ConditionContext ctx2 = new ConditionContext(
                64, 0, 0, 1, "other_part", null, "dungeon", null);
        assertFalse(entry.test(ctx2));
    }

    @Test
    public void testConditionGetRandomValue() {
        List<ConditionEntry<String>> entries = new ArrayList<>();
        entries.add(new ConditionEntry<>(10.0, "value1"));
        entries.add(new ConditionEntry<>(20.0, "value2"));
        entries.add(new ConditionEntry<>(30.0, "value3"));

        Condition<String> condition = new Condition<>("test", entries);

        ConditionContext context = ConditionContext.forBuilding(64, 0, 0, 1, null, null, null);
        Random random = new Random(12345L);

        String value = condition.getRandomValue(random, context);
        assertNotNull(value);
        assertTrue(value.equals("value1") || value.equals("value2") || value.equals("value3"));
    }

    @Test
    public void testConditionFilteredSelection() {
        List<ConditionEntry<String>> entries = new ArrayList<>();
        // 只在楼顶有效
        entries.add(new ConditionEntry<>(10.0, "top_only", true, null, null, null, null, null));
        // 只在地面有效
        entries.add(new ConditionEntry<>(10.0, "ground_only", null, true, null, null, null, null));
        // 无条件
        entries.add(new ConditionEntry<>(10.0, "always", null, null, null, null, null, null));

        Condition<String> condition = new Condition<>("test", entries);

        // 楼顶上下文
        ConditionContext topContext = ConditionContext.forBuilding(72, 9, 0, 10, null, null, null);
        Random random = new Random(42L);
        String value = condition.getRandomValue(random, topContext);

        // 应该返回top_only或always，不会是ground_only
        assertTrue(value.equals("top_only") || value.equals("always"));
    }

    @Test
    public void testConditionValidation() {
        List<ConditionEntry<String>> validEntries = new ArrayList<>();
        validEntries.add(new ConditionEntry<>(1.0, "valid"));

        Condition<String> validCondition = new Condition<>("test", validEntries);
        assertTrue(validCondition.validate());

        // 空ID
        Condition<String> invalidId = new Condition<>("", validEntries);
        assertFalse(invalidId.validate());

        // 空条目
        Condition<String> invalidEmpty = new Condition<>("test", new ArrayList<>());
        assertFalse(invalidEmpty.validate());

        // 负权重
        List<ConditionEntry<String>> negativeWeight = new ArrayList<>();
        negativeWeight.add(new ConditionEntry<>(-1.0, "invalid"));
        Condition<String> invalidWeight = new Condition<>("test", negativeWeight);
        assertFalse(invalidWeight.validate());
    }

    @Test
    public void testConditionRegistryOperations() {
        ConditionRegistry registry = new ConditionRegistry();

        List<ConditionEntry<String>> entries = new ArrayList<>();
        entries.add(new ConditionEntry<>(1.0, "test"));
        Condition<String> condition = new Condition<>("test_condition", entries);

        registry.register(condition);

        assertTrue(registry.exists("test_condition"));
        assertEquals(1, registry.getCount());
        assertNotNull(registry.get("test_condition"));

        registry.clear();
        assertEquals(0, registry.getCount());
    }
}
