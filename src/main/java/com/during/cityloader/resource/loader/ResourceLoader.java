package com.during.cityloader.resource.loader;

import com.during.cityloader.exception.ResourceLoadException;
import com.during.cityloader.resource.*;
import com.during.cityloader.resource.registry.*;
import com.during.cityloader.season.Season;
import com.during.cityloader.util.ResourceLocation;
import com.google.gson.*;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 资源加载器
 * 负责从JSON文件加载调色板、部件和建筑定义
 * 
 * @author During
 * @since 1.4.0
 */
public class ResourceLoader {

    private final Logger logger;
    private final Gson gson;

    /**
     * 构造函数
     * 
     * @param logger 日志记录器
     */
    public ResourceLoader(Logger logger) {
        this.logger = logger;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * 加载变体
     * 
     * @param directory 变体目录
     * @param registry  变体注册表
     * @return 成功加载的数量
     */
    /**
     * 加载变体
     * 
     * @param directory 变体目录
     * @param registry  变体注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadVariants(File directory, VariantRegistry registry, String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("变体目录不存在: " + directory.getPath());
            return 0;
        }

        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.fine("未找到变体文件: " + directory.getPath());
            return 0;
        }

        for (File file : files) {
            try {
                Variant variant = loadVariant(file);
                if (variant != null && variant.validate()) {
                    // 使用命名空间注册
                    ResourceLocation location = new ResourceLocation(namespace, variant.getId());
                    try {
                        registry.register(location, variant);
                        loaded++;
                        logger.fine("加载变体: " + location);
                    } catch (IllegalArgumentException e) {
                        logger.warning("变体ID重复: " + location);
                    }
                }
            } catch (Exception e) {
                logger.warning("加载变体失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个变体");
        return loaded;
    }

    /**
     * 从文件加载单个变体
     * 
     * @param file JSON文件
     * @return 变体对象
     * @throws ResourceLoadException 加载失败
     */
    private Variant loadVariant(File file) throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // 获取ID（从文件名或JSON）
            String id = file.getName().replace(".json", "");
            if (json.has("id")) {
                id = json.get("id").getAsString();
            }

            // 解析方块列表
            List<Variant.WeightedBlock> blocks = new ArrayList<>();
            if (json.has("blocks")) {
                JsonArray blocksArray = json.getAsJsonArray("blocks");
                for (JsonElement element : blocksArray) {
                    JsonObject blockObj = element.getAsJsonObject();

                    // 获取权重和方块
                    int weight = blockObj.has("random") ? blockObj.get("random").getAsInt() : 1;
                    String blockName = blockObj.get("block").getAsString();

                    Material material = Material.matchMaterial(blockName);
                    if (material != null) {
                        blocks.add(new Variant.WeightedBlock(material, weight));
                    } else {
                        logger.warning("未知的方块材质: " + blockName + " in variant " + id);
                    }
                }
            }

            return new Variant(id, blocks);

        } catch (IOException e) {
            throw new ResourceLoadException("读取变体文件失败: " + file.getName(), e);
        } catch (JsonSyntaxException e) {
            throw new ResourceLoadException("JSON格式错误: " + file.getName(), e);
        } catch (Exception e) {
            throw new ResourceLoadException("解析变体文件失败: " + file.getName(), e);
        }
    }

    /**
     * 加载条件
     * 
     * @param directory 条件目录
     * @param registry  条件注册表
     * @return 成功加载的数量
     */
    /**
     * 加载条件
     * 
     * @param directory 条件目录
     * @param registry  条件注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadConditions(File directory, com.during.cityloader.resource.condition.ConditionRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("条件目录不存在: " + directory.getPath());
            return 0;
        }

        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.fine("未找到条件文件: " + directory.getPath());
            return 0;
        }

        for (File file : files) {
            try {
                com.during.cityloader.resource.condition.Condition<String> condition = loadCondition(file);
                if (condition != null && condition.validate()) {
                    // 使用命名空间注册
                    ResourceLocation location = new ResourceLocation(namespace, condition.getId());
                    try {
                        registry.register(location, condition);
                        loaded++;
                        logger.fine("加载条件: " + location + " (" + condition.size() + "个条目)");
                    } catch (IllegalArgumentException e) {
                        logger.warning("条件ID重复: " + location);
                    }
                }
            } catch (Exception e) {
                logger.warning("加载条件失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个条件");
        return loaded;
    }

    /**
     * 从文件加载单个条件
     * 
     * @param file JSON文件
     * @return 条件对象
     * @throws ResourceLoadException 加载失败
     */
    private com.during.cityloader.resource.condition.Condition<String> loadCondition(File file)
            throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // 获取ID（从文件名）
            String id = file.getName().replace(".json", "");

            // 解析条目列表
            List<com.during.cityloader.resource.condition.ConditionEntry<String>> entries = new ArrayList<>();
            if (json.has("values")) {
                JsonArray values = json.getAsJsonArray("values");
                for (JsonElement element : values) {
                    JsonObject entryObj = element.getAsJsonObject();

                    // 必需字段
                    double factor = entryObj.get("factor").getAsDouble();
                    String value = entryObj.get("value").getAsString();

                    // 可选条件字段
                    Boolean top = entryObj.has("top") ? entryObj.get("top").getAsBoolean() : null;
                    Boolean ground = entryObj.has("ground") ? entryObj.get("ground").getAsBoolean() : null;
                    Boolean cellar = entryObj.has("cellar") ? entryObj.get("cellar").getAsBoolean() : null;
                    String range = entryObj.has("range") ? entryObj.get("range").getAsString() : null;
                    String inpart = entryObj.has("inpart") ? entryObj.get("inpart").getAsString() : null;
                    String inbiome = entryObj.has("inbiome") ? entryObj.get("inbiome").getAsString() : null;

                    com.during.cityloader.resource.condition.ConditionEntry<String> entry = new com.during.cityloader.resource.condition.ConditionEntry<>(
                            factor, value, top, ground, cellar, range, inpart, inbiome);
                    entries.add(entry);
                }
            }

            return new com.during.cityloader.resource.condition.Condition<>(id, entries);

        } catch (IOException e) {
            throw new ResourceLoadException("读取条件文件失败: " + file.getName(), e);
        } catch (JsonSyntaxException e) {
            throw new ResourceLoadException("JSON格式错误: " + file.getName(), e);
        } catch (Exception e) {
            throw new ResourceLoadException("解析条件文件失败: " + file.getName(), e);
        }
    }

    /**
     * 加载调色板
     * 
     * @param directory 调色板目录
     * @param registry  调色板注册表
     * @return 成功加载的数量
     */
    /**
     * 加载调色板
     * 
     * @param directory 调色板目录
     * @param registry  调色板注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadPalettes(File directory, PaletteRegistry registry, String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("调色板目录不存在: " + directory.getPath());
            return 0;
        }

        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.fine("未找到调色板文件: " + directory.getPath());
            return 0;
        }

        for (File file : files) {
            try {
                Palette palette = loadPalette(file);
                if (palette != null && palette.validate()) {
                    // 使用命名空间注册
                    ResourceLocation location = new ResourceLocation(namespace, palette.getId());

                    // 直接使用带ResourceLocation的注册方法，避免使用palette.getId()默认注册导致的重复
                    try {
                        registry.register(location, palette);
                        loaded++;
                        logger.fine("加载调色板: " + location);
                    } catch (IllegalArgumentException e) {
                        logger.warning("调色板ID重复: " + location);
                    }
                }
            } catch (Exception e) {
                logger.warning("加载调色板失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个调色板");
        return loaded;
    }

    /**
     * 从文件加载单个调色板
     * 
     * @param file JSON文件
     * @return 调色板对象
     * @throws ResourceLoadException 加载失败
     */
    private Palette loadPalette(File file) throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // 获取ID（从文件名或JSON）
            String id = file.getName().replace(".json", "");
            if (json.has("id")) {
                id = json.get("id").getAsString();
            }

            Palette palette = new Palette(id);

            // 解析方块映射
            if (json.has("blocks")) {
                JsonObject blocks = json.getAsJsonObject("blocks");
                for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                    char character = entry.getKey().charAt(0);
                    BlockMapping mapping = parseBlockMapping(entry.getValue());
                    if (mapping != null) {
                        palette.addBlockMapping(character, mapping);
                    }
                }
            }

            return palette;

        } catch (IOException e) {
            throw new ResourceLoadException("无法读取文件: " + file.getName(), e);
        } catch (JsonSyntaxException e) {
            throw new ResourceLoadException("JSON格式错误: " + file.getName(), e);
        }
    }

    /**
     * 解析方块映射
     * 
     * @param element JSON元素
     * @return 方块映射对象
     */
    private BlockMapping parseBlockMapping(JsonElement element) {
        try {
            if (element.isJsonPrimitive()) {
                // 简单格式: "character": "STONE"
                String blockName = element.getAsString();
                Material material = Material.matchMaterial(blockName);
                if (material != null) {
                    return new BlockMapping(material);
                }
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();

                // 获取默认方块
                Material defaultBlock = null;
                if (obj.has("default")) {
                    defaultBlock = Material.matchMaterial(obj.get("default").getAsString());
                }

                // 获取变体ID
                String variantId = null;
                if (obj.has("variant")) {
                    variantId = obj.get("variant").getAsString();
                }

                // 获取引用字符
                Character fromPaletteChar = null;
                if (obj.has("frompalette")) {
                    String s = obj.get("frompalette").getAsString();
                    if (s != null && !s.isEmpty()) {
                        fromPaletteChar = s.charAt(0);
                    }
                }

                // 验证：至少需要指定一种方式（默认方块、变体或引用）
                if (defaultBlock == null && variantId == null && fromPaletteChar == null) {
                    return null;
                }

                // 获取权重
                double weight = obj.has("weight") ? obj.get("weight").getAsDouble() : 1.0;

                // 获取季节变体
                Map<Season, Material> seasonalVariants = new HashMap<>();
                if (obj.has("seasonal")) {
                    JsonObject seasonal = obj.getAsJsonObject("seasonal");
                    for (Map.Entry<String, JsonElement> entry : seasonal.entrySet()) {
                        Season season = Season.fromString(entry.getKey());
                        Material material = Material.matchMaterial(entry.getValue().getAsString());
                        if (material != null) {
                            seasonalVariants.put(season, material);
                        }
                    }
                }

                // 解析Info元数据
                Info info = null;
                if (obj.has("mob") || obj.has("loot") || obj.has("torch") || obj.has("tag")) {
                    String mobId = obj.has("mob") ? obj.get("mob").getAsString() : null;
                    String loot = obj.has("loot") ? obj.get("loot").getAsString() : null;
                    boolean isTorch = obj.has("torch") && obj.get("torch").getAsBoolean();
                    String tag = obj.has("tag") ? obj.get("tag").toString() : null;

                    info = new Info(mobId, loot, isTorch, tag);
                }

                return new BlockMapping(defaultBlock, seasonalVariants, variantId, fromPaletteChar, weight, info);
            }
        } catch (Exception e) {
            logger.warning("解析方块映射失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 加载部件
     * 
     * @param directory 部件目录
     * @param registry  部件注册表
     * @return 成功加载的数量
     */
    /**
     * 加载部件
     * 
     * @param directory 部件目录
     * @param registry  部件注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadParts(File directory, PartRegistry registry, String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("部件目录不存在: " + directory.getPath());
            return 0;
        }

        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.fine("未找到部件文件: " + directory.getPath());
            return 0;
        }

        for (File file : files) {
            try {
                Part part = loadPart(file);
                if (part != null && part.validate()) {
                    // 使用命名空间注册
                    ResourceLocation location = new ResourceLocation(namespace, part.getId());
                    try {
                        registry.register(location, part);
                        loaded++;
                        logger.fine("加载部件: " + location);
                    } catch (IllegalArgumentException e) {
                        logger.warning("部件ID重复: " + location);
                    }
                }
            } catch (Exception e) {
                logger.warning("加载部件失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个部件");
        return loaded;
    }

    /**
     * 从文件加载单个部件
     * 
     * @param file JSON文件
     * @return 部件对象
     * @throws ResourceLoadException 加载失败
     */
    private Part loadPart(File file) throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // 获取ID
            String id = file.getName().replace(".json", "");
            if (json.has("id")) {
                id = json.get("id").getAsString();
            }

            // 获取调色板ID列表
            List<String> paletteIds = new ArrayList<>();
            if (json.has("palettes")) {
                JsonArray palettes = json.getAsJsonArray("palettes");
                for (JsonElement elem : palettes) {
                    paletteIds.add(elem.getAsString());
                }
            }

            // 获取尺寸
            int width = json.has("width") ? json.get("width").getAsInt() : 16;
            int height = json.has("height") ? json.get("height").getAsInt() : 1;
            int depth = json.has("depth") ? json.get("depth").getAsInt() : 16;

            // 解析结构数据
            char[][][] structure = new char[height][depth][width];
            if (json.has("structure")) {
                JsonArray layers = json.getAsJsonArray("structure");
                for (int y = 0; y < Math.min(height, layers.size()); y++) {
                    JsonArray layer = layers.get(y).getAsJsonArray();
                    for (int z = 0; z < Math.min(depth, layer.size()); z++) {
                        String row = layer.get(z).getAsString();
                        for (int x = 0; x < Math.min(width, row.length()); x++) {
                            structure[y][z][x] = row.charAt(x);
                        }
                    }
                }
            }

            return new Part(id, paletteIds, width, height, depth, structure);

        } catch (IOException e) {
            throw new ResourceLoadException("无法读取文件: " + file.getName(), e);
        } catch (JsonSyntaxException e) {
            throw new ResourceLoadException("JSON格式错误: " + file.getName(), e);
        }
    }

    /**
     * 加载建筑
     * 
     * @param directory 建筑目录
     * @param registry  建筑注册表
     * @return 成功加载的数量
     */
    /**
     * 加载建筑
     * 
     * @param directory 建筑目录
     * @param registry  建筑注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadBuildings(File directory, BuildingRegistry registry, String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warning("建筑目录不存在: " + directory.getPath());
            return 0;
        }

        int loaded = 0;
        java.util.Map<String, Integer> typeCount = new java.util.HashMap<>();
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.fine("未找到建筑文件: " + directory.getPath());
            return 0;
        }

        for (File file : files) {
            try {
                Building building = loadBuilding(file);
                if (building != null && building.validate()) {
                    // 使用命名空间注册
                    ResourceLocation location = new ResourceLocation(namespace, building.getId());
                    try {
                        registry.register(location, building);
                        loaded++;
                        
                        // 统计类型
                        String type = building.getType();
                        typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                        
                        logger.fine("加载建筑: " + location + " (类型: " + type + ")");
                    } catch (IllegalArgumentException e) {
                        logger.warning("建筑ID重复: " + location);
                    }
                }
            } catch (Exception e) {
                logger.warning("加载建筑失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个建筑");
        logger.info("[" + namespace + "] 建筑类型分布: " + typeCount);
        return loaded;
    }

    /**
     * 从文件加载单个建筑
     * 
     * @param file JSON文件
     * @return 建筑对象
     * @throws ResourceLoadException 加载失败
     */
    private Building loadBuilding(File file) throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // 获取ID
            String id = file.getName().replace(".json", "");
            if (json.has("id")) {
                id = json.get("id").getAsString();
            }

            // 获取类型 - 优先从JSON读取，否则从文件路径或文件名推断
            String type = "building"; // 默认类型
            if (json.has("type")) {
                type = json.get("type").getAsString();
            } else {
                // 从文件路径推断类型
                String filePath = file.getAbsolutePath().toLowerCase();
                String fileName = file.getName().toLowerCase();
                
                // 检查路径中是否包含类型关键词
                if (filePath.contains("/residential/") || fileName.contains("house") || 
                    fileName.contains("apartment") || fileName.contains("town")) {
                    type = "residential";
                } else if (filePath.contains("/commercial/") || fileName.contains("shop") || 
                           fileName.contains("store") || fileName.contains("mall") || 
                           fileName.contains("office")) {
                    type = "commercial";
                } else if (filePath.contains("/industrial/") || fileName.contains("factory") || 
                           fileName.contains("warehouse") || fileName.contains("plant")) {
                    type = "industrial";
                } else if (fileName.contains("library") || fileName.contains("school") || 
                           fileName.contains("hospital") || fileName.contains("station")) {
                    type = "public";
                } else if (fileName.contains("port") || fileName.contains("dock") || 
                           fileName.contains("harbor")) {
                    type = "port";
                }
                
                logger.fine("推断建筑类型: " + id + " -> " + type + " (基于文件名/路径)");
            }

            // 获取权重
            double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;

            // 获取楼层范围
            int minFloors = json.has("minFloors") ? json.get("minFloors").getAsInt() : 
                           json.has("minfloors") ? json.get("minfloors").getAsInt() : 1;
            int maxFloors = json.has("maxFloors") ? json.get("maxFloors").getAsInt() : 
                           json.has("maxfloors") ? json.get("maxfloors").getAsInt() : 5;

            // 获取调色板ID
            String palette = json.has("palette") ? json.get("palette").getAsString() : "standard";

            // 解析部件列表
            List<BuildingPart> parts = new ArrayList<>();
            if (json.has("parts")) {
                JsonArray partsArray = json.getAsJsonArray("parts");
                for (JsonElement elem : partsArray) {
                    JsonObject partObj = elem.getAsJsonObject();

                    String partId = null;
                    if (partObj.has("partId")) {
                        partId = partObj.get("partId").getAsString();
                    } else if (partObj.has("part")) {
                        partId = partObj.get("part").getAsString();
                    } else if (partObj.has("ref")) { // LostCities 也有时使用 ref
                        partId = partObj.get("ref").getAsString();
                    }

                    if (partId == null) {
                        logger.warning("建筑部件缺少ID定义: " + file.getName());
                        continue;
                    }

                    int offsetX = partObj.has("offsetX") ? partObj.get("offsetX").getAsInt() : 0;
                    int offsetY = partObj.has("offsetY") ? partObj.get("offsetY").getAsInt() : 0;
                    int offsetZ = partObj.has("offsetZ") ? partObj.get("offsetZ").getAsInt() : 0;

                    String condition = partObj.has("condition") ? partObj.get("condition").getAsString() : null;

                    // 兼容 top 属性
                    if (condition == null && partObj.has("top")) {
                        boolean isTop = partObj.get("top").getAsBoolean();
                        condition = isTop ? "top" : "!top";
                    }

                    parts.add(new BuildingPart(partId, offsetX, offsetY, offsetZ, condition));
                }
            }

            return new Building(id, type, parts, weight, minFloors, maxFloors, palette);

        } catch (

        IOException e) {
            throw new ResourceLoadException("无法读取文件: " + file.getName(), e);
        } catch (JsonSyntaxException e) {
            throw new ResourceLoadException("JSON格式错误: " + file.getName(), e);
        }
    }

    /**
     * 加载MultiBuildings
     *
     * @param directory MultiBuilding目录
     * @param registry  MultiBuilding注册表
     * @param namespace 命名空间
     * @return 成功加载的数量
     */
    public int loadMultiBuildings(File directory,
            com.during.cityloader.resource.registry.MultiBuildingRegistry registry, String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0; // 不是所有包都有MultiBuilding
        }

        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            return 0;
        }

        for (File file : files) {
            try {
                com.during.cityloader.resource.MultiBuilding mb = loadMultiBuilding(file);
                if (mb != null) {
                    ResourceLocation location = new ResourceLocation(namespace, mb.getId());
                    if (registry.contains(location)) {
                        logger.warning("MultiBuilding ID重复: " + location);
                        continue;
                    }
                    registry.register(location, mb);
                    loaded++;
                    logger.fine("加载MultiBuilding: " + location);
                }
            } catch (Exception e) {
                logger.warning("加载MultiBuilding失败 " + file.getName() + ": " + e.getMessage());
            }
        }

        logger.info("[" + namespace + "] 成功加载 " + loaded + " 个MultiBuildings");
        return loaded;
    }

    /**
     * 从文件加载单个MultiBuilding
     */
    private com.during.cityloader.resource.MultiBuilding loadMultiBuilding(File file) throws ResourceLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String id = file.getName().replace(".json", "");

            int dimX = json.has("dimx") ? json.get("dimx").getAsInt() : 1;
            int dimZ = json.has("dimz") ? json.get("dimz").getAsInt() : 1;

            List<List<String>> buildings = new ArrayList<>();
            if (json.has("buildings")) {
                JsonArray buildingsArray = json.getAsJsonArray("buildings");
                for (JsonElement rowElement : buildingsArray) {
                    List<String> row = new ArrayList<>();
                    JsonArray rowArray = rowElement.getAsJsonArray();
                    for (JsonElement cell : rowArray) {
                        row.add(cell.getAsString());
                    }
                    buildings.add(row);
                }
            }

            return new com.during.cityloader.resource.MultiBuilding(id, dimX, dimZ, buildings);

        } catch (Exception e) {
            throw new ResourceLoadException("解析MultiBuilding失败: " + file.getName(), e);
        }
    }

    /**
     * 加载Style
     *
     * @param directory 目录
     * @param registry  注册表
     * @param namespace 命名空间
     * @return 加载数量
     */
    public int loadStyles(File directory, com.during.cityloader.resource.registry.StyleRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            try {
                com.during.cityloader.resource.style.Style style = loadStyle(file);
                if (style != null) {
                    ResourceLocation location = new ResourceLocation(namespace, style.getId());
                    registry.register(location, style);
                    count++;
                }
            } catch (Exception e) {
                logger.warning("Failed to load Style from " + file.getName() + ": " + e.getMessage());
                if (Boolean.getBoolean("cityloader.debug")) {
                    e.printStackTrace();
                }
            }
        }
        return count;
    }

    private com.during.cityloader.resource.style.Style loadStyle(File file) {
        try (FileReader reader = new FileReader(file)) {
            com.during.cityloader.resource.style.Style style = gson.fromJson(reader,
                    com.during.cityloader.resource.style.Style.class);
            if (style != null) {
                String id = file.getName().replace(".json", "");
                style.setId(id);
            }
            return style;
        } catch (IOException | JsonSyntaxException e) {
            logger.warning("Error parsing Style JSON " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 加载CityStyle
     */
    public int loadCityStyles(File directory, com.during.cityloader.resource.registry.CityStyleRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            try {
                try (FileReader reader = new FileReader(file)) {
                    com.during.cityloader.resource.style.CityStyle style = gson.fromJson(reader,
                            com.during.cityloader.resource.style.CityStyle.class);
                    if (style != null) {
                        String id = file.getName().replace(".json", "");
                        style.setId(id);
                        ResourceLocation location = new ResourceLocation(namespace, id);
                        registry.register(location, style);
                        count++;
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to load CityStyle from " + file.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * 加载WorldStyle
     */
    public int loadWorldStyles(File directory, com.during.cityloader.resource.registry.WorldStyleRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            try {
                try (FileReader reader = new FileReader(file)) {
                    com.during.cityloader.resource.style.WorldStyle style = gson.fromJson(reader,
                            com.during.cityloader.resource.style.WorldStyle.class);
                    if (style != null) {
                        String id = file.getName().replace(".json", "");
                        style.setId(id);
                        ResourceLocation location = new ResourceLocation(namespace, id);
                        registry.register(location, style);
                        count++;
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to load WorldStyle from " + file.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * 加载Scattered
     */
    public int loadScattered(File directory, com.during.cityloader.resource.registry.ScatteredRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            try {
                try (FileReader reader = new FileReader(file)) {
                    com.during.cityloader.resource.scattered.Scattered scattered = gson.fromJson(reader,
                            com.during.cityloader.resource.scattered.Scattered.class);
                    if (scattered != null) {
                        String id = file.getName().replace(".json", "");
                        scattered.setId(id);
                        ResourceLocation location = new ResourceLocation(namespace, id);
                        registry.register(location, scattered);
                        count++;
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to load Scattered from " + file.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * 加载Stuff
     */
    public int loadStuff(File directory, com.during.cityloader.resource.registry.StuffRegistry registry,
            String namespace) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            try {
                try (FileReader reader = new FileReader(file)) {
                    com.during.cityloader.resource.stuff.Stuff stuff = gson.fromJson(reader,
                            com.during.cityloader.resource.stuff.Stuff.class);
                    if (stuff != null) {
                        String id = file.getName().replace(".json", "");
                        stuff.setId(id);
                        ResourceLocation location = new ResourceLocation(namespace, id);
                        registry.register(location, stuff);
                        count++;
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to load Stuff from " + file.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }
}
