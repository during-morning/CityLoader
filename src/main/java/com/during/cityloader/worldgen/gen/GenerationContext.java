package com.during.cityloader.worldgen.gen;

import com.during.cityloader.season.Season;
import com.during.cityloader.worldgen.ChunkDriver;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.CompiledPalette;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Lockable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 区块生成上下文
 */
public class GenerationContext {

    private final WorldInfo worldInfo;
    private final IDimensionInfo dimensionInfo;
    private final BuildingInfo buildingInfo;
    private final Random random;
    private final Season season;
    private final int chunkX;
    private final int chunkZ;
    private final int baseX;
    private final int baseZ;
    
    private final ChunkDriver driver;
    private final LimitedRegion region;

    // 缓存 BlockData 解析结果，避免重复调用 Bukkit API
    private final Map<String, BlockData> blockDataCache = new HashMap<>();
    private final List<BlockStateTask> pendingBlockStateTasks = new ArrayList<>();

    // 将常见模组方块名降级到可用的原版材质，避免解析失败导致建筑细节丢失
    private static final Map<String, String> NON_VANILLA_BLOCK_ALIASES = Map.ofEntries(
            Map.entry("exposed_iron_bars", "iron_bars"),
            Map.entry("weathered_iron_bars", "iron_bars"),
            Map.entry("rusted_iron_bars", "iron_bars"),
            Map.entry("waxed_iron_door", "iron_door"),
            Map.entry("cut_iron", "iron_block"),
            Map.entry("charred_planks", "oak_planks"),
            Map.entry("charred_slab", "oak_slab"),
            Map.entry("charred_stairs", "oak_stairs"),
            Map.entry("mossy_stone", "mossy_cobblestone"),
            Map.entry("mossy_stone_slab", "mossy_cobblestone_slab"),
            Map.entry("mossy_stone_stairs", "mossy_cobblestone_stairs"),
            Map.entry("mossy_stone_wall", "mossy_cobblestone_wall"),
            Map.entry("mossy_bricks", "mossy_stone_bricks"),
            Map.entry("mossy_brick_slab", "mossy_stone_brick_slab"),
            Map.entry("cracked_bricks", "cracked_stone_bricks"),
            Map.entry("cracked_stone_brick_wall", "stone_brick_wall"),
            Map.entry("cracked_deepslate_brick_slab", "deepslate_brick_slab"),
            Map.entry("pomkotscube", "iron_block"),
            Map.entry("mechworkbench", "crafting_table")
    );

    public GenerationContext(WorldInfo worldInfo,
                             LimitedRegion region,
                             IDimensionInfo dimensionInfo,
                             BuildingInfo buildingInfo,
                             Random random,
                             int chunkX,
                             int chunkZ) {
        this(worldInfo, region, dimensionInfo, buildingInfo, random, chunkX, chunkZ, Season.SPRING);
    }

    public GenerationContext(WorldInfo worldInfo,
                             LimitedRegion region,
                             IDimensionInfo dimensionInfo,
                             BuildingInfo buildingInfo,
                             Random random,
                             int chunkX,
                             int chunkZ,
                             Season season) {
        this.worldInfo = worldInfo;
        this.dimensionInfo = dimensionInfo;
        this.buildingInfo = buildingInfo;
        this.random = random;
        this.season = season == null ? Season.SPRING : season;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.baseX = chunkX << 4;
        this.baseZ = chunkZ << 4;
        this.region = region;
        
        this.driver = new ChunkDriver();
        if (dimensionInfo != null) {
            this.driver.setPrimer(dimensionInfo.getWorld(), region, chunkX, chunkZ);
        }
    }
    
    /**
     * 将缓冲区内容写入区块
     */
    public void flush() {
        driver.actuallyGenerate();
        applyPendingBlockStateTasks();
    }

    public WorldInfo getWorldInfo() {
        return worldInfo;
    }

    public IDimensionInfo getDimensionInfo() {
        return dimensionInfo;
    }

    public BuildingInfo getBuildingInfo() {
        return buildingInfo;
    }

    public Random getRandom() {
        return random;
    }

    public Season getSeason() {
        return season;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getBaseX() {
        return baseX;
    }

    public int getBaseZ() {
        return baseZ;
    }

    public int worldX(int localX) {
        return baseX + localX;
    }

    public int worldZ(int localZ) {
        return baseZ + localZ;
    }

    public boolean isCity() {
        return buildingInfo.isCity;
    }

    public boolean hasBuilding() {
        return buildingInfo.hasBuilding;
    }

    public CompiledPalette palette() {
        return buildingInfo.getCompiledPalette();
    }

    /**
     * 在指定位置放置方块（Material 版本，向后兼容）
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param material 方块材料
     */
    public void setBlock(int localX, int y, int localZ, Material material) {
        if (material == null) {
            return;
        }
        setBlockData(localX, y, localZ, material.createBlockData());
    }

    /**
     * 在指定位置放置方块（BlockData 版本，支持完整方块状态）
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param blockData 方块数据（包含材料和状态）
     */
    public void setBlockData(int localX, int y, int localZ, BlockData blockData) {
        if (blockData == null) {
            return;
        }
        driver.current(localX, y, localZ).block(blockData);
    }

    /**
     * 放置带状态的轨道方块。
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param material 轨道材质（RAIL/POWERED_RAIL/DETECTOR_RAIL/ACTIVATOR_RAIL）
     * @param shape 轨道形状
     * @param powered 供电状态（对可供电轨道生效）
     */
    public void setRail(int localX, int y, int localZ, Material material, Rail.Shape shape, boolean powered) {
        Material railMaterial = normalizeRailMaterial(material);
        if (shape == null) {
            setBlock(localX, y, localZ, railMaterial);
            return;
        }

        String shapeKey = railShapeKey(shape);
        StringBuilder definition = new StringBuilder()
                .append("minecraft:")
                .append(railMaterial.name().toLowerCase(Locale.ROOT))
                .append("[shape=")
                .append(shapeKey);
        if (isPowerableRail(railMaterial)) {
            definition.append(",powered=").append(powered);
        }
        definition.append("]");

        BlockData data = parseBlockData(definition.toString());
        if (data != null) {
            setBlockData(localX, y, localZ, data);
            return;
        }

        BlockData fallback = railMaterial.createBlockData();
        if (fallback instanceof Rail rail) {
            rail.setShape(shape);
        }
        if (fallback instanceof Powerable powerable) {
            powerable.setPowered(powered);
        }
        setBlockData(localX, y, localZ, fallback);
    }

    /**
     * 在指定方向附着藤蔓。
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param supportFace 支撑方块所在方向（相对藤蔓方块）
     * @return 是否成功设置附着面的藤蔓
     */
    public boolean setVine(int localX, int y, int localZ, BlockFace supportFace) {
        String face = horizontalFaceKey(supportFace);
        if (face == null) {
            return false;
        }
        BlockData data = parseBlockData("minecraft:vine[" + face + "=true]");
        if (data == null) {
            return false;
        }
        setBlockData(localX, y, localZ, data);
        return true;
    }

    /**
     * 在指定方向墙面放置火把。
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param supportFace 支撑方块所在方向（相对火把方块）
     * @return 是否成功放置壁挂火把
     */
    public boolean setWallTorch(int localX, int y, int localZ, BlockFace supportFace) {
        if (supportFace == null) {
            return false;
        }
        String facing = horizontalFaceKey(supportFace.getOppositeFace());
        if (facing == null) {
            return false;
        }
        BlockData data = parseBlockData("minecraft:wall_torch[facing=" + facing + "]");
        if (data == null) {
            return false;
        }
        setBlockData(localX, y, localZ, data);
        return true;
    }

    /**
     * 在当前位置放置可附着火把：优先地面火把，其次壁挂火把。
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @return 是否成功放置
     */
    public boolean setTorchWithSupport(int localX, int y, int localZ) {
        if (worldInfo != null && (y < worldInfo.getMinHeight() || y >= worldInfo.getMaxHeight())) {
            return false;
        }

        Material existing = getBlockType(localX, y, localZ);
        if (!isAirLike(existing)) {
            return false;
        }

        Material support = getBlockType(localX, y - 1, localZ);
        if (isSolidSupport(support)) {
            setBlock(localX, y, localZ, Material.TORCH);
            return true;
        }

        BlockFace[] wallFaces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : wallFaces) {
            int supportX = localX + face.getModX();
            int supportZ = localZ + face.getModZ();
            if (supportX < 0 || supportX >= 16 || supportZ < 0 || supportZ >= 16) {
                continue;
            }
            Material wallSupport = getBlockType(supportX, y, supportZ);
            if (isSolidSupport(wallSupport) && setWallTorch(localX, y, localZ, face)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定位置的方块数据
     * 优先从缓存读取，其次从Region读取
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @return 方块数据
     */
    public BlockData getBlockData(int localX, int y, int localZ) {
        return driver.getBlock(localX, y, localZ);
    }

    /**
     * 获取指定位置的方块类型
     * 优先从缓存读取，其次从Region读取
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @return 方块材料
     */
    public Material getBlockType(int localX, int y, int localZ) {
        BlockData data = getBlockData(localX, y, localZ);
        return data == null ? Material.AIR : data.getMaterial();
    }

    /**
     * 在 flush 后应用战利品表到对应方块实体（若目标方块支持 Lootable）。
     */
    public void queueLootTable(int localX, int y, int localZ, String lootTableId) {
        if (lootTableId == null || lootTableId.isBlank()) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(lootTableId.trim());
        if (key == null) {
            return;
        }
        LootTable lootTable = Bukkit.getLootTable(key);
        if (lootTable == null) {
            return;
        }
        long seed = random == null ? 0L : random.nextLong();
        queueBlockStateTask(localX, y, localZ, state -> {
            if (!(state instanceof Lootable lootable)) {
                return false;
            }
            lootable.setLootTable(lootTable, seed);
            return true;
        });
    }

    /**
     * 在 flush 后设置刷怪笼生物（若目标方块是 CreatureSpawner）。
     */
    public void queueSpawnerMob(int localX, int y, int localZ, String mobId) {
        EntityType type = parseEntityType(mobId);
        if (type == null) {
            return;
        }
        queueBlockStateTask(localX, y, localZ, state -> {
            if (!(state instanceof CreatureSpawner spawner)) {
                return false;
            }
            spawner.setSpawnedType(type);
            return true;
        });
    }

    /**
     * 在 flush 后应用 palette tag 的常用字段（LootTable/Items/CustomName/Lock/Spawner）。
     */
    public void queueBlockEntityTag(int localX, int y, int localZ, Map<String, Object> tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        Map<String, Object> snapshot = new HashMap<>(tag);
        queueBlockStateTask(localX, y, localZ, state -> applyBlockEntityTag(state, snapshot));
    }

    /**
     * 在指定位置放置方块（字符串定义版本，自动解析为 BlockData）
     * 支持完整的方块状态定义，如 "minecraft:oak_stairs[facing=north,half=bottom]"
     *
     * @param localX 区块内X坐标 (0-15)
     * @param y      Y坐标
     * @param localZ 区块内Z坐标 (0-15)
     * @param definition 方块定义字符串
     */
    public void setBlock(int localX, int y, int localZ, String definition) {
        if (definition == null || definition.isBlank()) {
            return;
        }
        BlockData blockData = parseBlockData(definition);
        if (blockData != null) {
            setBlockData(localX, y, localZ, blockData);
        }
    }

    /**
     * 解析方块数据字符串为 BlockData
     *
     * @param definition 方块定义字符串（支持 Minecraft 格式，如 "stone_bricks" 或 "oak_stairs[facing=north]"）
     * @return BlockData 对象，解析失败返回 null
     */
    public BlockData parseBlockData(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }

        String cacheKey = definition.trim();

        // 缓存查找
        if (blockDataCache.containsKey(cacheKey)) {
            return blockDataCache.get(cacheKey);
        }

        String normalized = cacheKey.toLowerCase(Locale.ROOT);

        // 优先按完整定义解析（保留状态）
        BlockData result = tryCreateBlockData(normalized);
        if (result == null && !normalized.contains(":")) {
            result = tryCreateBlockData("minecraft:" + normalized);
        }
        if (result == null && !cacheKey.equals(normalized)) {
            result = tryCreateBlockData(cacheKey);
        }

        // 回退：仅按材料解析（丢失状态）
        if (result == null) {
            result = tryCreateMaterialBlockData(normalized);
        }

        blockDataCache.put(cacheKey, result);
        return result;
    }

    private BlockData tryCreateBlockData(String definition) {
        try {
            return Bukkit.createBlockData(definition);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BlockData tryCreateMaterialBlockData(String normalizedDefinition) {
        String materialId = normalizedDefinition;
        int stateIndex = materialId.indexOf('[');
        if (stateIndex >= 0) {
            materialId = materialId.substring(0, stateIndex);
        }

        Material material = Material.matchMaterial(materialId);
        if (material == null) {
            int colon = materialId.indexOf(':');
            String simple = colon >= 0 ? materialId.substring(colon + 1) : materialId;
            material = Material.matchMaterial(simple);
        }
        if (material == null) {
            material = resolveAliasedMaterial(materialId);
        }
        if (material == null) {
            return null;
        }

        try {
            return material.createBlockData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Material resolveAliasedMaterial(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return null;
        }

        int colon = materialId.indexOf(':');
        if (colon < 0 || colon >= materialId.length() - 1) {
            return null;
        }
        String path = materialId.substring(colon + 1).toLowerCase(Locale.ROOT);

        String directAlias = NON_VANILLA_BLOCK_ALIASES.get(path);
        Material material = matchMaterial(directAlias);
        if (material != null) {
            return material;
        }

        material = matchMaterial(bestTokenAlias(path));
        if (material != null) {
            return material;
        }

        if (path.contains("iron_bars")) {
            return Material.IRON_BARS;
        }
        if (path.contains("iron_door")) {
            return Material.IRON_DOOR;
        }
        if (path.contains("stairs")) {
            return Material.OAK_STAIRS;
        }
        if (path.contains("slab")) {
            return Material.OAK_SLAB;
        }
        if (path.contains("planks")) {
            return Material.OAK_PLANKS;
        }
        if (path.contains("wall")) {
            return Material.STONE_BRICK_WALL;
        }
        if (path.contains("brick")) {
            return Material.STONE_BRICKS;
        }
        if (path.contains("cube")) {
            return Material.IRON_BLOCK;
        }
        return null;
    }

    private String bestTokenAlias(String path) {
        String[] tokens = path.split("_");
        if (tokens.length <= 1) {
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(path);
        for (int start = 0; start < tokens.length; start++) {
            for (int end = tokens.length; end > start; end--) {
                if (end - start == tokens.length) {
                    continue;
                }
                candidates.add(joinTokens(tokens, start, end));
            }
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (Material.matchMaterial(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private String joinTokens(String[] tokens, int startInclusive, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (i > startInclusive) {
                sb.append('_');
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    private Material matchMaterial(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        return Material.matchMaterial(candidate);
    }

    private Material normalizeRailMaterial(Material material) {
        if (material == null) {
            return Material.RAIL;
        }
        if (material == Material.RAIL
                || material == Material.POWERED_RAIL
                || material == Material.DETECTOR_RAIL
                || material == Material.ACTIVATOR_RAIL) {
            return material;
        }
        return Material.RAIL;
    }

    private boolean isPowerableRail(Material material) {
        return material == Material.POWERED_RAIL
                || material == Material.DETECTOR_RAIL
                || material == Material.ACTIVATOR_RAIL;
    }

    private String railShapeKey(Rail.Shape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> "north_south";
            case EAST_WEST -> "east_west";
            case ASCENDING_EAST -> "ascending_east";
            case ASCENDING_WEST -> "ascending_west";
            case ASCENDING_NORTH -> "ascending_north";
            case ASCENDING_SOUTH -> "ascending_south";
            case SOUTH_EAST -> "south_east";
            case SOUTH_WEST -> "south_west";
            case NORTH_WEST -> "north_west";
            case NORTH_EAST -> "north_east";
        };
    }

    private String horizontalFaceKey(BlockFace face) {
        if (face == null) {
            return null;
        }
        return switch (face) {
            case NORTH -> "north";
            case SOUTH -> "south";
            case EAST -> "east";
            case WEST -> "west";
            default -> null;
        };
    }

    private boolean isAirLike(Material material) {
        return material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private boolean isSolidSupport(Material material) {
        if (material == null || material == Material.BARRIER || isAirLike(material)) {
            return false;
        }
        try {
            return material.isSolid();
        } catch (Throwable ignored) {
            // Unit-test环境可能未初始化完整Registry，回退为“非空气即可支撑”。
            return true;
        }
    }

    /**
     * 解析材料字符串为 Material（仅材料，无状态）
     *
     * @param definition 方块定义字符串
     * @param fallback 解析失败时的回退材料
     * @return Material 对象
     */
    public Material resolveMaterial(String definition, Material fallback) {
        if (definition == null || definition.isBlank()) {
            return fallback;
        }

        String value = definition;
        int stateIndex = value.indexOf('[');
        if (stateIndex >= 0) {
            value = value.substring(0, stateIndex);
        }

        String namespaced = value.toLowerCase(Locale.ROOT);
        Material material = Material.matchMaterial(namespaced);
        if (material != null) {
            return material;
        }

        int colon = namespaced.indexOf(':');
        String simple = colon >= 0 ? namespaced.substring(colon + 1) : namespaced;
        material = Material.matchMaterial(simple.toUpperCase(Locale.ROOT));
        if (material != null) {
            return material;
        }

        return fallback;
    }

    private boolean applyBlockEntityTag(BlockState state, Map<String, Object> tag) {
        boolean changed = false;

        Object lootTableId = tag.get("LootTable");
        if (lootTableId instanceof String lootTableString && state instanceof Lootable lootable) {
            NamespacedKey key = NamespacedKey.fromString(lootTableString);
            if (key != null) {
                LootTable lootTable = Bukkit.getLootTable(key);
                if (lootTable != null) {
                    Object seedObject = tag.get("LootTableSeed");
                    long seed = numberAsLong(seedObject, random == null ? 0L : random.nextLong());
                    lootable.setLootTable(lootTable, seed);
                    changed = true;
                }
            }
        }

        if (state instanceof CreatureSpawner spawner) {
            EntityType fromTag = parseSpawnerEntity(tag);
            if (fromTag != null) {
                spawner.setSpawnedType(fromTag);
                changed = true;
            }
        }

        if (state instanceof Container container) {
            Object items = tag.get("Items");
            if (items instanceof List<?> itemList) {
                Inventory inventory = container.getSnapshotInventory();
                for (Object rawEntry : itemList) {
                    if (!(rawEntry instanceof Map<?, ?> entry)) {
                        continue;
                    }
                    int slot = numberAsInt(entry.get("Slot"), -1);
                    if (slot < 0 || slot >= inventory.getSize()) {
                        continue;
                    }
                    Material material = parseItemMaterial(entry.get("id"));
                    if (material == null || material == Material.AIR) {
                        continue;
                    }
                    int amount = Math.max(1, numberAsInt(entry.get("Count"), 1));
                    amount = Math.min(amount, material.getMaxStackSize());
                    try {
                        inventory.setItem(slot, new ItemStack(material, amount));
                        changed = true;
                    } catch (Throwable ignored) {
                        // 某些测试环境未初始化物品注册表，忽略单个物品写入失败。
                    }
                }
            }
        }

        Object customName = tag.get("CustomName");
        if (customName instanceof String name && state instanceof Nameable nameable) {
            nameable.setCustomName(name);
            changed = true;
        }

        Object lock = tag.get("Lock");
        if (lock instanceof String lockValue && state instanceof Lockable lockable) {
            lockable.setLock(lockValue);
            changed = true;
        }

        return changed;
    }

    private EntityType parseSpawnerEntity(Map<String, Object> tag) {
        EntityType direct = parseEntityType(tag.get("EntityId"));
        if (direct != null) {
            return direct;
        }

        Object spawnData = tag.get("SpawnData");
        if (!(spawnData instanceof Map<?, ?> spawnDataMap)) {
            return null;
        }

        EntityType nestedDirect = parseEntityType(spawnDataMap.get("id"));
        if (nestedDirect != null) {
            return nestedDirect;
        }

        Object entity = spawnDataMap.get("entity");
        if (entity instanceof Map<?, ?> entityMap) {
            return parseEntityType(entityMap.get("id"));
        }
        return null;
    }

    private EntityType parseEntityType(Object raw) {
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        String simple = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        EntityType type = EntityType.fromName(simple);
        if (type == null) {
            try {
                type = EntityType.valueOf(simple.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (type == EntityType.UNKNOWN) {
            return null;
        }
        return type;
    }

    private Material parseItemMaterial(Object raw) {
        if (!(raw instanceof String itemId) || itemId.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(itemId);
        if (material != null) {
            return material;
        }
        int colon = itemId.indexOf(':');
        if (colon >= 0 && colon < itemId.length() - 1) {
            return Material.matchMaterial(itemId.substring(colon + 1));
        }
        return null;
    }

    private int numberAsInt(Object raw, int fallback) {
        if (!(raw instanceof Number number)) {
            return fallback;
        }
        return number.intValue();
    }

    private long numberAsLong(Object raw, long fallback) {
        if (!(raw instanceof Number number)) {
            return fallback;
        }
        return number.longValue();
    }

    private void queueBlockStateTask(int localX, int y, int localZ, BlockStateMutator mutator) {
        if (mutator == null) {
            return;
        }
        int worldX = worldX(localX);
        int worldZ = worldZ(localZ);
        synchronized (pendingBlockStateTasks) {
            pendingBlockStateTasks.add(new BlockStateTask(worldX, y, worldZ, mutator));
        }
    }

    private void applyPendingBlockStateTasks() {
        World world = dimensionInfo == null ? null : dimensionInfo.getWorld();
        if (region == null) {
            synchronized (pendingBlockStateTasks) {
                if (world != null) {
                    for (BlockStateTask task : pendingBlockStateTasks) {
                        GlobalCompletionQueue.enqueue(world, task);
                    }
                }
                pendingBlockStateTasks.clear();
            }
            return;
        }

        List<BlockStateTask> tasks;
        synchronized (pendingBlockStateTasks) {
            if (pendingBlockStateTasks.isEmpty()) {
                return;
            }
            tasks = List.copyOf(pendingBlockStateTasks);
            pendingBlockStateTasks.clear();
        }

        for (BlockStateTask task : tasks) {
            try {
                if (!region.isInRegion(task.x(), task.y(), task.z())) {
                    if (world != null) {
                        GlobalCompletionQueue.enqueue(world, task);
                    }
                    continue;
                }
                BlockState state = region.getBlockState(task.x(), task.y(), task.z());
                if (state == null) {
                    continue;
                }
                if (task.mutator().mutate(state)) {
                    region.setBlockState(task.x(), task.y(), task.z(), state);
                }
            } catch (Exception ignored) {
                // 保持区块生成健壮性：单个 blockstate 回写失败不影响其余生成流程。
                if (world != null) {
                    GlobalCompletionQueue.enqueue(world, task);
                }
            }
        }
    }

    @FunctionalInterface
    public interface BlockStateMutator {
        boolean mutate(BlockState state);
    }

    public record BlockStateTask(int x, int y, int z, BlockStateMutator mutator) {
    }
}
