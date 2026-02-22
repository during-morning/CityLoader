package com.during.cityloader.worldgen.gen;

import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import org.bukkit.Material;

import java.util.Random;

/**
 * 桥梁生成阶段
 * 在城市间跨越峡谷生成桥梁
 * 兼容LostCities Bridges系统
 */
public class BridgeStage implements GenerationStage {

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (!info.isCity) {
            return;
        }
        
        if (!info.xBridge && !info.zBridge) {
            return;
        }

        Random random = context.getRandom();
        
        if (info.xBridge) {
            generateBridge(context, info.highwayXLevel, true, profile, random);
        }
        
        if (info.zBridge) {
            generateBridge(context, info.highwayZLevel, false, profile, random);
        }
    }

    private void generateBridge(GenerationContext context, int baseY, boolean xAxis,
                                LostCityProfile profile, Random random) {
        if (baseY <= 0) {
            return;
        }

        int bridgeY = baseY;
        boolean hasDeck = hasBridgeDeck(context, bridgeY, xAxis);

        if (!hasDeck) {
            if (xAxis) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 6; z < 10; z++) {
                        context.setBlock(x, bridgeY, z, Material.POLISHED_ANDESITE);
                    }
                }
            } else {
                for (int z = 0; z < 16; z++) {
                    for (int x = 6; x < 10; x++) {
                        context.setBlock(x, bridgeY, z, Material.POLISHED_ANDESITE);
                    }
                }
            }

            generateBridgeRailings(context, bridgeY, xAxis, random);
        }

        if (profile.isBridgeSupports()) {
            generateBridgeSupports(context, bridgeY, xAxis, random);
        }
    }

    private boolean hasBridgeDeck(GenerationContext context, int bridgeY, boolean xAxis) {
        int solidCount = 0;
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                for (int z = 6; z < 10; z++) {
                    if (!isAirOrFluid(context.getBlockType(x, bridgeY, z))) {
                        solidCount++;
                    }
                }
            }
        } else {
            for (int z = 0; z < 16; z++) {
                for (int x = 6; x < 10; x++) {
                    if (!isAirOrFluid(context.getBlockType(x, bridgeY, z))) {
                        solidCount++;
                    }
                }
            }
        }
        return solidCount >= 16;
    }

    private boolean isAirOrFluid(Material material) {
        if (material == null) {
            return true;
        }
        return material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.WATER
                || material == Material.LAVA
                || material == Material.BUBBLE_COLUMN;
    }

    private void generateBridgeSupports(GenerationContext context, int bridgeY, boolean xAxis, Random random) {
        int supportInterval = 4 + random.nextInt(3);
        
        if (xAxis) {
            for (int x = 0; x < 16; x += supportInterval) {
                for (int z = 6; z <= 9; z++) {
                    for (int y = bridgeY - 1; y >= context.getWorldInfo().getMinHeight(); y--) {
                        if (context.getBlockType(x, y, z) == Material.AIR) {
                            context.setBlock(x, y, z, Material.STONE_BRICKS);
                        } else {
                            break;
                        }
                    }
                }
            }
        } else {
            for (int z = 0; z < 16; z += supportInterval) {
                for (int x = 6; x <= 9; x++) {
                    for (int y = bridgeY - 1; y >= context.getWorldInfo().getMinHeight(); y--) {
                        if (context.getBlockType(x, y, z) == Material.AIR) {
                            context.setBlock(x, y, z, Material.STONE_BRICKS);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void generateBridgeRailings(GenerationContext context, int bridgeY, boolean xAxis, Random random) {
        if (xAxis) {
            for (int x = 0; x < 16; x++) {
                context.setBlock(x, bridgeY + 1, 5, Material.IRON_BARS);
                context.setBlock(x, bridgeY + 1, 10, Material.IRON_BARS);
                
                if (x % 2 == 0) {
                    context.setBlock(x, bridgeY + 2, 5, Material.IRON_BARS);
                    context.setBlock(x, bridgeY + 2, 10, Material.IRON_BARS);
                }
            }
            
            context.setBlock(0, bridgeY + 1, 5, Material.REDSTONE_BLOCK);
            context.setBlock(0, bridgeY + 1, 10, Material.REDSTONE_BLOCK);
        } else {
            for (int z = 0; z < 16; z++) {
                context.setBlock(5, bridgeY + 1, z, Material.IRON_BARS);
                context.setBlock(10, bridgeY + 1, z, Material.IRON_BARS);
                
                if (z % 2 == 0) {
                    context.setBlock(5, bridgeY + 2, z, Material.IRON_BARS);
                    context.setBlock(10, bridgeY + 2, z, Material.IRON_BARS);
                }
            }
            
            context.setBlock(5, bridgeY + 1, 0, Material.REDSTONE_BLOCK);
            context.setBlock(10, bridgeY + 1, 0, Material.REDSTONE_BLOCK);
        }
    }
}
