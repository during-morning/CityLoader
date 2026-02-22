package com.during.cityloader.worldgen.gen;

import com.during.cityloader.season.Season;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import org.bukkit.Material;

import java.util.Random;

/**
 * 喷泉生成阶段
 * 在城市内生成喷泉装饰
 */
public class FountainStage implements GenerationStage {

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        float fountainChance = profile.getFountainChance();
        if (fountainChance <= 0) {
            return;
        }
        
        Random random = context.getRandom();
        if (random.nextFloat() > fountainChance) {
            return;
        }
        
        CityStyle cityStyle = info.getCityStyle();
        if (cityStyle != null) {
            String fountainPart = cityStyle.pickSelectorValue("fountains", random);
            if (fountainPart != null && !fountainPart.isBlank()) {
                generateFountainPart(context, fountainPart, info);
                return;
            }
        }
        
        generateDefaultFountain(context, info, random);
    }

    private void generateFountainPart(GenerationContext context, String fountainPart, BuildingInfo info) {
        // TODO: 完成后端BuildingPart加载后实现
        generateDefaultFountain(context, info, context.getRandom());
    }

    private void generateDefaultFountain(GenerationContext context, BuildingInfo info, Random random) {
        int cityGroundY = info.getCityGroundLevel();
        Season season = context.getSeason();
        
        int[] positions = {3, 7, 12};
        
        int fx = positions[random.nextInt(positions.length)];
        int fz = positions[random.nextInt(positions.length)];
        
        generateBasicFountain(context, fx, fz, cityGroundY, random, season);
        
        if (random.nextBoolean()) {
            int fx2 = 16 - fx;
            int fz2 = 16 - fz;
            generateBasicFountain(context, fx2, fz2, cityGroundY, random, season);
        }
    }

    private void generateBasicFountain(GenerationContext context, int x, int z, int baseY, Random random, Season season) {
        Material baseBlock = Material.STONE_BRICKS;
        Material rimBlock = Material.COBBLESTONE;
        Material waterBlock = season == Season.WINTER ? Material.ICE : Material.WATER;
        
        context.setBlock(x, baseY - 1, z, baseBlock);
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.setBlock(x + dx, baseY, z + dz, rimBlock);
            }
        }
        
        context.setBlock(x, baseY + 1, z, waterBlock);
        
        if (random.nextFloat() > 0.5f) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (Math.abs(dx) + Math.abs(dz) == 1) {
                        context.setBlock(x + dx, baseY + 1, z + dz, Material.GLOWSTONE);
                    }
                }
            }
        }
        
        if (random.nextFloat() > 0.7f) {
            context.setBlock(x, baseY + 2, z, Material.DARK_PRISMARINE);
            context.setBlock(x, baseY + 3, z, Material.DARK_PRISMARINE_STAIRS);
        }
    }
}
