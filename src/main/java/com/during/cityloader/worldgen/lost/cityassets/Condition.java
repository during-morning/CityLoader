package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.ConditionRE;
import com.during.cityloader.worldgen.lost.regassets.data.ConditionPart;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * 条件类
 * 用于评估条件逻辑并根据条件选择值
 * 
 * @author During
 * @since 1.4.0
 */
public class Condition implements ILostCityAsset {

    private final ResourceLocation name;
    private final List<Pair<Predicate<ConditionContext>, Pair<Float, String>>> valueSelector = new ArrayList<>();

    /**
     * 从ConditionRE构造Condition对象
     * 
     * @param object ConditionRE注册实体
     */
    public Condition(ConditionRE object) {
        name = object.getRegistryName();
        for (ConditionPart cp : object.getValues()) {
            float factor = cp.getFactor();
            String value = cp.getValue();
            Predicate<ConditionContext> test = ConditionContext.parseTest(cp);
            valueSelector.add(Pair.of(test, Pair.of(factor, value)));
        }
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    /**
     * 根据条件上下文获取随机值
     * 
     * @param random 随机数生成器
     * @param info 条件上下文
     * @return 选中的值，如果没有匹配的条件则返回null
     */
    public String getRandomValue(Random random, ConditionContext info) {
        List<Pair<Float, String>> values = new ArrayList<>();
        for (Pair<Predicate<ConditionContext>, Pair<Float, String>> pair : valueSelector) {
            if (pair.getLeft().test(info)) {
                values.add(pair.getRight());
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        Pair<Float, String> randomFromList = getRandomFromList(random, values);
        if (randomFromList == null) {
            return null;
        } else {
            return randomFromList.getRight();
        }
    }

    /**
     * 从加权列表中随机选择一个元素
     * 
     * @param random 随机数生成器
     * @param list 加权列表
     * @return 选中的元素
     */
    private static Pair<Float, String> getRandomFromList(Random random, List<Pair<Float, String>> list) {
        if (list.isEmpty()) {
            return null;
        }
        float total = 0.0f;
        for (Pair<Float, String> pair : list) {
            total += pair.getLeft();
        }
        float r = random.nextFloat() * total;
        float current = 0.0f;
        for (Pair<Float, String> pair : list) {
            current += pair.getLeft();
            if (r <= current) {
                return pair;
            }
        }
        return list.get(list.size() - 1);
    }
}
