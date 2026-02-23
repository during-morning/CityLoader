package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

/**
 * 生成器(Spawner)生成阶段
 * 在建筑和地下区域生成各种生物的生成器
 */
public class SpawnerStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;
    private static final float FLOOR_SPAWNER_CHANCE = 0.45f;
    private static final float CELLAR_SPAWNER_CHANCE = 0.75f;
    private static final int MAX_SPAWNERS_PER_FLOOR = 2;
    private static final int MAX_SPAWNERS_PER_CELLAR = 3;
    private static final int SPAWNER_PLACE_ATTEMPTS = 6;
    
    private static final String[] DEFAULT_MOBS = {
        // 僵尸系高权重
        "minecraft:zombie", "minecraft:zombie", "minecraft:zombie", "minecraft:zombie",
        "minecraft:zombie", "minecraft:zombie", "minecraft:zombie", "minecraft:zombie",
        "minecraft:zombie_villager", "minecraft:zombie_villager", "minecraft:zombie_villager",
        "minecraft:husk", "minecraft:husk", "minecraft:drowned",
        // 工程僵尸候选（若服务端未注册，将在解析层回退为僵尸村民）
        "keerdm_zombie_essentials:engineer_zombie",
        "minecraft:skeleton", "minecraft:spider", "minecraft:creeper"
    };
    
    private static final String[] NETHER_MOBS = {
        "minecraft:blaze", "minecraft:magma_cube", "minecraft:wither_skeleton",
        "minecraft:ghast", "minecraft:piglin"
    };

    @Override
    public void generate(GenerationContext context) {
        // 明确禁用刷怪笼生成（用户需求：不要刷怪笼）
        return;
        /*
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!profile.isGenerateSpawners()) {
            return;
        }
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        Random random = context.getRandom();
        int cityGroundY = info.getCityGroundLevel();
        boolean isNether = context.getWorldInfo() != null
                && context.getWorldInfo().getEnvironment() == org.bukkit.World.Environment.NETHER;
        
        for (int floor = 0; floor < info.floors; floor++) {
            if (random.nextFloat() > FLOOR_SPAWNER_CHANCE) {
                continue;
            }
            
            int floorY = cityGroundY + floor * FLOOR_HEIGHT;
            generateFloorSpawners(context, floorY, random, isNether);
        }
        
        for (int cellar = 1; cellar <= info.cellars; cellar++) {
            if (random.nextFloat() > CELLAR_SPAWNER_CHANCE) {
                continue;
            }
            
            int cellarY = cityGroundY - cellar * FLOOR_HEIGHT;
            generateCellarSpawners(context, cellarY, random, isNether);
        }
        */
    }

    private void generateFloorSpawners(GenerationContext context, int floorY, Random random, boolean isNether) {
        int[] spawnerX = {3, 7, 12};
        int[] spawnerZ = {3, 7, 12};

        int target = 1 + random.nextInt(MAX_SPAWNERS_PER_FLOOR);
        int placed = 0;
        int attempts = 0;
        while (placed < target && attempts < SPAWNER_PLACE_ATTEMPTS) {
            attempts++;
            int sx = spawnerX[random.nextInt(spawnerX.length)];
            int sz = spawnerZ[random.nextInt(spawnerZ.length)];
            if (context.getBlockType(sx, floorY, sz) != Material.AIR) {
                continue;
            }
            context.setBlock(sx, floorY, sz, Material.SPAWNER);
            context.queueSpawnerMob(sx, floorY, sz, getRandomMob(random, isNether));
            placed++;
        }
    }

    private void generateCellarSpawners(GenerationContext context, int cellarY, Random random, boolean isNether) {
        int[] spawnerX = {2, 4, 10, 13};
        int[] spawnerZ = {2, 4, 10, 13};

        int target = 1 + random.nextInt(MAX_SPAWNERS_PER_CELLAR);
        int placed = 0;
        int attempts = 0;
        while (placed < target && attempts < SPAWNER_PLACE_ATTEMPTS * 2) {
            attempts++;
            int sx = spawnerX[random.nextInt(spawnerX.length)];
            int sz = spawnerZ[random.nextInt(spawnerZ.length)];
            if (context.getBlockType(sx, cellarY, sz) != Material.AIR) {
                continue;
            }
            context.setBlock(sx, cellarY, sz, Material.SPAWNER);
            context.queueSpawnerMob(sx, cellarY, sz, getRandomMob(random, isNether));
            placed++;
            if (random.nextFloat() > 0.5f) {
                context.setBlock(sx, cellarY + 1, sz, Material.COBWEB);
            }
        }
    }

    private String getRandomMob(Random random, boolean isNether) {
        if (isNether) {
            return NETHER_MOBS[random.nextInt(NETHER_MOBS.length)];
        }
        return DEFAULT_MOBS[random.nextInt(DEFAULT_MOBS.length)];
    }
}
