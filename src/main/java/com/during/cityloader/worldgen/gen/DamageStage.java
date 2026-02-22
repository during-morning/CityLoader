package com.during.cityloader.worldgen.gen;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.NoiseGeneratorPerlin;
import com.during.cityloader.worldgen.ChunkHeightmap;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.DamageArea;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * 破坏/废墟阶段
 * 包含rubble层生成功能
 */
public final class DamageStage implements GenerationStage {

    private static final int FLOOR_HEIGHT = 6;
    private static final Map<Material, Material> DEFAULT_DAMAGE_MAP = createDefaultDamageMap();
    private static final long RUBBLE_DIRT_SALT = 0x7d2f6a5b4c3e2901L;
    private static final long RUBBLE_LEAF_SALT = 0x4bf8d2710a93cc5dL;
    private static final Map<String, NoiseGeneratorPerlin> RUBBLE_NOISE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, NoiseGeneratorPerlin> eldest) {
                    return size() > 16;
                }
            });

    @Override
    public void generate(GenerationContext context) {
        BuildingInfo info = context.getBuildingInfo();
        LostCityProfile profile = context.getDimensionInfo().getProfile();
        
        if (profile.isRubbleLayer() && info.isCity) {
            generateRubbleLayer(context, profile);
        }
        
        if (!profile.isDamageEnabled()) {
            return;
        }
        
        if (!info.hasBuilding) {
            return;
        }

        float mainChance = clampChance(profile.getExplosionChance());
        float miniChance = clampChance(profile.getMiniExplosionChance());

        // LC 语义：爆炸基准高度由来源区块 cityLevel 决定，Stage 仅传递 profile 原始高度区间
        int mainMinY = profile.getExplosionMinHeight();
        int mainMaxY = profile.getExplosionMaxHeight();
        int miniMinY = profile.getMiniExplosionMinHeight();
        int miniMaxY = profile.getMiniExplosionMaxHeight();

        ChunkCoord coord = new ChunkCoord(
                context.getWorldInfo().getName(),
                context.getChunkX(),
                context.getChunkZ());

        DamageArea damageArea = DamageArea.getOrCreate(
                context.getDimensionInfo(),
                coord,
                mainMinY,
                mainMaxY,
                miniMinY,
                miniMaxY,
                mainChance,
                miniChance,
                profile.getExplosionMinRadius(),
                profile.getExplosionMaxRadius(),
                profile.getMiniExplosionMinRadius(),
                profile.getMiniExplosionMaxRadius());
        if (damageArea.isEmpty()) {
            return;
        }

        Map<Material, Material> damageMap = buildDamageMap(context.palette(), context);
        for (DamageArea.Blast blast : damageArea.getBlasts()) {
            applyExplosion(context, damageMap, blast);
        }
    }

    private void generateRubbleLayer(GenerationContext context, LostCityProfile profile) {
        BuildingInfo info = context.getBuildingInfo();
        
        if (!info.hasBuilding) {
            return;
        }
        
        IDimensionInfo dimInfo = context.getDimensionInfo();
        ChunkHeightmap heightmap = dimInfo.getHeightmap(context.getChunkX(), context.getChunkZ());
        int cityGroundY = info.getCityGroundLevel();
        float dirtScale = profile.getRubbleDirtScale();
        float leaveScale = profile.getRubbleLeaveScale();
        
        Material dirtMaterial = Material.DIRT;
        Material leaveMaterial = Material.MOSSY_COBBLESTONE;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int terrainHeight = getTerrainHeight(heightmap, x, z, cityGroundY);
                
                if (terrainHeight < cityGroundY) {
                    continue;
                }
                
                int rubbleY = cityGroundY - 1;
                if (rubbleY < 0 || rubbleY > 255) {
                    continue;
                }
                
                double noise = getNoiseValue(context, x, z, dirtScale, RUBBLE_DIRT_SALT);
                boolean useLeave = Math.abs(noise) > 0.5;
                Material rubbleMaterial = useLeave ? leaveMaterial : dirtMaterial;
                
                context.setBlock(x, rubbleY, z, rubbleMaterial);
                
                if (useLeave && leaveScale > 0) {
                    int leavesY = rubbleY + 1;
                    if (leavesY < 256) {
                        double leafNoise = getNoiseValue(context, x + 100, z + 100, leaveScale, RUBBLE_LEAF_SALT);
                        if (Math.abs(leafNoise) > 0.3) {
                            context.setBlock(x, leavesY, z, Material.VINE);
                        }
                    }
                }
            }
        }
    }

    private int getTerrainHeight(ChunkHeightmap heightmap, int x, int z, int fallback) {
        if (heightmap == null) {
            return fallback;
        }
        if (x < 0 || x >= 16 || z < 0 || z >= 16) {
            return fallback;
        }
        return heightmap.getHeight(x, z);
    }

    private double getNoiseValue(GenerationContext context, int x, int z, double scale, long salt) {
        IDimensionInfo provider = context.getDimensionInfo();
        if (provider == null) {
            return 0.0;
        }
        NoiseGeneratorPerlin perlin = getRubbleNoise(provider, salt);
        double adjustedScale = Math.max(1.0, scale);
        double worldX = ((context.getChunkX() << 4) + x) / adjustedScale;
        double worldZ = ((context.getChunkZ() << 4) + z) / adjustedScale;
        return perlin.getValue(worldX, worldZ);
    }

    private NoiseGeneratorPerlin getRubbleNoise(IDimensionInfo provider, long salt) {
        long noiseSeed = provider.getSeed() ^ salt;
        String dimension = provider.dimension() == null ? "<unknown>" : provider.dimension();
        String key = dimension + ":" + noiseSeed;
        return RUBBLE_NOISE_CACHE.computeIfAbsent(key, ignored -> new NoiseGeneratorPerlin(noiseSeed, 4));
    }

    private void applyExplosion(GenerationContext context,
                                Map<Material, Material> damageMap,
                                DamageArea.Blast blast) {
        int centerX = blast.x();
        int centerY = blast.y();
        int centerZ = blast.z();
        int radius = blast.radius();
        boolean destructive = blast.destructive();
        Random random = new Random(blast.randomSeed());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > radius) {
                        continue;
                    }

                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;
                    if (x < 0 || x >= 16 || z < 0 || z >= 16) {
                        continue;
                    }

                    float strength = 1.0f - (distance / Math.max(1.0f, radius));
                    float destroyChance = destructive ? strength * 0.65f : strength * 0.35f;
                    float damageChance = strength * 0.55f;

                    float roll = random.nextFloat();
                    if (roll < destroyChance) {
                        replaceWithDestroyed(context, x, y, z);
                        continue;
                    }
                    if (roll < destroyChance + damageChance) {
                        replaceWithDamaged(context, damageMap, x, y, z);
                    }
                }
            }
        }
    }

    private void replaceWithDestroyed(GenerationContext context, int localX, int y, int localZ) {
        Material current = getType(context, localX, y, localZ);
        if (current == null || current == Material.BEDROCK || current == Material.BARRIER) {
            return;
        }
        if (isProtectedForDamage(context, current, y)) {
            return;
        }
        Material replacement = y <= context.getBuildingInfo().waterLevel ? Material.WATER : Material.AIR;
        context.setBlock(localX, y, localZ, replacement);
    }

    private void replaceWithDamaged(GenerationContext context,
                                    Map<Material, Material> damageMap,
                                    int localX,
                                    int y,
                                    int localZ) {
        Material current = getType(context, localX, y, localZ);
        if (current == null || current == Material.AIR || current == Material.WATER || current == Material.LAVA) {
            return;
        }
        if (isProtectedForDamage(context, current, y)) {
            return;
        }
        Material damaged = damageMap.get(current);
        if (damaged == null) {
            damaged = DEFAULT_DAMAGE_MAP.get(current);
        }
        if (damaged == null || damaged == current) {
            return;
        }

        if (damaged == Material.AIR && y <= context.getBuildingInfo().waterLevel) {
            damaged = Material.WATER;
        }
        context.setBlock(localX, y, localZ, damaged);
    }

    private boolean isProtectedForDamage(GenerationContext context, Material current, int y) {
        if (current == null) {
            return true;
        }

        String name = current.name();
        if (name.contains("RAIL")) {
            return true;
        }
        if (current == Material.REDSTONE_BLOCK || current == Material.IRON_BARS) {
            return true;
        }

        // 尽量保护贴近道路/轨道层的关键结构，避免“地表支离破碎”
        int protectedCeiling = context.getBuildingInfo().getCityGroundLevel() + 2;
        if (y <= protectedCeiling) {
            return current == Material.SMOOTH_STONE
                    || current == Material.POLISHED_ANDESITE
                    || current == Material.STONE_BRICKS
                    || current == Material.COBBLED_DEEPSLATE
                    || current == Material.STONE_BRICK_SLAB
                    || current == Material.SMOOTH_STONE_SLAB
                    || current == Material.STONE_SLAB;
        }
        return false;
    }

    private Material getType(GenerationContext context, int localX, int y, int localZ) {
        return context.getBlockType(localX, y, localZ);
    }

    private float clampChance(float value) {
        if (value <= 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, value);
    }

    private Map<Material, Material> buildDamageMap(CompiledPalette palette, GenerationContext context) {
        Map<Material, Material> resolved = new EnumMap<>(Material.class);
        if (palette == null) {
            return resolved;
        }

        for (char token : palette.getCharacters()) {
            String damagedDefinition = palette.getDamaged(token);
            if (damagedDefinition == null || damagedDefinition.isBlank()) {
                continue;
            }
            Material damaged = context.resolveMaterial(damagedDefinition, null);
            if (damaged == null || damaged == Material.STRUCTURE_VOID) {
                continue;
            }
            for (String sourceDefinition : palette.getAll(token)) {
                Material source = context.resolveMaterial(sourceDefinition, null);
                if (source == null || source == Material.AIR || source == Material.STRUCTURE_VOID) {
                    continue;
                }
                resolved.putIfAbsent(source, damaged);
            }
        }
        return resolved;
    }

    private static Map<Material, Material> createDefaultDamageMap() {
        Map<Material, Material> map = new EnumMap<>(Material.class);

        map.put(Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS);
        map.put(Material.CRACKED_STONE_BRICKS, Material.MOSSY_STONE_BRICKS);
        map.put(Material.MOSSY_STONE_BRICKS, Material.IRON_BARS);

        map.put(Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        map.put(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE);

        map.put(Material.NETHER_BRICKS, Material.CRACKED_NETHER_BRICKS);
        map.put(Material.CRACKED_NETHER_BRICKS, Material.NETHER_BRICK_FENCE);

        map.put(Material.DEEPSLATE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS);
        map.put(Material.CRACKED_DEEPSLATE_BRICKS, Material.COBBLED_DEEPSLATE);
        map.put(Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_TILES);
        map.put(Material.CRACKED_DEEPSLATE_TILES, Material.COBBLED_DEEPSLATE);

        map.put(Material.GLASS, Material.AIR);
        map.put(Material.GLASS_PANE, Material.AIR);
        map.put(Material.TINTED_GLASS, Material.AIR);

        return map;
    }
}
