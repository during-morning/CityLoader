package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.Railway;
import org.bukkit.Material;
import org.bukkit.block.data.Rail;

import java.util.Random;

/**
 * 铁路地牢生成阶段
 * 在铁路系统附近生成地下密室
 */
public class RailDungeonStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCityRaw()) {
            return;
        }
        
        if (!profile.isRailwaysEnabled()) {
            return;
        }
        
        float dungeonChance = profile.getRailwayDungeonChance();
        if (dungeonChance <= 0) {
            return;
        }
        
        Random random = context.getRandom();
        if (random.nextFloat() > dungeonChance) {
            return;
        }
        
        ChunkCoord coord = new ChunkCoord(context.getWorldInfo().getName(), context.getChunkX(), context.getChunkZ());
        Railway.RailChunkInfo railInfo = Railway.getRailChunkType(coord, context.getDimensionInfo(), profile);
        Railway.RailChunkType railType = railInfo.getType();
        if (railType == Railway.RailChunkType.NONE) {
            return;
        }
        if (railInfo.getLevel() > Railway.RAILWAY_LEVEL_OFFSET) {
            return;
        }
        
        generateRailDungeon(context, info, profile, random, railInfo);
    }

    private void generateRailDungeon(GenerationContext context, BuildingInfo info, 
                                     LostCityProfile profile, Random random,
                                     Railway.RailChunkInfo railInfo) {
        Railway.RailChunkType railType = railInfo.getType();
        int dungeonY = info.groundLevel + railInfo.getLevel() * FLOOR_HEIGHT;
        
        boolean isXCorridor = railType == Railway.RailChunkType.HORIZONTAL
                || railType == Railway.RailChunkType.STATION_SURFACE
                || railType == Railway.RailChunkType.STATION_UNDERGROUND
                || railType == Railway.RailChunkType.STATION_EXTENSION_SURFACE
                || railType == Railway.RailChunkType.STATION_EXTENSION_UNDERGROUND
                || railType == Railway.RailChunkType.RAILS_END_HERE
                || railType == Railway.RailChunkType.THREE_SPLIT
                || railType == Railway.RailChunkType.DOUBLE_BEND
                || railType == Railway.RailChunkType.GOING_DOWN_ONE_FROM_SURFACE
                || railType == Railway.RailChunkType.GOING_DOWN_TWO_FROM_SURFACE
                || railType == Railway.RailChunkType.GOING_DOWN_FURTHER;
        boolean isZCorridor = railType == Railway.RailChunkType.VERTICAL
                || railType == Railway.RailChunkType.THREE_SPLIT
                || railType == Railway.RailChunkType.DOUBLE_BEND;
        
        if (isXCorridor) {
            generateDungeonRoom(context, dungeonY, true, profile, random);
        }
        
        if (isZCorridor) {
            generateDungeonRoom(context, dungeonY, false, profile, random);
        }
    }

    private void generateDungeonRoom(GenerationContext context, int baseY, boolean xAxis,
                                     LostCityProfile profile, Random random) {
        int roomX, roomZ, roomWidth, roomDepth;
        
        if (xAxis) {
            roomX = random.nextInt(4) + 6;
            roomZ = 3;
            roomWidth = 6;
            roomDepth = 10;
        } else {
            roomX = 3;
            roomZ = random.nextInt(4) + 6;
            roomWidth = 10;
            roomDepth = 6;
        }
        
        int floorY = baseY - 3;
        int ceilingY = baseY + 2;
        
        for (int x = roomX - roomWidth/2; x < roomX + roomWidth/2; x++) {
            for (int z = roomZ - roomDepth/2; z < roomZ + roomDepth/2; z++) {
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
                
                context.setBlock(x, floorY, z, Material.STONE_BRICKS);
                
                context.setBlock(x, ceilingY, z, Material.STONE_BRICKS);
            }
        }
        
        for (int x = roomX - roomWidth/2; x < roomX + roomWidth/2; x++) {
            if (x < 0 || x >= 16) continue;
            context.setBlock(x, floorY, roomZ - roomDepth/2, Material.COBBLESTONE_WALL);
            context.setBlock(x, floorY, roomZ + roomDepth/2 - 1, Material.COBBLESTONE_WALL);
        }
        
        for (int z = roomZ - roomDepth/2; z < roomZ + roomDepth/2; z++) {
            if (z < 0 || z >= 16) continue;
            context.setBlock(roomX - roomWidth/2, floorY, z, Material.COBBLESTONE_WALL);
            context.setBlock(roomX + roomWidth/2 - 1, floorY, z, Material.COBBLESTONE_WALL);
        }
        
        generateDungeonChests(context, roomX, roomZ, floorY, random);
        generateDungeonRails(context, roomX, roomZ, baseY, xAxis, random);
    }

    private void generateDungeonChests(GenerationContext context, int roomX, int roomZ, int floorY, Random random) {
        int chestX1 = roomX - 2;
        int chestX2 = roomX + 2;
        int chestZ = roomZ;
        
        if (random.nextFloat() < 0.7f) {
            context.setBlock(chestX1, floorY + 1, chestZ, Material.CHEST);
        }
        
        if (random.nextFloat() < 0.5f) {
            context.setBlock(chestX2, floorY + 1, chestZ, Material.CHEST);
        }
    }

    private void generateDungeonRails(GenerationContext context, int roomX, int roomZ, 
                                       int baseY, boolean xAxis, Random random) {
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                setRail(context, x, baseY, roomZ, Rail.Shape.EAST_WEST);
                context.setBlock(x, baseY - 1, roomZ, Material.STONE_BRICKS);
            }
        } else {
            for (int z = 0; z < 16; z++) {
                setRail(context, roomX, baseY, z, Rail.Shape.NORTH_SOUTH);
                context.setBlock(roomX, baseY - 1, z, Material.STONE_BRICKS);
            }
        }
    }

    private void setRail(GenerationContext context, int x, int y, int z, Rail.Shape shape) {
        context.setRail(x, y, z, Material.RAIL, shape, false);
    }
}
