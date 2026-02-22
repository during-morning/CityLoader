package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;

import java.util.Random;

/**
 * 海上平台/石油钻井生成阶段
 * 生成海上建筑结构
 * 兼容pomkots资源包offshore建筑和Keerdm oilrig
 */
public class OffshoreStage implements GenerationStage {

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        Random random = context.getRandom();
        
        if (random.nextFloat() > 0.03f) {
            return;
        }
        
        int cityGroundY = info.getCityGroundLevel();
        if (cityGroundY > info.waterLevel) {
            return;
        }
        
        generateOffshorePlatform(context, cityGroundY, info.waterLevel, random);
    }

    private void generateOffshorePlatform(GenerationContext context, int groundY, int waterLevel, Random random) {
        int platformY = waterLevel + 1;
        
        int startX = 2;
        int startZ = 2;
        int width = 12;
        int depth = 12;
        
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + depth; z++) {
                for (int y = platformY; y >= groundY; y--) {
                    if (context.getBlockType(x, y, z) == Material.WATER) {
                        context.setBlock(x, y, z, Material.DARK_OAK_LOG);
                    }
                }
            }
        }
        
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + depth; z++) {
                context.setBlock(x, platformY + 1, z, Material.DARK_OAK_PLANKS);
            }
        }
        
        generatePlatformStructures(context, platformY, random);
        generatePiles(context, startX, startZ, width, depth, groundY, random);
    }

    private void generatePlatformStructures(GenerationContext context, int platformY, Random random) {
        context.setBlock(7, platformY + 2, 7, Material.DARK_OAK_LOG);
        context.setBlock(7, platformY + 3, 7, Material.DARK_OAK_LOG);
        context.setBlock(7, platformY + 4, 7, Material.DARK_OAK_LOG);
        
        context.setBlock(7, platformY + 5, 7, Material.WHITE_CONCRETE);
        
        for (int x = 5; x <= 9; x++) {
            for (int z = 5; z <= 9; z++) {
                if (x == 7 && z == 7) continue;
                context.setBlock(x, platformY + 2, z, Material.IRON_BARS);
            }
        }
        
        context.setBlock(3, platformY + 2, 3, Material.CHEST);
        context.setBlock(10, platformY + 2, 3, Material.FURNACE);
        
        context.setBlock(3, platformY + 3, 3, Material.TORCH);
        context.setBlock(10, platformY + 3, 3, Material.TORCH);
        
        generateHelipad(context, platformY, random);
    }

    private void generateHelipad(GenerationContext context, int platformY, Random random) {
        if (random.nextFloat() > 0.5f) {
            return;
        }
        
        int helipadX = 9;
        int helipadZ = 9;
        
        context.setBlock(helipadX, platformY + 3, helipadZ, Material.IRON_BLOCK);
        context.setBlock(helipadX + 1, platformY + 3, helipadZ, Material.IRON_BLOCK);
        context.setBlock(helipadX, platformY + 3, helipadZ + 1, Material.IRON_BLOCK);
        context.setBlock(helipadX + 1, platformY + 3, helipadZ + 1, Material.IRON_BLOCK);
        
        context.setBlock(helipadX, platformY + 4, helipadZ, Material.REDSTONE_BLOCK);
        context.setBlock(helipadX + 1, platformY + 4, helipadZ, Material.REDSTONE_BLOCK);
        context.setBlock(helipadX, platformY + 4, helipadZ + 1, Material.REDSTONE_BLOCK);
        context.setBlock(helipadX + 1, platformY + 4, helipadZ + 1, Material.REDSTONE_BLOCK);
    }

    private void generatePiles(GenerationContext context, int startX, int startZ, 
                               int width, int depth, int bottomY, Random random) {
        int[][] pilePositions = {
            {startX, startZ},
            {startX + width - 1, startZ},
            {startX, startZ + depth - 1},
            {startX + width - 1, startZ + depth - 1},
            {startX + width / 2, startZ},
            {startX, startZ + depth / 2},
            {startX + width - 1, startZ + depth / 2},
            {startX + width / 2, startZ + depth - 1}
        };
        
        for (int[] pos : pilePositions) {
            if (random.nextFloat() > 0.3f) {
                continue;
            }
            
            for (int y = bottomY; y <= context.getWorldInfo().getMaxHeight() - 1; y++) {
                if (context.getBlockType(pos[0], y, pos[1]) == Material.AIR ||
                    context.getBlockType(pos[0], y, pos[1]) == Material.WATER) {
                    context.setBlock(pos[0], y, pos[1], Material.DARK_OAK_LOG);
                }
            }
        }
    }
}
