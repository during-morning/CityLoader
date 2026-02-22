package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.ConditionTest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.function.Predicate;

/**
 * 条件上下文抽象类
 * 用于评估条件资产选择的上下文对象
 * 
 * <p>提供生成上下文信息，包括：
 * <ul>
 *   <li>楼层级别（level）：世界中的全局层级，0为建筑部分可能的最低层级</li>
 *   <li>楼层（floor）：建筑中的楼层，0为地面层，floor == floorsAboveGround表示建筑顶部</li>
 *   <li>地下室数量（floorsBelowGround）：0表示没有地下室</li>
 *   <li>地上楼层数量（floorsAboveGround）：1表示地上有1层</li>
 *   <li>当前部件名称（part）</li>
 *   <li>下方部件名称（belowPart）</li>
 *   <li>建筑名称（building）</li>
 *   <li>区块坐标（coord）</li>
 * </ul>
 * 
 * @author During
 * @since 1.4.0
 */
public abstract class ConditionContext {
    private final int level;        // 世界中的全局层级，0为建筑部分可能的最低层级
    private final int floor;        // 建筑中的楼层，0为地面层
    private final int floorsBelowGround;    // 地下室数量，0表示没有地下室
    private final int floorsAboveGround;    // 地上楼层数量，1表示地上有1层
    private final String part;
    private final String belowPart;
    private final String building;
    private final ChunkCoord coord;

    /**
     * 构造条件上下文
     * 
     * @param level 世界中的全局层级
     * @param floor 建筑中的楼层
     * @param floorsBelowGround 地下室数量
     * @param floorsAboveGround 地上楼层数量
     * @param part 当前部件名称
     * @param belowPart 下方部件名称
     * @param building 建筑名称
     * @param coord 区块坐标
     */
    public ConditionContext(int level, int floor, int floorsBelowGround, int floorsAboveGround, 
                           String part, String belowPart, String building, ChunkCoord coord) {
        this.level = level;
        this.floor = floor;
        this.floorsBelowGround = floorsBelowGround;
        this.floorsAboveGround = floorsAboveGround;
        this.part = part;
        this.belowPart = belowPart;
        this.building = building;
        this.coord = coord;
    }

    /**
     * 组合两个条件谓词（AND逻辑）
     * 
     * @param orig 原始谓词
     * @param newTest 新谓词
     * @return 组合后的谓词
     */
    private static Predicate<ConditionContext> combine(Predicate<ConditionContext> orig, Predicate<ConditionContext> newTest) {
        if (orig == null) {
            return newTest;
        }
        return levelInfo -> orig.test(levelInfo) && newTest.test(levelInfo);
    }

    /**
     * 从ConditionTest对象解析条件谓词
     * 
     * @param element ConditionTest对象
     * @return 条件谓词
     */
    public static Predicate<ConditionContext> parseTest(ConditionTest element) {
        Predicate<ConditionContext> test = null;
        if (element.getTop() != null) {
            boolean top = element.getTop();
            if (top) {
                test = combine(test, ConditionContext::isTopOfBuilding);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isTopOfBuilding());
            }
        }
        if (element.getGround() != null) {
            boolean ground = element.getGround();
            if (ground) {
                test = combine(test, ConditionContext::isGroundFloor);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isGroundFloor());
            }
        }
        if (element.getIsbuilding() != null) {
            boolean ground = element.getIsbuilding();
            if (ground) {
                test = combine(test, ConditionContext::isBuilding);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isBuilding());
            }
        }
        if (element.getIssphere() != null) {
            boolean ground = element.getIssphere();
            if (ground) {
                test = combine(test, ConditionContext::isSphere);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isSphere());
            }
        }
        if (element.getChunkx() != null) {
            int chunkX = element.getChunkx();
            test = combine(test, context -> chunkX == context.getChunkX());
        }
        if (element.getChunkz() != null) {
            int chunkZ = element.getChunkz();
            test = combine(test, context -> chunkZ == context.getChunkZ());
        }
        if (element.getBelowPart() != null) {
            Set<String> belowPart = element.getBelowPart();
            test = combine(test, context -> belowPart.contains(context.getBelowPart()));
        }
        if (element.getInpart() != null) {
            Set<String> part = element.getInpart();
            test = combine(test, context -> part.contains(context.getPart()));
        }
        if (element.getInbuilding() != null) {
            Set<String> building = element.getInbuilding();
            test = combine(test, context -> building.contains(context.getBuilding()));
        }
        if (element.getInbiome() != null) {
            Set<String> biome = element.getInbiome();
            test = combine(test, context -> biome.contains(context.getBiome().toString()));
        }
        if (element.getCellar() != null) {
            boolean cellar = element.getCellar();
            if (cellar) {
                test = combine(test, ConditionContext::isCellar);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isCellar());
            }
        }
        if (element.getFloor() != null) {
            int level = element.getFloor();
            test = combine(test, levelInfo -> levelInfo.isFloor(level));
        }
        if (element.getRange() != null) {
            String range = element.getRange();
            String[] split = StringUtils.split(range, ',');
            try {
                int l1 = Integer.parseInt(split[0]);
                int l2 = Integer.parseInt(split[1]);
                test = combine(test, levelInfo -> levelInfo.isRange(l1, l2));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("错误的范围规范: <l1>,<l2>!", e);
            }
        }
        if (test == null) {
            test = conditionContext -> true;
        }
        return test;
    }

    /**
     * 从JsonElement解析条件谓词
     * 
     * @param element JsonElement对象
     * @return 条件谓词
     */
    public static Predicate<ConditionContext> parseTest(JsonElement element) {
        Predicate<ConditionContext> test = null;
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("top")) {
            boolean top = obj.get("top").getAsBoolean();
            if (top) {
                test = combine(test, ConditionContext::isTopOfBuilding);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isTopOfBuilding());
            }
        }
        if (obj.has("ground")) {
            boolean ground = obj.get("ground").getAsBoolean();
            if (ground) {
                test = combine(test, ConditionContext::isGroundFloor);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isGroundFloor());
            }
        }
        if (obj.has("isbuilding")) {
            boolean ground = obj.get("isbuilding").getAsBoolean();
            if (ground) {
                test = combine(test, ConditionContext::isBuilding);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isBuilding());
            }
        }
        if (obj.has("issphere")) {
            boolean ground = obj.get("issphere").getAsBoolean();
            if (ground) {
                test = combine(test, ConditionContext::isSphere);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isSphere());
            }
        }
        if (obj.has("chunkx")) {
            int chunkX = obj.get("chunkx").getAsInt();
            test = combine(test, context -> chunkX == context.getChunkX());
        }
        if (obj.has("chunkz")) {
            int chunkZ = obj.get("chunkz").getAsInt();
            test = combine(test, context -> chunkZ == context.getChunkZ());
        }
        if (obj.has("inpart")) {
            String part = obj.get("inpart").getAsString();
            test = combine(test, context -> part.equals(context.getPart()));
        }
        if (obj.has("inbuilding")) {
            String building = obj.get("inbuilding").getAsString();
            test = combine(test, context -> building.equals(context.getBuilding()));
        }
        if (obj.has("inbiome")) {
            String biome = obj.get("inbiome").getAsString();
            test = combine(test, context -> biome.equals(context.getBiome().toString()));
        }
        if (obj.has("cellar")) {
            boolean cellar = obj.get("cellar").getAsBoolean();
            if (cellar) {
                test = combine(test, ConditionContext::isCellar);
            } else {
                test = combine(test, levelInfo -> !levelInfo.isCellar());
            }
        }
        if (obj.has("floor")) {
            int level = obj.get("floor").getAsInt();
            test = combine(test, levelInfo -> levelInfo.isFloor(level));
        }
        if (obj.has("range")) {
            String range = obj.get("range").getAsString();
            String[] split = StringUtils.split(range, ',');
            try {
                int l1 = Integer.parseInt(split[0]);
                int l2 = Integer.parseInt(split[1]);
                test = combine(test, levelInfo -> levelInfo.isRange(l1, l2));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("错误的范围规范: <l1>,<l2>!", e);
            }
        }
        if (test == null) {
            test = conditionContext -> true;
        }
        return test;
    }

    public int getLevel() {
        return level;
    }

    public int getFloor() {
        return floor;
    }

    public int getFloorsBelowGround() {
        return floorsBelowGround;
    }

    public int getFloorsAboveGround() {
        return floorsAboveGround;
    }

    /**
     * 是否为地面层
     * 
     * @return 如果是地面层返回true
     */
    public boolean isGroundFloor() {
        return floor == 0;
    }

    /**
     * 是否在建筑中
     * 
     * @return 如果在建筑中返回true
     */
    public boolean isBuilding() {
        return !"<none>".equals(building);
    }

    /**
     * 是否在球体中（由子类实现）
     * 
     * @return 如果在球体中返回true
     */
    public abstract boolean isSphere();

    /**
     * 获取生物群系（由子类实现）
     * 
     * @return 生物群系资源位置
     */
    public abstract ResourceLocation getBiome();

    /**
     * 是否为建筑顶部
     * 
     * @return 如果是建筑顶部返回true
     */
    public boolean isTopOfBuilding() {
        if (floorsAboveGround <= 0) {
            return false;
        }
        return floor >= (floorsAboveGround - 1);
    }

    /**
     * 是否为地下室
     * 
     * @return 如果是地下室返回true
     */
    public boolean isCellar() {
        return floor < 0;
    }

    /**
     * 是否为指定楼层
     * 
     * @param l 楼层编号
     * @return 如果是指定楼层返回true
     */
    public boolean isFloor(int l) {
        return floor == l;
    }

    /**
     * 是否在指定楼层范围内
     * 
     * @param l1 起始楼层
     * @param l2 结束楼层
     * @return 如果在范围内返回true
     */
    public boolean isRange(int l1, int l2) {
        return floor >= l1 && floor <= l2;
    }

    public String getPart() {
        return part;
    }

    public String getBelowPart() {
        return belowPart;
    }

    public String getBuilding() {
        return building;
    }

    public int getChunkX() {
        return coord.chunkX();
    }

    public int getChunkZ() {
        return coord.chunkZ();
    }
}
