package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.ILostCityBuilding;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.regassets.BuildingRE;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;
import com.during.cityloader.worldgen.lost.regassets.data.ConditionTest;
import com.during.cityloader.worldgen.lost.regassets.data.PartRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * 建筑类
 */
public class Building implements ILostCityAsset, ILostCityBuilding {

    private final ResourceLocation name;

    private final int minFloors;
    private final int minCellars;
    private final int maxFloors;
    private final int maxCellars;

    private final String filler;
    private final String rubble;
    private final String refPalette;
    private final Palette localPalette;
    private final boolean allowDoors;
    private final boolean allowFillers;
    private final boolean overrideFloors;
    private final float prefersLonely;

    private final List<WeightedPart> parts = new ArrayList<>();
    private final List<WeightedPart> parts2 = new ArrayList<>();

    public Building(BuildingRE object) {
        this.name = object.getRegistryName();
        this.minFloors = object.getMinFloors();
        this.minCellars = object.getMinCellars();
        this.maxFloors = object.getMaxFloors();
        this.maxCellars = object.getMaxCellars();

        this.filler = object.getFiller();
        this.rubble = object.getRubble();
        this.refPalette = object.getRefPalette();
        this.allowDoors = object.getAllowDoors() == null || object.getAllowDoors();
        this.allowFillers = object.getAllowFillers() == null || object.getAllowFillers();
        this.overrideFloors = object.getOverrideFloors() != null && object.getOverrideFloors();
        this.prefersLonely = object.getPrefersLonely() == null ? 0.0f : object.getPrefersLonely();

        PaletteRE inlinePalette = object.getLocalPalette();
        if (inlinePalette != null) {
            inlinePalette.setRegistryName(new ResourceLocation(name.getNamespace(), name.getPath() + "$inline"));
            this.localPalette = new Palette(inlinePalette);
        } else {
            this.localPalette = null;
        }

        readParts(this.parts, object.getParts());
        readParts(this.parts2, object.getParts2());
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    @Override
    public int getMinFloors() {
        return minFloors;
    }

    @Override
    public int getMaxFloors() {
        return maxFloors;
    }

    @Override
    public int getMinCellars() {
        return minCellars;
    }

    @Override
    public int getMaxCellars() {
        return maxCellars;
    }

    public String getFiller() {
        return filler;
    }

    public String getRubble() {
        return rubble;
    }

    public String getRefPalette() {
        return refPalette;
    }

    public Palette getLocalPalette() {
        return localPalette;
    }

    public boolean isAllowDoors() {
        return allowDoors;
    }

    public boolean isAllowFillers() {
        return allowFillers;
    }

    public boolean isOverrideFloors() {
        return overrideFloors;
    }

    public float getPrefersLonely() {
        return prefersLonely;
    }

    public String getRandomPart(Random random, ConditionContext info) {
        PartSelection selection = getRandomPartRef(random, info);
        return selection == null ? null : selection.partName();
    }

    public String getRandomPart2(Random random, ConditionContext info) {
        PartSelection selection = getRandomPart2Ref(random, info);
        return selection == null ? null : selection.partName();
    }

    public PartSelection getRandomPartRef(Random random, ConditionContext info) {
        return pickPart(parts, random, info);
    }

    public PartSelection getRandomPart2Ref(Random random, ConditionContext info) {
        return pickPart(parts2, random, info);
    }

    public List<String> getPartNames() {
        List<String> names = new ArrayList<>();
        for (WeightedPart part : parts) {
            if (part != null && part.partName() != null && !part.partName().isBlank()) {
                names.add(part.partName());
            }
        }
        return Collections.unmodifiableList(names);
    }

    public List<String> getPartNames2() {
        List<String> names = new ArrayList<>();
        for (WeightedPart part : parts2) {
            if (part != null && part.partName() != null && !part.partName().isBlank()) {
                names.add(part.partName());
            }
        }
        return Collections.unmodifiableList(names);
    }

    private PartSelection pickPart(List<WeightedPart> candidates, Random random, ConditionContext info) {
        int total = 0;
        List<WeightedPart> matched = new ArrayList<>();
        for (WeightedPart part : candidates) {
            if (part.test().test(info)) {
                int weight = Math.max(1, Math.round(part.factor() * 100));
                total += weight;
                matched.add(part);
            }
        }
        if (matched.isEmpty()) {
            return null;
        }
        if (matched.size() == 1 || total <= 0 || random == null) {
            WeightedPart selected = matched.get(0);
            return new PartSelection(selected.partName(), selected.transform());
        }

        int roll = random.nextInt(total);
        int current = 0;
        for (WeightedPart part : matched) {
            current += Math.max(1, Math.round(part.factor() * 100));
            if (roll < current) {
                return new PartSelection(part.partName(), part.transform());
            }
        }
        WeightedPart selected = matched.get(matched.size() - 1);
        return new PartSelection(selected.partName(), selected.transform());
    }

    private void readParts(List<WeightedPart> target, List<PartRef> refs) {
        target.clear();
        if (refs == null) {
            return;
        }
        for (PartRef ref : refs) {
            if (ref == null || ref.getPart() == null || ref.getPart().isBlank()) {
                continue;
            }
            ConditionTest testDefinition = ref.getCondition();
            Predicate<ConditionContext> test = testDefinition == null
                    ? context -> true
                    : ConditionContext.parseTest(testDefinition);
            float factor = ref.getWeight() <= 0.0f ? 1.0f : ref.getWeight();
            Transform transform = decodeTransform(ref.getTransformCode());
            target.add(new WeightedPart(test, ref.getPart(), factor, transform));
        }
    }

    private Transform decodeTransform(Integer transformCode) {
        if (transformCode == null) {
            return Transform.ROTATE_NONE;
        }
        return Transform.fromCode(transformCode);
    }

    private record WeightedPart(Predicate<ConditionContext> test,
                                String partName,
                                float factor,
                                Transform transform) {
    }

    public record PartSelection(String partName, Transform transform) {
    }
}
