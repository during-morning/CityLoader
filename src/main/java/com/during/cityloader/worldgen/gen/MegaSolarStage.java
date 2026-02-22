package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;

import java.util.Random;

/**
 * 巨型太阳能发电站生成阶段
 * 生成大面积太阳能板阵列
 * 兼容pomkots资源包megasolar建筑
 */
public class MegaSolarStage implements GenerationStage {

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        Random random = context.getRandom();
        int cityGroundY = info.getCityGroundLevel();
        
        if (random.nextFloat() > 0.05f) {
            return;
        }
        
        generateMegaSolar(context, cityGroundY, random);
    }

    private void generateMegaSolar(GenerationContext context, int baseY, Random random) {
        int startX = 2;
        int startZ = 2;
        int width = 12;
        int depth = 12;
        
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + depth; z++) {
                context.setBlock(x, baseY - 1, z, Material.IRON_BLOCK);
            }
        }
        
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + depth; z++) {
                if ((x + z) % 4 == 0) {
                    context.setBlock(x, baseY, z, Material.IRON_BARS);
                    context.setBlock(x, baseY + 1, z, Material.IRON_BARS);
                } else {
                    context.setBlock(x, baseY, z, Material.DAYLIGHT_DETECTOR);
                    context.setBlock(x, baseY + 1, z, Material.REDSTONE_LAMP);
                }
            }
        }
        
        generateSolarPanels(context, startX, startZ, width, depth, baseY + 2, random);
        
        generatePowerPoles(context, startX, startZ, width, depth, baseY, random);
    }

    private void generateSolarPanels(GenerationContext context, int startX, int startZ, 
                                    int width, int depth, int baseY, Random random) {
        for (int x = startX + 1; x < startX + width - 1; x += 2) {
            for (int z = startZ + 1; z < startZ + depth - 1; z += 2) {
                context.setBlock(x, baseY, z, Material.WHITE_STAINED_GLASS);
                context.setBlock(x + 1, baseY, z, Material.WHITE_STAINED_GLASS);
                context.setBlock(x, baseY, z + 1, Material.WHITE_STAINED_GLASS);
                context.setBlock(x + 1, baseY, z + 1, Material.WHITE_STAINED_GLASS);
                
                context.setBlock(x, baseY - 1, z, Material.IRON_BARS);
                context.setBlock(x + 1, baseY - 1, z, Material.IRON_BARS);
                context.setBlock(x, baseY - 1, z + 1, Material.IRON_BARS);
                context.setBlock(x + 1, baseY - 1, z + 1, Material.IRON_BARS);
            }
        }
    }

    private void generatePowerPoles(GenerationContext context, int startX, int startZ, 
                                   int width, int depth, int baseY, Random random) {
        int[][] polePositions = {
            {startX, startZ},
            {startX + width - 1, startZ},
            {startX, startZ + depth - 1},
            {startX + width - 1, startZ + depth - 1}
        };
        
        for (int[] pos : polePositions) {
            for (int y = baseY + 1; y <= baseY + 4; y++) {
                context.setBlock(pos[0], y, pos[1], Material.IRON_BLOCK);
            }
            
            context.setBlock(pos[0], baseY + 5, pos[1], Material.REDSTONE_BLOCK);
            context.setBlock(pos[0], baseY + 6, pos[1], Material.LIGHTNING_ROD);
        }
    }
}
