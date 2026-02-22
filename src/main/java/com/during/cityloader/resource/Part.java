package com.during.cityloader.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * 部件类
 * 表示建筑的一个部分（如地板、墙壁、屋顶等）
 * 包含3D结构数据和调色板引用
 * 
 * @author During
 * @since 1.4.0
 */
public class Part {
    
    // 部件ID
    private final String id;
    
    // 使用的调色板ID列表
    private final List<String> paletteIds;
    
    // 部件尺寸
    private final int width;   // X轴
    private final int height;  // Y轴
    private final int depth;   // Z轴
    
    // 结构数据（3D字符数组）
    // structure[y][z][x] - 按层、行、列组织
    private final char[][][] structure;
    
    /**
     * 构造函数
     * 
     * @param id 部件ID
     * @param paletteIds 调色板ID列表
     * @param width 宽度
     * @param height 高度
     * @param depth 深度
     * @param structure 结构数据
     */
    public Part(String id, List<String> paletteIds, int width, int height, int depth, char[][][] structure) {
        this.id = id;
        this.paletteIds = paletteIds != null ? new ArrayList<>(paletteIds) : new ArrayList<>();
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.structure = structure;
    }
    
    /**
     * 获取指定位置的字符
     * 
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 对应位置的字符，如果越界返回空格
     */
    public char getCharAt(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            return ' '; // 越界返回空格
        }
        
        if (structure == null || structure[y] == null || structure[y][z] == null) {
            return ' ';
        }
        
        return structure[y][z][x];
    }
    
    /**
     * 旋转部件（顺时针90度）
     * 创建一个新的旋转后的Part对象
     * 
     * @return 旋转后的新Part对象
     */
    public Part rotate() {
        return rotate90();
    }

    /**
     * 旋转部件（顺时针90度）
     *
     * @return 旋转后的新Part对象
     */
    public Part rotate90() {
        return rotate(1);
    }

    /**
     * 旋转部件（顺时针180度）
     *
     * @return 旋转后的新Part对象
     */
    public Part rotate180() {
        return rotate(2);
    }

    /**
     * 旋转部件（顺时针270度）
     *
     * @return 旋转后的新Part对象
     */
    public Part rotate270() {
        return rotate(3);
    }

    /**
     * 旋转部件
     *
     * @param quarterTurns 顺时针旋转90度的次数（可为负数）
     * @return 旋转后的新Part对象
     */
    public Part rotate(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) {
            return copyWithSuffix("_rot0");
        }

        int newWidth = (turns % 2 == 0) ? width : depth;
        int newDepth = (turns % 2 == 0) ? depth : width;
        char[][][] newStructure = new char[height][newDepth][newWidth];

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int newX;
                    int newZ;
                    switch (turns) {
                        case 1 -> {
                            newX = z;
                            newZ = width - 1 - x;
                        }
                        case 2 -> {
                            newX = width - 1 - x;
                            newZ = depth - 1 - z;
                        }
                        case 3 -> {
                            newX = depth - 1 - z;
                            newZ = x;
                        }
                        default -> throw new IllegalStateException("Unexpected turns: " + turns);
                    }
                    newStructure[y][newZ][newX] = getCharAt(x, y, z);
                }
            }
        }

        return new Part(id + "_rot" + (turns * 90), paletteIds, newWidth, height, newDepth, newStructure);
    }
    
    /**
     * 镜像部件（沿X轴镜像）
     * 创建一个新的镜像后的Part对象
     * 
     * @return 镜像后的新Part对象
     */
    public Part mirror() {
        return mirrorX();
    }

    /**
     * 镜像部件（沿X轴镜像）
     *
     * @return 镜像后的新Part对象
     */
    public Part mirrorX() {
        char[][][] newStructure = new char[height][depth][width];
        
        // 镜像变换：x -> width-1-x
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int newX = width - 1 - x;
                    newStructure[y][z][newX] = getCharAt(x, y, z);
                }
            }
        }
        
        return new Part(id + "_mirror_x", paletteIds, width, height, depth, newStructure);
    }

    /**
     * 镜像部件（沿Z轴镜像）
     *
     * @return 镜像后的新Part对象
     */
    public Part mirrorZ() {
        char[][][] newStructure = new char[height][depth][width];

        // 镜像变换：z -> depth-1-z
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int newZ = depth - 1 - z;
                    newStructure[y][newZ][x] = getCharAt(x, y, z);
                }
            }
        }

        return new Part(id + "_mirror_z", paletteIds, width, height, depth, newStructure);
    }

    /**
     * 组合变换：先旋转，再镜像。
     * 镜像操作在旋转后的坐标系上执行。
     *
     * @param quarterTurns 顺时针旋转90度的次数（可为负数）
     * @param mirrorX 是否沿X轴镜像
     * @param mirrorZ 是否沿Z轴镜像
     * @return 变换后的新Part对象
     */
    public Part transform(int quarterTurns, boolean mirrorX, boolean mirrorZ) {
        Part transformed = rotate(quarterTurns);
        if (mirrorX) {
            transformed = transformed.mirrorX();
        }
        if (mirrorZ) {
            transformed = transformed.mirrorZ();
        }
        return transformed;
    }

    /**
     * 按预定义变换枚举执行变换。
     *
     * @param transform 变换类型
     * @return 变换后的新Part对象
     */
    public Part transform(Transform transform) {
        if (transform == null) {
            return copyWithSuffix("_transform_none");
        }
        return switch (transform) {
            case ROTATE_NONE -> copyWithSuffix("_rot0");
            case ROTATE_90 -> rotate90();
            case ROTATE_180 -> rotate180();
            case ROTATE_270 -> rotate270();
            case MIRROR_X -> mirrorX();
            case MIRROR_Z -> mirrorZ();
            // 与 LC 语义对齐：先 X 镜像，再顺时针旋转 90°
            case MIRROR_90_X -> mirrorX().rotate90();
        };
    }

    private Part copyWithSuffix(String suffix) {
        char[][][] copied = new char[height][depth][width];
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    copied[y][z][x] = getCharAt(x, y, z);
                }
            }
        }
        return new Part(id + suffix, paletteIds, width, height, depth, copied);
    }
    
    /**
     * 验证部件的有效性
     * 
     * @return 如果有效返回true
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        if (width <= 0 || height <= 0 || depth <= 0) {
            return false;
        }
        
        if (structure == null || structure.length != height) {
            return false;
        }
        
        // 检查结构数据的完整性
        for (int y = 0; y < height; y++) {
            if (structure[y] == null || structure[y].length != depth) {
                return false;
            }
            for (int z = 0; z < depth; z++) {
                if (structure[y][z] == null || structure[y][z].length != width) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Getter方法
    
    public String getId() {
        return id;
    }
    
    public List<String> getPaletteIds() {
        return new ArrayList<>(paletteIds);
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public char[][][] getStructure() {
        return structure;
    }
}
