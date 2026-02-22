package com.during.cityloader.worldgen;

import com.during.cityloader.season.Season;
import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.gen.BridgeStage;
import com.during.cityloader.worldgen.gen.CityCoreStage;
import com.during.cityloader.worldgen.gen.CitySphereStage;
import com.during.cityloader.worldgen.gen.CorridorStage;
import com.during.cityloader.worldgen.gen.DamageStage;
import com.during.cityloader.worldgen.gen.FountainStage;
import com.during.cityloader.worldgen.gen.GenerationContext;
import com.during.cityloader.worldgen.gen.GenerationStage;
import com.during.cityloader.worldgen.gen.InfrastructureStage;
import com.during.cityloader.worldgen.gen.LootStage;
import com.during.cityloader.worldgen.gen.MegaSolarStage;
import com.during.cityloader.worldgen.gen.MonorailStage;
import com.during.cityloader.worldgen.gen.OffshoreStage;
import com.during.cityloader.worldgen.gen.ParkStage;
import com.during.cityloader.worldgen.gen.PostProcessStage;
import com.during.cityloader.worldgen.gen.QuarryStage;
import com.during.cityloader.worldgen.gen.RailDungeonStage;
import com.during.cityloader.worldgen.gen.ScatteredStage;
import com.during.cityloader.worldgen.gen.SpawnerStage;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 城市地形总控
 * 负责按阶段执行区块级城市生成。
 * 与LostCities 1.20完全兼容
 */
public class LostCityTerrainFeature {

    public static final LostCityTerrainFeature DEFAULT = new LostCityTerrainFeature("default");
    public static final LostCityTerrainFeature FLOATING = new LostCityTerrainFeature("floating");
    public static final LostCityTerrainFeature CAVERN = new LostCityTerrainFeature("cavern");
    public static final LostCityTerrainFeature SPACE = new LostCityTerrainFeature("space");

    private final String type;
    private final List<GenerationStage> stages;

    /**
     * 安全模式：默认开启，仅执行核心阶段，避免服务器在区块加载时长时间阻塞。
     * 可通过 -Dcityloader.safeMode=false 关闭。
     */
    private static final boolean SAFE_MODE = Boolean.parseBoolean(
            System.getProperty("cityloader.safeMode", "false"));

    /**
     * 单区块生成预算（毫秒），超出后跳过剩余阶段。
     * 可通过 -Dcityloader.maxChunkGenMs=<ms> 调整。
     */
    private static final long MAX_CHUNK_GEN_NANOS = TimeUnit.MILLISECONDS.toNanos(
            Long.getLong("cityloader.maxChunkGenMs", 0L));

    public LostCityTerrainFeature(String type) {
        this.type = type;
        List<GenerationStage> pipeline = new ArrayList<>();

        pipeline.add(new CityCoreStage());
        pipeline.add(new InfrastructureStage());
        pipeline.add(new CorridorStage());
        pipeline.add(new BridgeStage());

        if (!SAFE_MODE) {
            pipeline.add(new ScatteredStage());
            pipeline.add(new CitySphereStage());
            pipeline.add(new MonorailStage());
            pipeline.add(new DamageStage());
            pipeline.add(new RailDungeonStage());
            pipeline.add(new ParkStage());
            pipeline.add(new FountainStage());
            pipeline.add(new MegaSolarStage());
            pipeline.add(new QuarryStage());
            pipeline.add(new OffshoreStage());
            pipeline.add(new LootStage());
            pipeline.add(new SpawnerStage());
        }

        pipeline.add(new PostProcessStage());
        
        this.stages = Collections.unmodifiableList(pipeline);
    }

    public String getType() {
        return type;
    }

    public List<GenerationStage> getStages() {
        return stages;
    }

    public void generate(WorldInfo worldInfo,
                         Random random,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion limitedRegion,
                         IDimensionInfo dimensionInfo) {
        generate(worldInfo, random, chunkX, chunkZ, limitedRegion, dimensionInfo, Season.SPRING);
    }

    public void generate(WorldInfo worldInfo,
                         Random random,
                         int chunkX,
                         int chunkZ,
                         LimitedRegion limitedRegion,
                         IDimensionInfo dimensionInfo,
                         Season season) {
        GenerationContext context = null;
        if (dimensionInfo instanceof PaperDimensionInfo paperDimensionInfo) {
            paperDimensionInfo.beginChunkGeneration(limitedRegion, chunkX, chunkZ);
        }
        long startNanos = System.nanoTime();
        try {
            String dimension = dimensionInfo.dimension() != null ? dimensionInfo.dimension() : worldInfo.getName();
            ChunkCoord coord = new ChunkCoord(dimension, chunkX, chunkZ);
            BuildingInfo buildingInfo = BuildingInfo.getBuildingInfo(coord, dimensionInfo);

            context = new GenerationContext(
                    worldInfo,
                    limitedRegion,
                    dimensionInfo,
                    buildingInfo,
                    random,
                    chunkX,
                    chunkZ,
                    season);

            for (GenerationStage stage : stages) {
                stage.generate(context);
                if (MAX_CHUNK_GEN_NANOS > 0 && System.nanoTime() - startNanos > MAX_CHUNK_GEN_NANOS) {
                    break;
                }
            }
        } finally {
            if (context != null) {
                context.flush();
            }
            ChunkFixer.fix(dimensionInfo, chunkX, chunkZ);
            if (dimensionInfo instanceof PaperDimensionInfo paperDimensionInfo) {
                paperDimensionInfo.endChunkGeneration();
            }
        }
    }
}
