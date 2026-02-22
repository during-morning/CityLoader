package com.during.cityloader.worldgen.gen;

import com.during.cityloader.season.Season;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import org.bukkit.Material;

import java.util.Random;

/**
 * 公园生成阶段
 * 在城市内生成公园区域
 */
public class ParkStage implements GenerationStage {

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        float parkChance = profile.getParkChance();
        if (parkChance <= 0) {
            return;
        }
        
        Random random = context.getRandom();
        if (random.nextFloat() > parkChance) {
            return;
        }
        
        CityStyle cityStyle = info.getCityStyle();
        if (cityStyle == null) {
            generateDefaultPark(context, info);
            return;
        }
        
        String parkPart = cityStyle.pickSelectorValue("parks", random);
        if (parkPart != null && !parkPart.isBlank()) {
            generateParkPart(context, parkPart, info, cityStyle);
        } else {
            generateDefaultPark(context, info);
        }
    }

    private void generateParkPart(GenerationContext context, String parkPart, BuildingInfo info, CityStyle cityStyle) {
        // TODO: 完成后端BuildingPart加载后实现
        generateDefaultPark(context, info);
    }

    private void generateDefaultPark(GenerationContext context, BuildingInfo info) {
        Random random = context.getRandom();
        Season season = context.getSeason();
        int cityGroundY = info.getCityGroundLevel();
        
        boolean elevation = context.getDimensionInfo().getProfile().isParkElevation();
        boolean border = context.getDimensionInfo().getProfile().isParkBorder();
        
        int parkY = elevation ? cityGroundY + 1 : cityGroundY;
        
        int[] parkX = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        int[] parkZ = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        Material surface = season == Season.WINTER ? Material.SNOW_BLOCK : Material.GRASS_BLOCK;
        Material borderBlock = season == Season.WINTER ? Material.COBBLESTONE : Material.STONE_BRICKS;

        for (int x : parkX) {
            for (int z : parkZ) {
                context.setBlock(x, parkY - 1, z, surface);
            }
        }
        
        if (border) {
            for (int x : parkX) {
                context.setBlock(x, parkY, 2, borderBlock);
                context.setBlock(x, parkY, 13, borderBlock);
            }
            for (int z : parkZ) {
                context.setBlock(2, parkY, z, borderBlock);
                context.setBlock(13, parkY, z, borderBlock);
            }
        }
        
        generateTrees(context, parkX, parkZ, parkY, random, season);
        generateFlowers(context, parkX, parkZ, parkY, random, season);
        generatePaths(context, parkX, parkZ, parkY, random, season);
        applySnowCover(context, parkX, parkZ, parkY, season);
    }

    private void generateTrees(GenerationContext context, int[] parkX, int[] parkZ, int parkY, Random random, Season season) {
        int treeCount = 2 + random.nextInt(3);
        Material[] palette = treePalette(season, random);
        Material log = palette[0];
        Material leaves = palette[1];
        
        for (int i = 0; i < treeCount; i++) {
            int tx = parkX[2 + random.nextInt(parkX.length - 4)];
            int tz = parkZ[2 + random.nextInt(parkZ.length - 4)];
            
            context.setBlock(tx, parkY, tz, log);
            context.setBlock(tx, parkY + 1, tz, log);
            context.setBlock(tx - 1, parkY + 2, tz, leaves);
            context.setBlock(tx + 1, parkY + 2, tz, leaves);
            context.setBlock(tx, parkY + 2, tz - 1, leaves);
            context.setBlock(tx, parkY + 2, tz + 1, leaves);
            context.setBlock(tx, parkY + 3, tz, leaves);
        }
    }

    private void generateFlowers(GenerationContext context, int[] parkX, int[] parkZ, int parkY, Random random, Season season) {
        Material[] flowers = flowerPalette(season);
        if (flowers.length == 0) {
            return;
        }
        int flowerCount = switch (season) {
            case SPRING -> 5 + random.nextInt(5);
            case SUMMER -> 3 + random.nextInt(5);
            case AUTUMN -> 1 + random.nextInt(3);
            case WINTER -> 0;
        };
        if (flowerCount <= 0) {
            return;
        }
        
        for (int i = 0; i < flowerCount; i++) {
            int fx = parkX[1 + random.nextInt(parkX.length - 2)];
            int fz = parkZ[1 + random.nextInt(parkZ.length - 2)];
            
            if (context.getBlockType(fx, parkY, fz) == Material.AIR) {
                context.setBlock(fx, parkY, fz, flowers[random.nextInt(flowers.length)]);
            }
        }
    }

    private void generatePaths(GenerationContext context, int[] parkX, int[] parkZ, int parkY, Random random, Season season) {
        Material pathMaterial = season == Season.WINTER ? Material.COBBLESTONE : Material.GRAVEL;
        for (int x : parkX) {
            if (random.nextFloat() < 0.3f) {
                context.setBlock(x, parkY, 7, pathMaterial);
                context.setBlock(x, parkY, 8, pathMaterial);
            }
        }
        
        for (int z : parkZ) {
            if (random.nextFloat() < 0.3f) {
                context.setBlock(7, parkY, z, pathMaterial);
                context.setBlock(8, parkY, z, pathMaterial);
            }
        }
    }

    private void applySnowCover(GenerationContext context, int[] parkX, int[] parkZ, int parkY, Season season) {
        if (season != Season.WINTER) {
            return;
        }
        for (int x : parkX) {
            for (int z : parkZ) {
                if (context.getBlockType(x, parkY, z) != Material.AIR) {
                    continue;
                }
                Material below = context.getBlockType(x, parkY - 1, z);
                if (below != null && below.isSolid()) {
                    context.setBlock(x, parkY, z, Material.SNOW);
                }
            }
        }
    }

    private Material[] treePalette(Season season, Random random) {
        return switch (season) {
            case SPRING -> random.nextBoolean()
                    ? new Material[]{Material.BIRCH_LOG, Material.BIRCH_LEAVES}
                    : new Material[]{Material.OAK_LOG, Material.OAK_LEAVES};
            case SUMMER -> random.nextBoolean()
                    ? new Material[]{Material.OAK_LOG, Material.OAK_LEAVES}
                    : new Material[]{Material.JUNGLE_LOG, Material.JUNGLE_LEAVES};
            case AUTUMN -> random.nextBoolean()
                    ? new Material[]{Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES}
                    : new Material[]{Material.OAK_LOG, Material.ACACIA_LEAVES};
            case WINTER -> new Material[]{Material.SPRUCE_LOG, Material.SPRUCE_LEAVES};
        };
    }

    private Material[] flowerPalette(Season season) {
        return switch (season) {
            case SPRING -> new Material[]{
                    Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM,
                    Material.AZURE_BLUET, Material.RED_TULIP, Material.PINK_TULIP,
                    Material.WHITE_TULIP, Material.ORANGE_TULIP, Material.CORNFLOWER,
                    Material.LILY_OF_THE_VALLEY
            };
            case SUMMER -> new Material[]{
                    Material.POPPY, Material.DANDELION, Material.AZURE_BLUET,
                    Material.CORNFLOWER, Material.OXEYE_DAISY
            };
            case AUTUMN -> new Material[]{
                    Material.ORANGE_TULIP, Material.RED_TULIP, Material.POPPY
            };
            case WINTER -> new Material[0];
        };
    }
}
