package com.during.cityloader.worldgen;

import com.during.cityloader.exception.ChunkGenerationException;
import com.during.cityloader.util.CityLoaderLogger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.generator.LimitedRegion;

import java.util.function.Predicate;

/**
 * 区块驱动器
 * 高效的方块放置和状态更新引擎
 * 
 * 这是Paper API的完整实现，包含：
 * - SectionCache批处理系统
 * - 相邻方块状态自动更新（楼梯、墙、栅栏）
 * - 高度图维护
 * - 范围操作支持
 * - 错误处理和日志记录
 * 
 * @author During
 * @since 1.4.0
 */
public class ChunkDriver {
    
    private World world;
    private LimitedRegion region;
    private int chunkX;
    private int chunkZ;
    private int currentX;
    private int currentY;
    private int currentZ;
    
    // 区段缓存系统
    private SectionCache cache;
    
    // 日志记录器（可选）
    private CityLoaderLogger logger;
    
    /**
     * 设置日志记录器
     * 
     * @param logger 日志记录器
     */
    public void setLogger(CityLoaderLogger logger) {
        this.logger = logger;
    }
    
    /**
     * 设置区块数据
     * 
     * @param world 世界
     * @param region 限制区域（Paper API）
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     */
    public void setPrimer(World world, LimitedRegion region, int chunkX, int chunkZ) {
        try {
            this.world = world;
            this.region = region;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.currentX = 0;
            this.currentY = 0;
            this.currentZ = 0;
            
            // 初始化区段缓存
            if (region != null) {
                this.cache = new SectionCache(world, chunkX << 4, chunkZ << 4);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logGenerationError(chunkX, chunkZ, "初始化区块驱动器失败", e);
            }
            throw new ChunkGenerationException("初始化区块驱动器失败", chunkX, chunkZ, e);
        }
    }
    
    /**
     * 设置当前位置（相对坐标）
     * 
     * @param x 相对X坐标（0-15）
     * @param y Y坐标
     * @param z 相对Z坐标（0-15）
     * @return this
     */
    public ChunkDriver current(int x, int y, int z) {
        this.currentX = x + (chunkX << 4);
        this.currentY = y;
        this.currentZ = z + (chunkZ << 4);
        return this;
    }
    
    /**
     * 设置当前位置（绝对坐标）
     * 
     * @param x 绝对X坐标
     * @param y Y坐标
     * @param z 绝对Z坐标
     * @return this
     */
    public ChunkDriver currentAbsolute(int x, int y, int z) {
        this.currentX = x;
        this.currentY = y;
        this.currentZ = z;
        return this;
    }
    
    /**
     * Y坐标增加1
     */
    public void incY() {
        currentY++;
    }
    
    /**
     * Y坐标减少1
     */
    public void decY() {
        currentY--;
    }
    
    /**
     * X坐标增加1
     */
    public void incX() {
        currentX++;
    }
    
    /**
     * Z坐标增加1
     */
    public void incZ() {
        currentZ++;
    }
    
    /**
     * 获取当前X坐标
     * 
     * @return X坐标
     */
    public int getX() {
        return currentX;
    }
    
    /**
     * 获取当前Y坐标
     * 
     * @return Y坐标
     */
    public int getY() {
        return currentY;
    }
    
    /**
     * 获取当前Z坐标
     * 
     * @return Z坐标
     */
    public int getZ() {
        return currentZ;
    }
    
    /**
     * 在当前位置放置方块（带相邻方块更新）
     * 
     * @param blockData 方块数据
     * @return this
     */
    public ChunkDriver block(BlockData blockData) {
        if (blockData != null && cache != null) {
            cache.put(currentX, currentY, currentZ, blockData);
        }
        return this;
    }
    
    /**
     * 在当前位置放置方块并移动到下一个Y
     * 
     * @param blockData 方块数据
     * @return this
     */
    public ChunkDriver add(BlockData blockData) {
        block(blockData);
        incY();
        return this;
    }
    
    /**
     * 设置方块范围
     * 
     * @param x 相对X坐标
     * @param y1 起始Y坐标
     * @param z 相对Z坐标
     * @param y2 结束Y坐标（不包含）
     * @param blockData 方块数据
     */
    public void setBlockRange(int x, int y1, int z, int y2, BlockData blockData) {
        if (cache == null || blockData == null) {
            return;
        }
        
        int absX = x + (chunkX << 4);
        int absZ = z + (chunkZ << 4);
        cache.putRange(absX, absZ, y1, y2 - 1, blockData);
    }
    
    /**
     * 设置方块范围（带条件）
     * 
     * @param x 相对X坐标
     * @param y1 起始Y坐标
     * @param z 相对Z坐标
     * @param y2 结束Y坐标（不包含）
     * @param blockData 方块数据
     * @param test 条件测试
     */
    public void setBlockRange(int x, int y1, int z, int y2, BlockData blockData, Predicate<BlockData> test) {
        if (cache == null || blockData == null) {
            return;
        }
        
        int absX = x + (chunkX << 4);
        int absZ = z + (chunkZ << 4);
        cache.putRange(absX, absZ, y1, y2 - 1, blockData, test);
    }
    
    /**
     * 设置方块范围为空气
     * 
     * @param x 相对X坐标
     * @param y1 起始Y坐标
     * @param z 相对Z坐标
     * @param y2 结束Y坐标（不包含）
     */
    public void setBlockRangeToAir(int x, int y1, int z, int y2) {
        setBlockRange(x, y1, z, y2, Material.AIR.createBlockData());
    }
    
    /**
     * 获取当前位置的方块
     * 
     * @return 方块数据
     */
    public BlockData getBlock() {
        return getBlockAbsolute(currentX, currentY, currentZ);
    }
    
    /**
     * 获取指定位置的方块（绝对坐标）
     * 
     * @param x 绝对X坐标
     * @param y Y坐标
     * @param z 绝对Z坐标
     * @return 方块数据
     */
    private BlockData getBlockAbsolute(int x, int y, int z) {
        if (cache == null) {
            return null;
        }
        BlockData cached = cache.get(x, y, z);
        if (cached != null) {
            return cached;
        }
        // 如果缓存中没有，从LimitedRegion获取
        if (region != null && region.isInRegion(x, y, z)) {
            return region.getBlockData(x, y, z);
        }
        return null;
    }
    
    /**
     * 获取指定位置的方块（相对坐标）
     * 
     * @param x 相对X坐标
     * @param y Y坐标
     * @param z 相对Z坐标
     * @return 方块数据
     */
    public BlockData getBlock(int x, int y, int z) {
        int absX = x + (chunkX << 4);
        int absZ = z + (chunkZ << 4);
        return getBlockAbsolute(absX, y, absZ);
    }
    
    /**
     * 获取下方方块
     * 
     * @return 方块数据
     */
    public BlockData getBlockDown() {
        return getBlockAbsolute(currentX, currentY - 1, currentZ);
    }
    
    /**
     * 获取东侧方块
     * 
     * @return 方块数据
     */
    public BlockData getBlockEast() {
        return getBlockAbsolute(currentX + 1, currentY, currentZ);
    }
    
    /**
     * 获取西侧方块
     * 
     * @return 方块数据
     */
    public BlockData getBlockWest() {
        return getBlockAbsolute(currentX - 1, currentY, currentZ);
    }
    
    /**
     * 获取南侧方块
     * 
     * @return 方块数据
     */
    public BlockData getBlockSouth() {
        return getBlockAbsolute(currentX, currentY, currentZ + 1);
    }
    
    /**
     * 获取北侧方块
     * 
     * @return 方块数据
     */
    public BlockData getBlockNorth() {
        return getBlockAbsolute(currentX, currentY, currentZ - 1);
    }
    
    /**
     * 修正方块状态（处理相邻方块连接）
     * 
     * @param blockData 原始方块数据
     * @param x 绝对X坐标
     * @param y Y坐标
     * @param z 绝对Z坐标
     * @return 修正后的方块数据，如果是STRUCTURE_VOID则返回null
     */
    public BlockData correct(BlockData blockData, int x, int y, int z) {
        if (blockData == null) {
            return null;
        }
        // STRUCTURE_VOID作为透明层，不改变原有方块
        if (blockData.getMaterial() == Material.STRUCTURE_VOID) {
            return null;
        }
        
        if (blockData instanceof Wall) {
            return correctWall((Wall) blockData, x, y, z);
        } else if (blockData instanceof Fence) {
            return correctFence((Fence) blockData, x, y, z);
        }
        if (blockData instanceof Stairs) {
            return correctStairs((Stairs) blockData, x, y, z);
        }
        
        return blockData;
    }

    private BlockData correctStairs(Stairs stairs, int x, int y, int z) {
        stairs = (Stairs) stairs.clone();
        BlockFace face = stairs.getFacing();
        if (!isHorizontalFace(face)) {
            stairs.setShape(Stairs.Shape.STRAIGHT);
            return stairs;
        }

        // 与 LC 对齐：先检查前方（外角），再检查后方（内角）
        BlockData frontBlock = getBlockAbsolute(x + face.getModX(), y, z + face.getModZ());
        if (frontBlock instanceof Stairs frontStairs
                && frontStairs.getHalf() == stairs.getHalf()) {
            BlockFace frontFacing = frontStairs.getFacing();
            if (isHorizontalFace(frontFacing)
                    && !sameAxis(frontFacing, face)
                    && isDifferentStairs(stairs, x, y, z, frontFacing.getOppositeFace())) {
                if (frontFacing == rotateLeft(face)) {
                    stairs.setShape(Stairs.Shape.OUTER_LEFT);
                } else {
                    stairs.setShape(Stairs.Shape.OUTER_RIGHT);
                }
                return stairs;
            }
        }

        BlockFace backFace = face.getOppositeFace();
        BlockData backBlock = getBlockAbsolute(x + backFace.getModX(), y, z + backFace.getModZ());
        if (backBlock instanceof Stairs backStairs
                && backStairs.getHalf() == stairs.getHalf()) {
            BlockFace backFacing = backStairs.getFacing();
            if (isHorizontalFace(backFacing)
                    && !sameAxis(backFacing, face)
                    && isDifferentStairs(stairs, x, y, z, backFacing)) {
                if (backFacing == rotateLeft(face)) {
                    stairs.setShape(Stairs.Shape.INNER_LEFT);
                } else {
                    stairs.setShape(Stairs.Shape.INNER_RIGHT);
                }
                return stairs;
            }
        }

        stairs.setShape(Stairs.Shape.STRAIGHT);
        return stairs;
    }

    private boolean isDifferentStairs(Stairs stairs, int x, int y, int z, BlockFace face) {
        if (!isHorizontalFace(face)) {
            return true;
        }
        BlockData neighbor = getBlockAbsolute(x + face.getModX(), y, z + face.getModZ());
        if (!(neighbor instanceof Stairs neighborStairs)) {
            return true;
        }
        return neighborStairs.getFacing() != stairs.getFacing() || neighborStairs.getHalf() != stairs.getHalf();
    }

    private boolean isHorizontalFace(BlockFace face) {
        return face == BlockFace.NORTH
                || face == BlockFace.SOUTH
                || face == BlockFace.EAST
                || face == BlockFace.WEST;
    }

    private boolean sameAxis(BlockFace a, BlockFace b) {
        return (isNorthSouth(a) && isNorthSouth(b))
                || (isEastWest(a) && isEastWest(b));
    }

    private boolean isNorthSouth(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH;
    }

    private boolean isEastWest(BlockFace face) {
        return face == BlockFace.EAST || face == BlockFace.WEST;
    }

    private BlockFace rotateLeft(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.WEST;
            case WEST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.EAST;
            case EAST: return BlockFace.NORTH;
            default: return face;
        }
    }

    private BlockData correctWall(Wall wall, int x, int y, int z) {
        wall = (Wall) wall.clone();

        boolean north = canAttach(getBlockAbsolute(x, y, z - 1));
        boolean south = canAttach(getBlockAbsolute(x, y, z + 1));
        boolean west = canAttach(getBlockAbsolute(x - 1, y, z));
        boolean east = canAttach(getBlockAbsolute(x + 1, y, z));

        wall.setHeight(BlockFace.NORTH, north ? Wall.Height.LOW : Wall.Height.NONE);
        wall.setHeight(BlockFace.SOUTH, south ? Wall.Height.LOW : Wall.Height.NONE);
        wall.setHeight(BlockFace.WEST, west ? Wall.Height.LOW : Wall.Height.NONE);
        wall.setHeight(BlockFace.EAST, east ? Wall.Height.LOW : Wall.Height.NONE);
        wall.setUp(shouldWallPostBeUp(north, south, west, east, getBlockAbsolute(x, y + 1, z)));

        return wall;
    }

    private boolean shouldWallPostBeUp(boolean north, boolean south, boolean west, boolean east, BlockData above) {
        if (canAttach(above)) {
            return true;
        }
        boolean straightNorthSouth = north && south && !west && !east;
        boolean straightEastWest = west && east && !north && !south;
        return !(straightNorthSouth || straightEastWest);
    }

    private boolean canAttach(BlockData neighbor) {
        if (neighbor == null) {
            return false;
        }
        Material material = neighbor.getMaterial();
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        if (neighbor instanceof Fence || neighbor instanceof Wall || neighbor instanceof Gate) {
            return true;
        }
        return material.isOccluding();
    }

    private BlockData correctFence(Fence fence, int x, int y, int z) {
        fence = (Fence) fence.clone();
        fence.setFace(BlockFace.NORTH, false);
        fence.setFace(BlockFace.SOUTH, false);
        fence.setFace(BlockFace.WEST, false);
        fence.setFace(BlockFace.EAST, false);

        if (canAttach(getBlockAbsolute(x, y, z - 1))) {
            fence.setFace(BlockFace.NORTH, true);
        }
        if (canAttach(getBlockAbsolute(x, y, z + 1))) {
            fence.setFace(BlockFace.SOUTH, true);
        }
        if (canAttach(getBlockAbsolute(x - 1, y, z))) {
            fence.setFace(BlockFace.WEST, true);
        }
        if (canAttach(getBlockAbsolute(x + 1, y, z))) {
            fence.setFace(BlockFace.EAST, true);
        }

        return fence;
    }
    
    /**
     * 实际生成方块到区块
     * 将缓存的方块数据写入LimitedRegion
     */
    public void actuallyGenerate() {
        if (cache != null && region != null) {
            cache.fixStates(this);
            cache.generate(region);
        }
    }
    
    /**
     * 清理缓存
     */
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
    }
    
    /**
     * 获取世界
     * 
     * @return 世界对象
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * 获取区块X坐标
     * 
     * @return 区块X坐标
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * 获取区块Z坐标
     * 
     * @return 区块Z坐标
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * 区段缓存系统
     * 用于批量处理方块放置，提高性能
     */
    private static class SectionCache {
        private final int minY;
        private final int maxY;
        private final int cx;
        private final int cz;
        private final Section[] sections;
        private final int[][] heightmap = new int[16][16];
        
        /**
         * 构造函数
         * 
         * @param world 世界
         * @param cx 区块绝对X坐标（方块坐标）
         * @param cz 区块绝对Z坐标（方块坐标）
         */
        public SectionCache(World world, int cx, int cz) {
            this.minY = world.getMinHeight();
            this.maxY = world.getMaxHeight();
            this.cx = cx;
            this.cz = cz;
            
            // 计算区段数量（每个区段16格高）
            int sectionCount = (maxY - minY) / 16;
            this.sections = new Section[sectionCount];
            
            clear();
        }
        
        /**
         * 放置单个方块
         * 
         * @param x 绝对X坐标
         * @param y Y坐标
         * @param z 绝对Z坐标
         * @param blockData 方块数据
         */
        public void put(int x, int y, int z, BlockData blockData) {
            if (blockData == null || y < minY || y >= maxY) {
                return;
            }
            
            int sectionIdx = (y - minY) / 16;
            int px = x & 0xf;
            int pz = z & 0xf;
            int py = y & 0xf;
            int idx = toIndex(px, py, pz);
            
            if (sections[sectionIdx].blocks[idx] == blockData) {
                return;
            }
            
            sections[sectionIdx].blocks[idx] = blockData;
            sections[sectionIdx].isEmpty = false;
            
            if (!isAir(blockData)) {
                if (heightmap[px][pz] < y) {
                    heightmap[px][pz] = y;
                }
            } else {
                // 如果放置空气，需要重新计算高度图
                fixHeightmapForAir(y, px, pz);
            }
        }
        
        /**
         * 获取方块
         * 
         * @param x 绝对X坐标
         * @param y Y坐标
         * @param z 绝对Z坐标
         * @return 方块数据，如果未缓存则返回null
         */
        public BlockData get(int x, int y, int z) {
            if (y < minY || y >= maxY) {
                return null;
            }
            
            int sectionIdx = (y - minY) / 16;
            int px = x & 0xf;
            int pz = z & 0xf;
            int py = y & 0xf;
            int idx = toIndex(px, py, pz);
            
            return sections[sectionIdx].blocks[idx];
        }
        
        /**
         * 放置方块范围
         * 
         * @param x 绝对X坐标
         * @param z 绝对Z坐标
         * @param y1 起始Y坐标
         * @param y2 结束Y坐标（包含）
         * @param blockData 方块数据
         */
        public void putRange(int x, int z, int y1, int y2, BlockData blockData) {
            if (blockData == null) {
                return;
            }
            
            int ystart = y1;
            int px = x & 0xf;
            int pz = z & 0xf;
            boolean isAir = isAir(blockData);
            boolean dirty = false;
            
            while (y1 <= y2 && y1 < maxY) {
                if (y1 >= minY) {
                    int sectionIdx = (y1 - minY) / 16;
                    int py = y1 & 0xf;
                    int idx = toIndex(px, py, pz);
                    
                    if (sections[sectionIdx].blocks[idx] != blockData) {
                        dirty = true;
                        sections[sectionIdx].blocks[idx] = blockData;
                        sections[sectionIdx].isEmpty = false;
                    }
                }
                y1++;
            }
            
            // 更新高度图
            if (dirty) {
                if (!isAir) {
                    if (heightmap[px][pz] < y2) {
                        heightmap[px][pz] = y2;
                    }
                } else {
                    fixHeightmapForAir(ystart, px, pz);
                }
            }
        }
        
        /**
         * 放置方块范围（带条件）
         * 
         * @param x 绝对X坐标
         * @param z 绝对Z坐标
         * @param y1 起始Y坐标
         * @param y2 结束Y坐标（包含）
         * @param blockData 方块数据
         * @param test 条件测试
         */
        public void putRange(int x, int z, int y1, int y2, BlockData blockData, Predicate<BlockData> test) {
            if (blockData == null) {
                return;
            }
            
            int ystart = y1;
            int px = x & 0xf;
            int pz = z & 0xf;
            boolean isAir = isAir(blockData);
            boolean dirty = false;
            
            while (y1 <= y2 && y1 < maxY) {
                if (y1 >= minY) {
                    int sectionIdx = (y1 - minY) / 16;
                    int py = y1 & 0xf;
                    int idx = toIndex(px, py, pz);
                    
                    BlockData existing = sections[sectionIdx].blocks[idx];
                    if (existing != blockData && existing != null && test.test(existing)) {
                        dirty = true;
                        sections[sectionIdx].blocks[idx] = blockData;
                        sections[sectionIdx].isEmpty = false;
                    }
                }
                y1++;
            }
            
            // 更新高度图
            if (dirty) {
                if (!isAir) {
                    if (heightmap[px][pz] < y2) {
                        heightmap[px][pz] = y2;
                    }
                } else {
                    fixHeightmapForAir(ystart, px, pz);
                }
            }
        }
        
        /**
         * 修复所有方块状态
         *
         * @param driver 驱动器实例（用于调用 correct）
         */
        public void fixStates(ChunkDriver driver) {
            for (int si = 0; si < sections.length; si++) {
                Section section = sections[si];
                if (!section.isEmpty) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int idx = toIndex(x, y, z);
                                BlockData original = section.blocks[idx];
                                if (original != null) {
                                    int worldY = si * 16 + y + minY;
                                    int worldX = cx + x;
                                    int worldZ = cz + z;
                                    BlockData corrected = driver.correct(original, worldX, worldY, worldZ);
                                    if (corrected != original) {
                                        section.blocks[idx] = corrected;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * 修复空气方块的高度图
         * 
         * @param y1 起始Y坐标
         * @param px 相对X坐标
         * @param pz 相对Z坐标
         */
        private void fixHeightmapForAir(int y1, int px, int pz) {
            if (heightmap[px][pz] >= y1) {
                int y = Math.max(heightmap[px][pz], y1);
                while (y >= minY) {
                    int si = (y - minY) / 16;
                    int py = y & 0xf;
                    int i = toIndex(px, py, pz);
                    BlockData mat = sections[si].blocks[i];
                    if (mat != null && !isAir(mat)) {
                        heightmap[px][pz] = y;
                        return;
                    }
                    y--;
                }
                heightmap[px][pz] = Integer.MIN_VALUE;
            }
        }
        
        /**
         * 生成方块到LimitedRegion
         * 
         * @param region 限制区域
         */
        public void generate(LimitedRegion region) {
            for (int si = 0; si < sections.length; si++) {
                Section section = sections[si];
                if (!section.isEmpty) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockData blockData = section.blocks[toIndex(x, y, z)];
                                if (blockData != null) {
                                    int worldY = si * 16 + y + minY;
                                    // LimitedRegion 使用绝对坐标
                                    int worldX = cx + x;
                                    int worldZ = cz + z;
                                    region.setBlockData(worldX, worldY, worldZ, blockData);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        /**
         * 清理缓存
         */
        public void clear() {
            for (int si = 0; si < sections.length; si++) {
                sections[si] = new Section();
            }
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap[x][z] = Integer.MIN_VALUE;
                }
            }
        }
        
        /**
         * 判断是否为空气
         * 
         * @param blockData 方块数据
         * @return 是否为空气
         */
        private boolean isAir(BlockData blockData) {
            if (blockData == null) return true;
            Material material = blockData.getMaterial();
            return material == Material.AIR || 
                   material == Material.CAVE_AIR || 
                   material == Material.VOID_AIR;
        }

        /**
         * 计算区段内一维索引。
         * 与 LC 对齐：((y << 8) + (x << 4) + z)
         */
        private int toIndex(int px, int py, int pz) {
            return (py << 8) + (px << 4) + pz;
        }
        
        /**
         * 区段存储
         */
        private static class Section {
            // 16x16x16 = 4096个方块
            private final BlockData[] blocks = new BlockData[4096];
            private boolean isEmpty = true;
        }
    }
}
