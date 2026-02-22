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
    
    private static final String[] DEFAULT_MOBS = {
        "minecraft:zombie", "minecraft:skeleton", "minecraft:spider",
        "minecraft:creeper", "minecraft:zombie_villager"
    };
    
    private static final String[] NETHER_MOBS = {
        "minecraft:blaze", "minecraft:magma_cube", "minecraft:wither_skeleton",
        "minecraft:ghast", "minecraft:piglin"
    };

    @Override
    public void generate(GenerationContext context) {
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
        
        for (int floor = 0; floor < info.floors; floor++) {
            if (random.nextFloat() > 0.15f) {
                continue;
            }
            
            int floorY = cityGroundY + floor * FLOOR_HEIGHT;
            generateFloorSpawners(context, floorY, info, random);
        }
        
        for (int cellar = 1; cellar <= info.cellars; cellar++) {
            if (random.nextFloat() > 0.2f) {
                continue;
            }
            
            int cellarY = cityGroundY - cellar * FLOOR_HEIGHT;
            generateCellarSpawners(context, cellarY, info, random);
        }
    }

    private void generateFloorSpawners(GenerationContext context, int floorY, BuildingInfo info, Random random) {
        int[] spawnerX = {3, 7, 12};
        int[] spawnerZ = {3, 7, 12};
        
        int sx = spawnerX[random.nextInt(spawnerX.length)];
        int sz = spawnerZ[random.nextInt(spawnerZ.length)];
        
        if (context.getBlockType(sx, floorY, sz) == Material.AIR) {
            context.setBlock(sx, floorY, sz, Material.SPAWNER);
        }
    }

    private void generateCellarSpawners(GenerationContext context, int cellarY, BuildingInfo info, Random random) {
        int[] spawnerX = {2, 4, 10, 13};
        int[] spawnerZ = {2, 4, 10, 13};
        
        int sx = spawnerX[random.nextInt(spawnerX.length)];
        int sz = spawnerZ[random.nextInt(spawnerZ.length)];
        
        if (context.getBlockType(sx, cellarY, sz) == Material.AIR) {
            context.setBlock(sx, cellarY, sz, Material.SPAWNER);
            
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
