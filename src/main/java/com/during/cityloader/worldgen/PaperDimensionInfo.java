package com.during.cityloader.worldgen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.LimitedRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Paper维度信息实现
 * 适配Paper API的维度信息接口实现
 * 
 * @author During
 * @since 1.4.0
 */
public class PaperDimensionInfo implements IDimensionInfo {

    /**
     * 安全高度图模式：默认开启，避免在区块生成线程中调用 world.getHighestBlockYAt()
     * 导致递归区块加载/主线程阻塞。
     */
    private static final boolean SAFE_HEIGHTMAP = Boolean.parseBoolean(
            System.getProperty("cityloader.safeHeightmap", "false"));

    private static final ThreadLocal<GenerationFrame> ACTIVE_GENERATION = new ThreadLocal<>();
    
    private World world;
    private final LostCityProfile profile;
    private final LostCityProfile outsideProfile;
    private final WorldStyle worldStyle;
    private final LostCityTerrainFeature feature;
    private final Random random;
    private final Map<ChunkCoord, ChunkHeightmap> heightmapCache;

    private record GenerationFrame(LimitedRegion region, int chunkX, int chunkZ) {
    }
    
    /**
     * 构造Paper维度信息
     *
     * @param world 世界对象
     * @param profile 城市配置
     * @param outsideProfile 外部配置
     * @param worldStyle 世界风格
     */
    public PaperDimensionInfo(World world, LostCityProfile profile, LostCityProfile outsideProfile, WorldStyle worldStyle) {
        this.world = world;
        this.profile = profile;
        this.outsideProfile = outsideProfile == null ? profile : outsideProfile;
        this.worldStyle = worldStyle;
        this.feature = LostCityTerrainFeature.DEFAULT;
        this.random = new Random(world.getSeed());
        this.heightmapCache = new HashMap<>();
    }
    
    @Override
    public void setWorld(World world) {
        this.world = world;
    }
    
    @Override
    public long getSeed() {
        return world.getSeed();
    }
    
    @Override
    public World getWorld() {
        return world;
    }
    
    @Override
    public String getType() {
        return world.getEnvironment().name().toLowerCase();
    }
    
    @Override
    public LostCityProfile getProfile() {
        return profile;
    }
    
    @Override
    public LostCityProfile getOutsideProfile() {
        return outsideProfile;
    }
    
    @Override
    public WorldStyle getWorldStyle() {
        return worldStyle;
    }
    
    @Override
    public Random getRandom() {
        return random;
    }
    
    @Override
    public LostCityTerrainFeature getFeature() {
        return feature;
    }
    
    @Override
    public ChunkHeightmap getHeightmap(int chunkX, int chunkZ) {
        return getHeightmap(new ChunkCoord(world.getEnvironment().name(), chunkX, chunkZ));
    }
    
    @Override
    public ChunkHeightmap getHeightmap(ChunkCoord coord) {
        return heightmapCache.computeIfAbsent(coord, this::generateHeightmap);
    }

    public void beginChunkGeneration(LimitedRegion region, int chunkX, int chunkZ) {
        if (region == null) {
            ACTIVE_GENERATION.remove();
            return;
        }
        ACTIVE_GENERATION.set(new GenerationFrame(region, chunkX, chunkZ));
    }

    public void endChunkGeneration() {
        ACTIVE_GENERATION.remove();
    }
    
    /**
     * 生成区块高度图
     * 
     * @param coord 区块坐标
     * @return 生成的高度图
     */
    private ChunkHeightmap generateHeightmap(ChunkCoord coord) {
        ChunkHeightmap heightmap = new ChunkHeightmap();

        GenerationFrame frame = ACTIVE_GENERATION.get();
        if (frame != null && fillFromRegion(heightmap, frame.region(), coord)) {
            return heightmap;
        }

        if (SAFE_HEIGHTMAP) {
            int safeHeight = Math.max(world.getMinHeight() + 1, world.getSeaLevel());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, safeHeight);
                }
            }
            return heightmap;
        }

        // 非安全模式：读取真实高度（可能触发区块加载，谨慎使用）
        int baseX = coord.chunkX() * 16;
        int baseZ = coord.chunkZ() * 16;

        if (!world.isChunkLoaded(coord.chunkX(), coord.chunkZ())) {
            int safeHeight = Math.max(world.getMinHeight() + 1, world.getSeaLevel());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.setHeight(x, z, safeHeight);
                }
            }
            return heightmap;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int height = world.getHighestBlockYAt(worldX, worldZ);
                heightmap.setHeight(x, z, height);
            }
        }
        
        return heightmap;
    }

    private boolean fillFromRegion(ChunkHeightmap heightmap, LimitedRegion region, ChunkCoord coord) {
        if (region == null) {
            return false;
        }

        int baseX = coord.chunkX() * 16;
        int baseZ = coord.chunkZ() * 16;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int probeY = Math.max(minY + 1, Math.min(world.getSeaLevel(), maxY));

        if (!region.isInRegion(baseX, probeY, baseZ)) {
            return false;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                if (!region.isInRegion(worldX, probeY, worldZ)) {
                    return false;
                }

                int y = maxY;
                while (y >= minY) {
                    Material type = region.getType(worldX, y, worldZ);
                    if (type != null && !type.isAir()) {
                        break;
                    }
                    y--;
                }
                if (y < minY) {
                    y = Math.max(minY + 1, world.getSeaLevel());
                }
                heightmap.setHeight(x, z, y);
            }
        }
        return true;
    }
    
    @Override
    public Biome getBiome(int x, int y, int z) {
        return world.getBiome(x, y, z);
    }
    
    @Override
    public String dimension() {
        return world.getName();
    }
}
