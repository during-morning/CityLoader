package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;

/**
 * 统一高度模型入口，避免各阶段重复硬编码高度规则。
 */
public final class GenerationHeightModel {

    public static final int FLOOR_HEIGHT = 6;

    private GenerationHeightModel() {
    }

    public static int cityGroundY(BuildingInfo info) {
        return info == null ? 0 : info.getCityGroundLevel();
    }

    public static int railY(BuildingInfo info, int railLevel, WorldStyle worldStyle) {
        if (info == null) {
            return 0;
        }
        return info.groundLevel + railLevel * FLOOR_HEIGHT + railPartOffset(worldStyle);
    }

    public static int railPartOffset(WorldStyle worldStyle) {
        if (worldStyle == null || worldStyle.getSettings() == null) {
            return 0;
        }
        Integer partHeight6 = worldStyle.getSettings().getRailPartHeight6();
        if (partHeight6 == null) {
            return 0;
        }
        int normalized = Math.max(1, partHeight6);
        return (normalized - 1) * FLOOR_HEIGHT;
    }
}
