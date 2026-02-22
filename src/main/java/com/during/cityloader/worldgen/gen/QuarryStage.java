package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;

import java.util.Random;

/**
 * 采石场生成阶段
 * 生成地下采石场结构
 * 兼容pomkots资源包quarry建筑
 */
public class QuarryStage implements GenerationStage {

    private static final int QUARRY_DEPTH = -12;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        Random random = context.getRandom();
        
        if (random.nextFloat() > 0.05f) {
            return;
        }
        
        int cityGroundY = info.getCityGroundLevel();
        generateQuarry(context, cityGroundY, random);
    }

    private void generateQuarry(GenerationContext context, int baseY, Random random) {
        int quarryTop = baseY + QUARRY_DEPTH;
        
        int startX = 2;
        int startZ = 2;
        int width = 12;
        int depth = 12;
        
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + depth; z++) {
                for (int y = quarryTop; y >= quarryTop - 8; y--) {
                    if (y < context.getWorldInfo().getMinHeight()) break;
                    context.setBlock(x, y, z, Material.AIR);
                }
            }
        }
        
        generateQuarryWalls(context, startX, startZ, width, depth, quarryTop, random);
        generateQuarryFloors(context, startX, startZ, width, depth, quarryTop, random);
        generateQuarrySupports(context, startX, startZ, width, depth, quarryTop, random);
    }

    private void generateQuarryWalls(GenerationContext context, int startX, int startZ, 
                                     int width, int depth, int topY, Random random) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = topY; y >= topY - 8; y--) {
                context.setBlock(x, y, startZ, Material.COBBLESTONE_WALL);
                context.setBlock(x, y, startZ + depth - 1, Material.COBBLESTONE_WALL);
            }
        }
        
        for (int z = startZ; z < startZ + depth; z++) {
            for (int y = topY; y >= topY - 8; y--) {
                context.setBlock(startX, y, z, Material.COBBLESTONE_WALL);
                context.setBlock(startX + width - 1, y, z, Material.COBBLESTONE_WALL);
            }
        }
    }

    private void generateQuarryFloors(GenerationContext context, int startX, int startZ, 
                                      int width, int depth, int topY, Random random) {
        int floorY = topY - 8;
        
        for (int x = startX + 1; x < startX + width - 1; x++) {
            for (int z = startZ + 1; z < startZ + depth - 1; z++) {
                if (random.nextFloat() > 0.3f) {
                    context.setBlock(x, floorY, z, Material.GRAVEL);
                } else {
                    context.setBlock(x, floorY, z, Material.COBBLESTONE);
                }
            }
        }
        
        for (int x = startX; x < startX + width; x++) {
            context.setBlock(x, floorY - 1, startZ, Material.BEDROCK);
            context.setBlock(x, floorY - 1, startZ + depth - 1, Material.BEDROCK);
        }
        for (int z = startZ; z < startZ + depth; z++) {
            context.setBlock(startX, floorY - 1, z, Material.BEDROCK);
            context.setBlock(startX + width - 1, floorY - 1, z, Material.BEDROCK);
        }
    }

    private void generateQuarrySupports(GenerationContext context, int startX, int startZ, 
                                       int width, int depth, int topY, Random random) {
        int supportHeight = topY;
        
        int[][] supportPositions = {
            {startX + 2, startZ + 2},
            {startX + width - 3, startZ + 2},
            {startX + 2, startZ + depth - 3},
            {startX + width - 3, startZ + depth - 3},
            {startX + width / 2, startZ + depth / 2}
        };
        
        for (int[] pos : supportPositions) {
            for (int y = supportHeight; y >= topY - 8; y--) {
                context.setBlock(pos[0], y, pos[1], Material.DARK_OAK_LOG);
            }
        }
        
        for (int x = startX + 1; x < startX + width - 1; x++) {
            if (random.nextFloat() > 0.5f) {
                context.setBlock(x, supportHeight + 1, startZ + 1, Material.TORCH);
            }
        }
    }
}
