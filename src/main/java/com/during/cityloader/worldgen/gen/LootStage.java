package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * 战利品箱子生成阶段
 * 在建筑内生成带有战利品的箱子
 * 支持Keerdm战利品箱子(ammochest, gunchest)和标准Minecraft战利品
 */
public class LootStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity || !info.hasBuilding) {
            return;
        }
        
        Random random = context.getRandom();
        int cityGroundY = info.getCityGroundLevel();
        
        for (int floor = 0; floor < info.floors; floor++) {
            if (random.nextFloat() > 0.3f) {
                continue;
            }
            
            int floorY = cityGroundY + floor * FLOOR_HEIGHT;
            generateFloorLoot(context, floorY, profile, random);
        }
        
        if (random.nextFloat() > profile.getBuildingWithoutLootChance()) {
            int groundFloorY = cityGroundY;
            generateFloorLoot(context, groundFloorY, profile, random);
        }
    }

    private void generateFloorLoot(GenerationContext context, int floorY, 
                                    LostCityProfile profile, Random random) {
        int[] chestX = {2, 13};
        int[] chestZ = {2, 7, 13};
        
        int cx = chestX[random.nextInt(chestX.length)];
        int cz = chestZ[random.nextInt(chestZ.length)];
        
        String lootTable = resolveLootTable(context, floorY, cx, cz, random);
        if (context.getBlockType(cx, floorY, cz) == Material.AIR) {
            Material chestType = random.nextFloat() < 0.8f ? Material.CHEST : Material.TRAPPED_CHEST;
            context.setBlock(cx, floorY, cz, chestType);
            if (profile != null && profile.isGenerateLoot()) {
                context.queueLootTable(cx, floorY, cz, lootTable);
            }
        }
        
        if (random.nextFloat() > 0.7f) {
            int cx2 = 16 - cx;
            int cz2 = 16 - cz;
            
            if (context.getBlockType(cx2, floorY, cz2) == Material.AIR) {
                context.setBlock(cx2, floorY, cz2, Material.CHEST);
                if (profile != null && profile.isGenerateLoot()) {
                    context.queueLootTable(cx2, floorY, cz2, resolveLootTable(context, floorY, cx2, cz2, random));
                }
            }
        }
    }

    private String resolveLootTable(GenerationContext context, int y, int localX, int localZ, Random random) {
        Biome biome = context.getDimensionInfo().getBiome(context.worldX(localX), y, context.worldZ(localZ));
        String biomeName = biome == null ? null : biome.name();
        return getLootTableForBiome(biomeName, random);
    }

    public static String getLootTableForBiome(String biomeName, Random random) {
        if (biomeName == null) {
            return "minecraft:chests/village/village_toolsmith";
        }
        
        String lowerBiome = biomeName.toLowerCase();
        
        if (lowerBiome.contains("desert")) {
            return "minecraft:chests/village/village_desert_house";
        } else if (lowerBiome.contains("taiga") || lowerBiome.contains("snow")) {
            return "minecraft:chests/village/village_snowy_house";
        } else if (lowerBiome.contains("savanna")) {
            return "minecraft:chests/village/village_savanna_house";
        } else if (lowerBiome.contains("jungle")) {
            return "minecraft:chests/jungle_temple";
        } else if (lowerBiome.contains("ocean") || lowerBiome.contains("river")) {
            return "minecraft:chests/shipwreck_treasure";
        } else if (lowerBiome.contains("nether") || lowerBiome.contains("hell")) {
            return "minecraft:chests/nether_bridge";
        } else if (lowerBiome.contains("end")) {
            return "minecraft:chests/end_city_treasure";
        }
        
        return "minecraft:chests/village/village_house";
    }
    
    public static Material resolveKeerdmChest(String namespace, String lootTable, Random random) {
        if (namespace == null || !namespace.equals("keerdm_zombie_essentials")) {
            return Material.CHEST;
        }
        
        if (lootTable != null) {
            if (lootTable.contains("ammo") || lootTable.contains("ammunition")) {
                return Material.CHEST;
            } else if (lootTable.contains("gun") || lootTable.contains("weapon")) {
                return Material.CHEST;
            }
        }
        
        if (random.nextFloat() > 0.5f) {
            return Material.CHEST;
        }
        return Material.CHEST;
    }
    
    public static ItemStack[] generateKeerdmLoot(String lootTable, Random random) {
        ItemStack[] items = new ItemStack[random.nextInt(9) + 3];
        
        if (lootTable != null && lootTable.contains("ammo")) {
            items[0] = new ItemStack(Material.ARROW, random.nextInt(32) + 8);
            items[1] = new ItemStack(Material.TIPPED_ARROW, random.nextInt(16) + 4);
            if (random.nextFloat() > 0.5f) {
                items[2] = new ItemStack(Material.BOW);
            }
        } else if (lootTable != null && lootTable.contains("gun")) {
            items[0] = new ItemStack(Material.IRON_SWORD, random.nextInt(2) + 1);
            items[1] = new ItemStack(Material.ARROW, random.nextInt(16) + 4);
            if (random.nextFloat() > 0.3f) {
                items[2] = new ItemStack(Material.BOW);
            }
            if (random.nextFloat() > 0.6f) {
                items[3] = new ItemStack(Material.TNT, random.nextInt(4) + 1);
            }
        } else {
            items[0] = new ItemStack(Material.IRON_INGOT, random.nextInt(8) + 2);
            items[1] = new ItemStack(Material.GOLD_INGOT, random.nextInt(4) + 1);
            items[2] = new ItemStack(Material.DIAMOND, random.nextInt(2) + 1);
            if (random.nextFloat() > 0.5f) {
                items[3] = new ItemStack(Material.IRON_SWORD);
            }
            if (random.nextFloat() > 0.6f) {
                items[4] = new ItemStack(Material.GOLDEN_APPLE, random.nextInt(2) + 1);
            }
        }
        
        return items;
    }
}
