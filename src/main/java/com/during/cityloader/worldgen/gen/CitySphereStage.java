package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import com.during.cityloader.worldgen.lost.cityassets.BuildingPart;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import com.during.cityloader.worldgen.lost.cityassets.Palette;
import com.during.cityloader.worldgen.lost.cityassets.PredefinedSphere;
import com.during.cityloader.worldgen.lost.cityassets.WorldStyle;
import org.bukkit.Material;
import org.bukkit.block.data.Rail;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 球体城市生成阶段
 * 在玻璃球中生成城市
 */
public class CitySphereStage implements GenerationStage {

    private static final long SPHERE_GRID_SALT = 0x7FFFFFFFFFFFFFFFL;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!profile.isCitySphereEnabled()) {
            return;
        }
        
        if (!profile.getLandscapeType().equals("space") && 
            !profile.getLandscapeType().equals("spheres") &&
            !profile.getLandscapeType().equals("cavernspheres")) {
            return;
        }

        int chunkX = context.getChunkX();
        int chunkZ = context.getChunkZ();
        
        int gridSize = profile.isCitySphere32Grid() ? 32 : 16;
        int gridX = Math.floorDiv(chunkX, gridSize);
        int gridZ = Math.floorDiv(chunkZ, gridSize);
        
        Random gridRandom = gridRandom(context.getDimensionInfo().getSeed(), gridX, gridZ);
        float chance = profile.getCitySphereChance();
        
        if (gridRandom.nextFloat() >= chance) {
            return;
        }

        int sphereCenterX = gridX * gridSize + gridSize / 2;
        int sphereCenterZ = gridZ * gridSize + gridSize / 2;
        
        int sphereRadius = 8 + gridRandom.nextInt(8);
        
        double distanceFromCenter = Math.sqrt(
            Math.pow(chunkX - sphereCenterX, 2) + 
            Math.pow(chunkZ - sphereCenterZ, 2)
        );
        
        if (distanceFromCenter > sphereRadius) {
            return;
        }

        generateSphereShell(context, sphereCenterX, sphereCenterZ, sphereRadius, profile, gridRandom);
        
        // 使用BuildingInfo中的单轨检测方法
        if (context.getBuildingInfo().hasHorizontalMonorail()) {
            generateHorizontalMonorail(context, profile, gridRandom);
        }
        if (context.getBuildingInfo().hasVerticalMonorail()) {
            generateVerticalMonorail(context, profile, gridRandom);
        }
    }

    private void generateSphereShell(GenerationContext context, 
                                    int centerX, int centerZ, 
                                    int radius, LostCityProfile profile,
                                    Random random) {
        BuildingInfo info = context.getBuildingInfo();
        
        int clearAbove = profile.getCitySphereClearAbove();
        int clearBelow = profile.getCitySphereClearBelow();
        int surfaceVariation = (int) profile.getCitySphereSurfaceVariation();
        
        int sphereBaseY = profile.getGroundLevel();
        
        ChunkHeightmap heightmap = context.getDimensionInfo().getHeightmap(
            context.getChunkX(), context.getChunkZ());
        
        int localCenterX = (context.getChunkX() << 4) + 8 - centerX;
        int localCenterZ = (context.getChunkZ() << 4) + 8 - centerZ;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (context.getChunkX() << 4) + x;
                int worldZ = (context.getChunkZ() << 4) + z;
                
                double distFromCenter = Math.sqrt(
                    Math.pow(worldX - centerX, 2) + 
                    Math.pow(worldZ - centerZ, 2)
                );
                
                if (distFromCenter > radius + 2) {
                    continue;
                }
                
                double normalizedDist = distFromCenter / radius;
                double shellHeight = Math.sqrt(1 - normalizedDist * normalizedDist) * radius;
                
                int sphereSurfaceY = sphereBaseY + (int)(shellHeight * 6);
                
                if (surfaceVariation > 0) {
                    sphereSurfaceY += (random.nextInt(surfaceVariation * 2) - surfaceVariation);
                }
                
                int clearAboveY = sphereSurfaceY + clearAbove;
                int clearBelowY = sphereSurfaceY - clearBelow;
                
                for (int y = sphereSurfaceY - radius; y <= sphereSurfaceY + radius; y++) {
                    if (y < context.getWorldInfo().getMinHeight() || y >= context.getWorldInfo().getMaxHeight()) {
                        continue;
                    }
                    
                    double shellDist = Math.abs(y - sphereSurfaceY);
                    if (shellDist < shellHeight * 0.7) {
                        if (y >= clearBelowY && y <= clearAboveY) {
                            continue;
                        }
                        context.setBlock(x, y, z, Material.AIR);
                    } else if (shellDist < shellHeight) {
                        context.setBlock(x, y, z, Material.GLASS);
                    }
                }
                
                if (normalizedDist < 0.3) {
                    info.isCity = true;
                    info.hasBuilding = true;
                    info.groundLevel = sphereBaseY + 6;
                    info.floors = 2 + random.nextInt(6);
                    info.cellars = 0;
                }
            }
        }
    }

    private void generateHorizontalMonorail(GenerationContext context, LostCityProfile profile, Random random) {
        int heightOffset = profile.getCitySphereMonorailHeightOffset();
        int railY = profile.getGroundLevel() + heightOffset;
        
        for (int x = 0; x < 16; x++) {
            context.setRail(x, railY, 7, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setRail(x, railY, 8, Material.RAIL, Rail.Shape.EAST_WEST, false);
            context.setBlock(x, railY - 1, 7, Material.IRON_BLOCK);
            context.setBlock(x, railY - 1, 8, Material.IRON_BLOCK);
        }
    }

    private void generateVerticalMonorail(GenerationContext context, LostCityProfile profile, Random random) {
        int heightOffset = profile.getCitySphereMonorailHeightOffset();
        int railY = profile.getGroundLevel() + heightOffset;
        
        for (int z = 0; z < 16; z++) {
            context.setRail(7, railY, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setRail(8, railY, z, Material.RAIL, Rail.Shape.NORTH_SOUTH, false);
            context.setBlock(7, railY - 1, z, Material.IRON_BLOCK);
            context.setBlock(8, railY - 1, z, Material.IRON_BLOCK);
        }
    }

    private Random gridRandom(long worldSeed, int gridX, int gridZ) {
        long seed = worldSeed;
        seed ^= (long) gridX * 341873128712L;
        seed ^= (long) gridZ * 132897987541L;
        seed ^= SPHERE_GRID_SALT;
        return new Random(seed);
    }
}
