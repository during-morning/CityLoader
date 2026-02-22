package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;
import org.bukkit.block.data.Rail;

import java.util.Random;

/**
 * 单轨列车生成阶段
 * 在城市间生成单轨列车系统
 * 兼容LostCities Monorails系统
 */
public class MonorailStage implements GenerationStage {

    private static final int MONORAIL_HEIGHT_OFFSET = -2;
    private static final int FLOOR_HEIGHT = 6;

    public enum RailType {
        NONE,
        HORIZONTAL,
        VERTICAL,
        STATION_SURFACE,
        STATION_UNDERGROUND,
        RAILS_END_HERE,
        THREE_SPLIT,
        DOUBLE_BEND,
        GOING_DOWN_ONE,
        GOING_DOWN_FURTHER,
        GOING_UP_ONE,
        GOING_UP_FURTHER
    }

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!profile.isCitySphereEnabled()) {
            return;
        }
        
        if (!info.isCity) {
            return;
        }
        
        float monorailChance = profile.getCitySphereMonorailChance();
        if (monorailChance <= 0) {
            return;
        }
        
        Random random = context.getRandom();
        if (random.nextFloat() > monorailChance) {
            return;
        }
        
        String landscapeType = profile.getLandscapeType();
        if (!landscapeType.equals("space") && !landscapeType.equals("spheres")) {
            return;
        }
        
        generateMonorail(context, info, profile, random);
    }

    private void generateMonorail(GenerationContext context, BuildingInfo info, 
                                LostCityProfile profile, Random random) {
        int cityGroundY = info.getCityGroundLevel();
        int monorailY = cityGroundY + profile.getCitySphereMonorailHeightOffset();
        
        int chunkX = context.getChunkX();
        int chunkZ = context.getChunkZ();
        
        RailType railType = determineRailType(chunkX, chunkZ, random);
        
        switch (railType) {
            case HORIZONTAL -> generateHorizontalRail(context, monorailY, random);
            case VERTICAL -> generateVerticalRail(context, monorailY, random);
            case STATION_SURFACE -> generateStationRail(context, monorailY, true, random);
            case STATION_UNDERGROUND -> generateStationRail(context, monorailY, false, random);
            case RAILS_END_HERE -> generateEndRail(context, monorailY, random);
            case THREE_SPLIT -> generateThreeSplit(context, monorailY, random);
            case DOUBLE_BEND -> generateDoubleBend(context, monorailY, random);
            case GOING_DOWN_ONE, GOING_DOWN_FURTHER -> generateSlopedRail(context, monorailY, -1, random);
            case GOING_UP_ONE, GOING_UP_FURTHER -> generateSlopedRail(context, monorailY, 1, random);
            default -> {}
        }
    }

    private RailType determineRailType(int chunkX, int chunkZ, Random random) {
        int gridX = Math.floorMod(chunkX, 20);
        int gridZ = Math.floorMod(chunkZ, 20);
        
        if (gridX == 0 || gridZ == 0) {
            if (gridX == 10 || gridZ == 10) {
                return random.nextBoolean() ? RailType.STATION_SURFACE : RailType.STATION_UNDERGROUND;
            }
            return random.nextBoolean() ? RailType.HORIZONTAL : RailType.VERTICAL;
        }
        
        if ((gridX == 5 || gridX == 15) && (gridZ == 5 || gridZ == 15)) {
            return RailType.THREE_SPLIT;
        }
        
        if ((gridX == 5 || gridX == 15) && gridZ != 0) {
            return RailType.DOUBLE_BEND;
        }
        
        if (gridZ == 19 || gridX == 19) {
            return RailType.RAILS_END_HERE;
        }
        
        return RailType.NONE;
    }

    private void generateHorizontalRail(GenerationContext context, int y, Random random) {
        for (int x = 0; x < 16; x++) {
            context.setRail(x, y, 7, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setRail(x, y, 8, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setBlock(x, y - 1, 7, Material.IRON_BLOCK);
            context.setBlock(x, y - 1, 8, Material.IRON_BLOCK);
        }
        
        if (random.nextFloat() > 0.7f) {
            generatePillar(context, 0, y, 7);
            generatePillar(context, 15, y, 7);
        }
    }

    private void generateVerticalRail(GenerationContext context, int y, Random random) {
        for (int z = 0; z < 16; z++) {
            context.setRail(7, y, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setRail(8, y, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setBlock(7, y - 1, z, Material.IRON_BLOCK);
            context.setBlock(8, y - 1, z, Material.IRON_BLOCK);
        }
        
        if (random.nextFloat() > 0.7f) {
            generatePillar(context, 7, y, 0);
            generatePillar(context, 7, y, 15);
        }
    }

    private void generateStationRail(GenerationContext context, int y, boolean surface, Random random) {
        int stationY = surface ? y : y - FLOOR_HEIGHT;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 5; z <= 10; z++) {
                context.setBlock(x, stationY, z, Material.SMOOTH_STONE);
            }
        }
        
        for (int x = 0; x < 16; x++) {
            context.setRail(x, stationY, 7, Material.POWERED_RAIL, Rail.Shape.EAST_WEST, false);
            context.setRail(x, stationY, 8, Material.POWERED_RAIL, Rail.Shape.EAST_WEST, false);
        }
        
        for (int x = 2; x <= 4; x++) {
            context.setBlock(x, stationY + 1, 6, Material.GLOWSTONE);
            context.setBlock(x, stationY + 1, 9, Material.GLOWSTONE);
        }
        
        generateStationBuilding(context, stationY, random);
    }

    private void generateStationBuilding(GenerationContext context, int baseY, Random random) {
        int[] signX = {3, 12};
        int[] signZ = {6, 9};
        
        for (int sx : signX) {
            for (int sz : signZ) {
                context.setBlock(sx, baseY + 1, sz, Material.OAK_SIGN);
            }
        }
        
        for (int x = 2; x <= 5; x++) {
            for (int z = 5; z <= 10; z++) {
                context.setBlock(x, baseY + 2, z, Material.IRON_BARS);
            }
        }
    }

    private void generateEndRail(GenerationContext context, int y, Random random) {
        if (random.nextBoolean()) {
            for (int x = 0; x < 16; x++) {
                context.setRail(x, y, 7, Material.RAIL, Rail.Shape.EAST_WEST, false);
                context.setRail(x, y, 8, Material.RAIL, Rail.Shape.EAST_WEST, false);
            }
            context.setBlock(15, y + 1, 7, Material.REDSTONE_BLOCK);
            context.setBlock(15, y + 1, 8, Material.REDSTONE_BLOCK);
        } else {
            for (int z = 0; z < 16; z++) {
                context.setRail(7, y, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
                context.setRail(8, y, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            }
            context.setBlock(7, y + 1, 15, Material.REDSTONE_BLOCK);
            context.setBlock(8, y + 1, 15, Material.REDSTONE_BLOCK);
        }
    }

    private void generateThreeSplit(GenerationContext context, int y, Random random) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (Math.abs(x - 7.5) + Math.abs(z - 7.5) <= 3) {
                    context.setRail(x, y, z, Material.RAIL, inferIntersectionShape(x, z), false);
                }
            }
        }
        
        context.setBlock(7, y + 1, 7, Material.OBSERVER);
        context.setBlock(8, y + 1, 7, Material.OBSERVER);
        context.setBlock(7, y + 1, 8, Material.OBSERVER);
        context.setBlock(8, y + 1, 8, Material.OBSERVER);
    }

    private void generateDoubleBend(GenerationContext context, int y, Random random) {
        for (int i = 0; i < 16; i++) {
            context.setRail(i, y, 7, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setRail(i, y, 8, Material.RAIL, Rail.Shape.EAST_WEST, false);
            
            if (i >= 5 && i <= 10) {
                context.setRail(7, y, i, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
                context.setRail(8, y, i, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            }
        }
    }

    private void generateSlopedRail(GenerationContext context, int y, int direction, Random random) {
        for (int i = 0; i < 16; i++) {
            int currentY = y + (direction > 0 ? i / 4 : -i / 4);
            int prevY = i == 0 ? currentY : y + (direction > 0 ? (i - 1) / 4 : -(i - 1) / 4);
            int nextY = i == 15 ? currentY : y + (direction > 0 ? (i + 1) / 4 : -(i + 1) / 4);
            Rail.Shape shape = Rail.Shape.EAST_WEST;
            if (nextY > currentY) {
                shape = Rail.Shape.ASCENDING_EAST;
            } else if (prevY > currentY) {
                shape = Rail.Shape.ASCENDING_WEST;
            }
            context.setRail(i, currentY, 7, Material.RAIL, shape, false);
            context.setRail(i, currentY, 8, Material.RAIL, shape, false);
            
            for (int sy = currentY - 1; sy >= y - FLOOR_HEIGHT; sy--) {
                context.setBlock(i, sy, 7, Material.IRON_BLOCK);
                context.setBlock(i, sy, 8, Material.IRON_BLOCK);
            }
        }
    }

    private Rail.Shape inferIntersectionShape(int x, int z) {
        if (x == 7 || x == 8) {
            return Rail.Shape.NORTH_SOUTH;
        }
        if (z == 7 || z == 8) {
            return Rail.Shape.EAST_WEST;
        }
        if (x < 7 && z < 7) {
            return Rail.Shape.SOUTH_EAST;
        }
        if (x > 8 && z < 7) {
            return Rail.Shape.SOUTH_WEST;
        }
        if (x < 7 && z > 8) {
            return Rail.Shape.NORTH_EAST;
        }
        if (x > 8 && z > 8) {
            return Rail.Shape.NORTH_WEST;
        }
        return Rail.Shape.EAST_WEST;
    }

    private void generatePillar(GenerationContext context, int x, int y, int z) {
        for (int py = y - 1; py >= context.getWorldInfo().getMinHeight(); py--) {
            context.setBlock(x, py, z, Material.IRON_BLOCK);
            if (context.getBlockType(x + 1, py, z) == Material.AIR) {
                context.setBlock(x + 1, py, z, Material.IRON_BARS);
            }
            if (context.getBlockType(x - 1, py, z) == Material.AIR) {
                context.setBlock(x - 1, py, z, Material.IRON_BARS);
            }
        }
    }
}
