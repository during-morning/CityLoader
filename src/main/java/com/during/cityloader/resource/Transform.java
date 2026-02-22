package com.during.cityloader.resource;

import java.util.Random;

/**
 * 部件变换枚举。
 * 对齐 LostCities Transform 的核心变换集合。
 */
public enum Transform {
    ROTATE_NONE,
    ROTATE_90,
    ROTATE_180,
    ROTATE_270,
    MIRROR_X,
    MIRROR_Z,
    MIRROR_90_X;

    public static Transform fromCode(int code) {
        return switch (code) {
            case 0 -> ROTATE_NONE;
            case 1 -> ROTATE_90;
            case 2 -> ROTATE_180;
            case 3 -> ROTATE_270;
            case 4 -> MIRROR_X;
            case 5 -> MIRROR_Z;
            case 6 -> MIRROR_90_X;
            default -> ROTATE_NONE;
        };
    }

    public static Transform randomRotation(Random random) {
        if (random == null) {
            return ROTATE_NONE;
        }
        return switch (random.nextInt(4)) {
            case 0 -> ROTATE_NONE;
            case 1 -> ROTATE_90;
            case 2 -> ROTATE_180;
            case 3 -> ROTATE_270;
            default -> ROTATE_NONE;
        };
    }

    public Transform getOpposite() {
        return switch (this) {
            case ROTATE_NONE -> ROTATE_NONE;
            case ROTATE_90 -> ROTATE_270;
            case ROTATE_180 -> ROTATE_180;
            case ROTATE_270 -> ROTATE_90;
            case MIRROR_X -> MIRROR_X;
            case MIRROR_Z -> MIRROR_Z;
            case MIRROR_90_X -> MIRROR_90_X;
        };
    }

    /**
     * 将源坐标系中的 x/z 映射到目标坐标系 x。
     *
     * @param x 源X坐标
     * @param z 源Z坐标
     * @param maxX 源X最大值（通常为 width-1）
     * @param maxZ 源Z最大值（通常为 depth-1）
     */
    public int mapX(int x, int z, int maxX, int maxZ) {
        return switch (this) {
            case ROTATE_NONE -> x;
            case ROTATE_90 -> z;
            case ROTATE_180 -> maxX - x;
            case ROTATE_270 -> maxZ - z;
            case MIRROR_X -> maxX - x;
            case MIRROR_Z -> x;
            case MIRROR_90_X -> z;
        };
    }

    /**
     * 将源坐标系中的 x/z 映射到目标坐标系 z。
     *
     * @param x 源X坐标
     * @param z 源Z坐标
     * @param maxX 源X最大值（通常为 width-1）
     * @param maxZ 源Z最大值（通常为 depth-1）
     */
    public int mapZ(int x, int z, int maxX, int maxZ) {
        return switch (this) {
            case ROTATE_NONE -> z;
            case ROTATE_90 -> maxX - x;
            case ROTATE_180 -> maxZ - z;
            case ROTATE_270 -> x;
            case MIRROR_X -> z;
            case MIRROR_Z -> maxZ - z;
            case MIRROR_90_X -> x;
        };
    }
}
