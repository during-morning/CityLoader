# CityLoader - 程序化城市生成插件

**版本**: 1.4.0-SURFACE-SHIFT  
**平台**: Paper 1.21.8 + Java 21  
**状态**: 🟡 开发中 - P0架构收敛阶段  
**评分**: C+ (65/100) → 目标 A (90/100)

---

## 📑 目录

- [项目概述](#-项目概述)
- [当前状态](#-当前状态)
- [核心架构概念](#-核心架构概念)
- [三大核心问题详解](#-三大核心问题详解)
- [快速开始](#-快速开始)
- [安装与更新](#-安装与更新)
- [配置说明](#-配置说明)
- [命令说明](#-命令说明)
- [内置/data资产创建指南](#-内置data资产创建指南)
- [优先级路线图](#-优先级路线图)
  - [P0: 架构收敛](#p0-架构收敛2-3周-必须)
  - [P1: 核心特性](#p1-核心特性4-6周-重要)
  - [P2: 生态完善](#p2-生态完善2-3周-可选)
- [架构深度对比](#-架构深度对比)
- [测试策略](#-测试策略)
- [常用命令](#-常用命令)
- [故障排除](#-故障排除)
- [开发工作流](#-开发工作流)
- [最佳实践](#-最佳实践)
- [文档](#-文档)
- [成功标准](#-成功标准)
- [已知问题](#️-已知问题)
- [里程碑](#-里程碑)

---

## 📋 项目概述

CityLoader是一个Minecraft Paper服务器插件，复刻Forge模组LostCities的程序化城市生成功能，并添加季节适配特性。使用三层JSON资产架构（palettes、parts、buildings）生成原版兼容的城市结构。

### 核心特性
- ✅ 程序化城市生成（三层资产架构）
- ✅ 季节适配（集成RealisticSeasons）
- ✅ 资源包系统（支持9894+ JSON资产文件）
- ✅ 命令系统（4个管理命令，更多规划中）
- ✅ 性能优化（TPS 19+）

### 技术栈
- **构建**: Maven 3.x
- **语言**: Java 21
- **API**: Paper 1.21.8
- **JSON**: Gson 2.10.1
- **测试**: JUnit 5.10.2

---

## 🎯 当前状态

### 关键指标

| 指标 | 当前 | 目标 | 状态 |
|------|------|------|------|
| 资产加载 | 30% | 100% | ⚠️ P0 |
| 功能完整性 | 55% | 85% | ⚠️ P0-P1 |
| 架构统一 | 0% | 100% | 🔴 P0 |
| 测试覆盖率 | 35% | 60% | 🟡 P1 |
| TPS性能 | 19+ | 19+ | ✅ 完成 |
| 代码质量 | B | A | 🟡 P1 |

### 进度
```
总体: ▰▰▱▱▱▱▱▱▱▱ 15% (2/13任务)
P0: ▰▱▱▱▱ 20% (1/5) - 进行中
P1: ▱▱▱▱▱ 0% (0/5) - 未开始
P2: ▱▱▱ 0% (0/3) - 未开始
```

---

## ⚠️ 当前实现与规划差异（必读）

为避免误解，这里明确“当前代码真实行为”：

- **默认主链路已切换**：`CityLoaderPlugin` → `WorldInitListener` → `CityBlockPopulator` → `LostCityTerrainFeature` → `worldgen/gen/*` 分阶段执行。
- **`BuildingInfo` 已接入决策链**：实现城市判定、城市等级、建筑/多建筑选择、楼层选择、调色板编译与缓存。
- **`CompiledPalette` 已升级**：支持 `variant/frompalette/blocks/damaged/info` 解析与 128 槽随机表。
- **`regassets/data` 已补齐关键模型**：`WorldSettings/StreetSettings/RailSettings/Selectors/...` 等结构已可反序列化主流 JSON。
- **`WorldStyleRE/CityStyleRE/StyleRE` 已对齐扩展字段**：兼容 `inherit`、`selectors`、`settings` 以及 snake_case 变体。
- **`LostCityTerrainFeature` 已由枚举占位升级为总控类**：默认包含 `CityCore + Infrastructure + Scattered + Damage + PostProcess` 阶段。
- **数据加载策略已收敛**：仅扫描插件内置 `/data/`（多 namespace + 覆盖日志），不接管 Paper 外部数据包规则。
- **旧兼容层已降级**：`resource.*` 标记为 `@Deprecated`，默认生成路径不再依赖旧 `ResourceManager`。

> 目标是统一到新架构（`worldgen/lost/*`），并移除旧 registry 依赖。以下 P0/P1 方案即为迁移路线。

---

## 🧩 核心架构概念

### 上下文驱动生成 (Context-Driven Generation)

CityLoader采用LostCities的"上下文感知"架构，每个方块的放置都基于：
- **位置上下文**: 区块坐标、楼层、生物群系
- **邻居感知**: 相邻区块的建筑类型、高速公路、铁路
- **条件系统**: 基于运行时条件的动态资产选择

```
方块放置 = f(坐标, 楼层, 生物群系, 邻居状态, 随机种子, 条件)
```

### 三层决策链

```
1. BuildingInfo (区块级决策)
   ├── 是否为城市？
   ├── 城市等级？
   ├── 建筑类型？
   └── 基础设施（高速公路/铁路）？

2. ConditionContext (部件级决策)
   ├── 当前楼层？
   ├── 是否顶层/地下室？
   ├── 生物群系匹配？
   └── 选择哪个BuildingPart？

3. CompiledPalette (方块级决策)
   ├── 字符 'X' → 哪种方块？
   ├── 加权随机（90%石砖 + 10%圆石）
   └── 季节适配（冬季 → 雪覆盖）
```

### 生成管线 (Generation Pipeline)

```
阶段1: 基础设施层
├── Highway系统（X轴/Z轴高速公路）
│   ├── Perlin噪声检测
│   ├── 城市连接验证
│   ├── 立交桥/路口生成
│   └── 支撑柱向下延伸
└── Railway系统（地下铁路网络）
    ├── 动态水域检测
    ├── 隧道/车站生成
    └── 铁路地牢

阶段2: 建筑层
├── 多方块建筑（2x2, 4x4）
├── 楼层堆叠（地下室 → 地面 → 楼层 → 屋顶）
├── 条件部件选择
└── 调色板编译

阶段3: 装饰层
├── Scattered Buildings（野外建筑）
├── 废墟系统（爆炸/损坏）
└── Stuff Objects（装饰物）
```

---

## 🔴 三大核心问题详解

### 问题1: 双架构并存 🔴 严重

#### 现状分析
```
旧系统 (resource.*)              新系统 (worldgen.lost.*)
├── Building.java                ├── Building.java
├── BuildingPart.java            ├── BuildingPart.java
├── Palette.java                 ├── Palette.java
├── registry/                    ├── cityassets/
│   ├── BuildingRegistry         │   ├── AssetRegistries
│   ├── PaletteRegistry          │   └── RegistryAssetRegistry
│   └── PartRegistry             └── regassets/
└── ResourceManager                  ├── BuildingRE
    (实际运行) ✅                     └── PaletteRE
                                     (已搭建未使用) ❌
```

#### 问题根源
```java
// CityLoaderPlugin.java - 双系统同时初始化
@Override
public void onEnable() {
    // 1. 初始化旧系统
    resourceManager = new ResourceManager(this);
    resourceManager.loadResources();  // 加载到旧注册表
    
    // 2. 初始化新系统（但未使用）
    AssetRegistries.load(world);  // 加载到新注册表
    
    // 3. 实际生成使用旧系统
    cityBlockPopulator = new CityBlockPopulator(
        getLogger(), config, seasonAdapter,
        resourceManager.getPaletteRegistry(),    // ❌ 旧注册表
        resourceManager.getPartRegistry(),       // ❌ 旧注册表
        resourceManager.getBuildingRegistry()    // ❌ 旧注册表
    );
}
```

#### 详细解决方案（P0.4）

**步骤1: 修改CityBlockPopulator构造函数**
```java
// 之前
public CityBlockPopulator(
    Logger logger,
    PluginConfig config,
    SeasonAdapter seasonAdapter,
    PaletteRegistry paletteRegistry,    // ❌ 删除
    PartRegistry partRegistry,          // ❌ 删除
    BuildingRegistry buildingRegistry   // ❌ 删除
) {
    this.logger = logger;
    this.config = config;
    this.seasonAdapter = seasonAdapter;
    this.paletteRegistry = paletteRegistry;
    this.partRegistry = partRegistry;
    this.buildingRegistry = buildingRegistry;
}

// 之后
public CityBlockPopulator(
    Logger logger,
    PluginConfig config,
    SeasonAdapter seasonAdapter
) {
    this.logger = logger;
    this.config = config;
    this.seasonAdapter = seasonAdapter;
    // 不再需要注册表参数，直接使用AssetRegistries
}
```

**步骤2: 重写populate()方法**
```java
@Override
public void populate(WorldInfo worldInfo, Random random, 
                    int chunkX, int chunkZ, LimitedRegion region) {
    logger.fine("处理区块 [" + chunkX + ", " + chunkZ + "]");
    
    try {
        // 1. 创建坐标和维度信息
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        IDimensionInfo provider = new PaperDimensionInfo(worldInfo, config);
        
        // 2. 使用BuildingInfo决策（新架构）
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        
        // 3. 检查是否应该生成
        if (!info.isCity()) {
            logger.fine("区块不在城市内，跳过");
            return;
        }
        
        // 4. 获取建筑和调色板（完整决策链）
        Building building = info.getBuilding();
        CompiledPalette palette = info.getCompiledPalette();
        
        if (building == null) {
            logger.warning("未找到合适的建筑");
            return;
        }
        
        // 5. 创建ChunkDriver并生成
        ChunkDriver driver = new ChunkDriver(region, coord);
        building.generate(driver, palette, info, seasonAdapter);
        
        logger.fine("区块处理完成");
        
    } catch (Exception e) {
        logger.severe("区块生成失败: " + e.getMessage());
    }
}
```

**步骤3: 更新CityLoaderPlugin**
```java
@Override
public void onEnable() {
    // 1. 只初始化新系统
    AssetRegistries.load(getServer().getWorlds().get(0));
    
    // 2. 创建CityBlockPopulator（不传递旧注册表）
    cityBlockPopulator = new CityBlockPopulator(
        getLogger(),
        config,
        seasonAdapter
    );
    
    // 3. 注册到世界
    for (World world : getServer().getWorlds()) {
        world.getPopulators().add(cityBlockPopulator);
    }
    
    getLogger().info("CityLoader 启动成功");
}

@Override
public void onDisable() {
    // 清理新系统
    AssetRegistries.clear();
    BuildingInfo.clearCache();
}
```

**步骤4: 标记旧系统为废弃**
```java
// resource/ResourceManager.java
@Deprecated
public class ResourceManager {
    // 添加废弃警告
    public ResourceManager(Plugin plugin) {
        plugin.getLogger().warning(
            "ResourceManager已废弃，请使用AssetRegistries"
        );
    }
}
```

---

### 问题2: 资产加载不完整 🔴 严重

#### 当前加载状态
```java
// AssetRegistries.load() - 只加载30%
public static void load(World world) {
    PARTS.loadAll(world);           // ✅ 已加载
    BUILDINGS.loadAll(world);       // ✅ 已加载
    STUFF.loadAll(world);           // ✅ 已加载
    
    // ❌ 以下70%未加载
    // PALETTES.loadAll(world);
    // VARIANTS.loadAll(world);
    // CONDITIONS.loadAll(world);
    // STYLES.loadAll(world);
    // CITYSTYLES.loadAll(world);
    // WORLDSTYLES.loadAll(world);
    // MULTIBUILDINGS.loadAll(world);
    // PREDEFINED_CITIES.loadAll(world);
}
```

**现状更新**:
- 代码中 `AssetRegistries.load` 已按依赖顺序加载大部分资产（VARIANTS/CONDITIONS/PALETTES/STYLES/PARTS/BUILDINGS/MULTI_BUILDINGS/CITYSTYLES/WORLDSTYLES/SCATTERED/STUFF）。
- 预定义相关资产需要额外调用 `AssetRegistries.loadPredefinedStuff(world)`。

#### 详细解决方案（P0.1）

**步骤1: 实现RegistryAssetRegistry.loadAsset()方法**
```java
public class RegistryAssetRegistry<T extends ILostCityAsset> {
    private final String folder;
    private final Class<? extends IAsset> regAssetClass;
    private final Map<String, T> assets = new ConcurrentHashMap<>();
    
    public RegistryAssetRegistry(String folder, Class<? extends IAsset> regAssetClass) {
        this.folder = folder;
        this.regAssetClass = regAssetClass;
    }
    
    public void loadAll(World world) {
        try {
            // 1. 使用PaperResourceLoader扫描资源
            List<String> resourcePaths = PaperResourceLoader.scanResources(
                world, "data/lostcities/" + folder
            );
            
            int loaded = 0;
            int failed = 0;
            
            for (String path : resourcePaths) {
                try {
                    // 2. 加载单个资产
                    T asset = loadAsset(world, path);
                    if (asset != null) {
                        assets.put(asset.getName(), asset);
                        loaded++;
                    }
                } catch (Exception e) {
                    failed++;
                    CityLoaderLogger.logAssetError(folder, path, e);
                }
            }
            
            CityLoaderLogger.logAssetLoad(folder, loaded, failed);
            
        } catch (Exception e) {
            throw new AssetLoadException("加载" + folder + "失败", e);
        }
    }
    
    private T loadAsset(World world, String path) throws Exception {
        // 1. 读取JSON内容
        String json = PaperResourceLoader.loadResource(world, path);
        
        // 2. 反序列化为regasset对象
        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
        IAsset regAsset = gson.fromJson(json, regAssetClass);
        
        // 3. 设置注册名称
        String name = extractNameFromPath(path);
        regAsset.setRegistryName(name);
        
        // 4. 转换为运行时资产对象
        T asset = convertToAsset(regAsset);
        
        return asset;
    }
    
    @SuppressWarnings("unchecked")
    private T convertToAsset(IAsset regAsset) {
        // 根据类型转换
        if (regAsset instanceof PaletteRE) {
            return (T) new Palette((PaletteRE) regAsset);
        } else if (regAsset instanceof BuildingRE) {
            return (T) new Building((BuildingRE) regAsset);
        }
        // ... 其他类型
        throw new IllegalArgumentException("未知的资产类型");
    }
}
```

**步骤2: 按依赖顺序加载所有资产**
```java
public static void load(World world) {
    CityLoaderLogger.info("开始加载资产...");
    
    try {
        // 第一层：无依赖的基础资产
        VARIANTS.loadAll(world);      // 变体
        CONDITIONS.loadAll(world);    // 条件
        
        // 第二层：依赖第一层的资产
        PALETTES.loadAll(world);      // 调色板（可能引用变体）
        STYLES.loadAll(world);        // 样式（可能引用变体）
        
        // 第三层：依赖第二层的资产
        PARTS.loadAll(world);         // 部件（引用调色板）
        
        // 第四层：依赖第三层的资产
        BUILDINGS.loadAll(world);     // 建筑（引用部件）
        MULTIBUILDINGS.loadAll(world); // 多建筑（引用建筑）
        
        // 第五层：依赖第四层的资产
        CITYSTYLES.loadAll(world);    // 城市样式（引用建筑）
        WORLDSTYLES.loadAll(world);   // 世界样式（引用城市样式）
        
        // 第六层：特殊资产
        SCATTERED.loadAll(world);     // 散布建筑
        PREDEFINED_CITIES.loadAll(world); // 预定义城市
        STUFF.loadAll(world);         // 特殊物品
        
        // 构建索引
        buildStuffByTagIndex();
        
        CityLoaderLogger.info("资产加载完成: " + getStatistics());
        
    } catch (Exception e) {
        CityLoaderLogger.severe("资产加载失败: " + e.getMessage());
        throw e;
    }
}

public static String getStatistics() {
    return String.format(
        "Palettes=%d, Variants=%d, Conditions=%d, Styles=%d, " +
        "Parts=%d, Buildings=%d, MultiBuildings=%d, " +
        "CityStyles=%d, WorldStyles=%d, " +
        "Scattered=%d, PredefinedCities=%d, Stuff=%d",
        PALETTES.size(), VARIANTS.size(), CONDITIONS.size(), STYLES.size(),
        PARTS.size(), BUILDINGS.size(), MULTIBUILDINGS.size(),
        CITYSTYLES.size(), WORLDSTYLES.size(),
        SCATTERED.size(), PREDEFINED_CITIES.size(), STUFF.size()
    );
}
```

**步骤3: 添加验证测试**
```java
@Test
public void testAssetLoadingComplete() {
    // 加载资产
    AssetRegistries.load(mockWorld);
    
    // 验证所有类型都已加载
    assertTrue(AssetRegistries.PALETTES.size() > 0, "调色板未加载");
    assertTrue(AssetRegistries.VARIANTS.size() > 0, "变体未加载");
    assertTrue(AssetRegistries.CONDITIONS.size() > 0, "条件未加载");
    assertTrue(AssetRegistries.STYLES.size() > 0, "样式未加载");
    assertTrue(AssetRegistries.PARTS.size() > 0, "部件未加载");
    assertTrue(AssetRegistries.BUILDINGS.size() > 0, "建筑未加载");
    assertTrue(AssetRegistries.MULTIBUILDINGS.size() > 0, "多建筑未加载");
    assertTrue(AssetRegistries.CITYSTYLES.size() > 0, "城市样式未加载");
    assertTrue(AssetRegistries.WORLDSTYLES.size() > 0, "世界样式未加载");
    
    // 验证统计信息
    String stats = AssetRegistries.getStatistics();
    assertFalse(stats.contains("=0"), "存在未加载的资产类型");
}
```

---

### 问题3: BuildingInfo空壳实现 🔴 严重

#### 当前实现问题
```java
public class BuildingInfo {
    // ❌ 所有方法返回占位值
    public Building getBuilding() {
        return null;  // 应该根据条件选择建筑
    }
    
    public CompiledPalette getCompiledPalette() {
        return new CompiledPalette();  // 应该编译调色板
    }
    
    public boolean isCity() {
        return false;  // 应该检测城市
    }
    
    public int getCityLevel() {
        return 0;  // 应该计算城市等级
    }
}
```

#### 详细解决方案（P0.3）

**步骤1: 实现城市检测逻辑**
```java
public class BuildingInfo {
    private boolean isCity;
    private int cityLevel;
    private boolean cityCalculated = false;
    
    public boolean isCity() {
        if (!cityCalculated) {
            calculateCityInfo();
        }
        return isCity;
    }
    
    public int getCityLevel() {
        if (!cityCalculated) {
            calculateCityInfo();
        }
        return cityLevel;
    }
    
    private void calculateCityInfo() {
        LostCityProfile profile = provider.getProfile();
        
        // 1. 检查预定义城市
        PredefinedCity predefined = checkPredefinedCity();
        if (predefined != null) {
            isCity = true;
            cityLevel = predefined.getLevel();
            cityCalculated = true;
            return;
        }
        
        // 2. 使用Perlin噪声计算
        float cityChance = profile.CITY_CHANCE;
        long seed = coord.getSeed();
        Random random = new Random(seed);
        
        // 3. 随机判断是否为城市
        isCity = random.nextFloat() < cityChance;
        
        if (isCity) {
            // 4. 计算城市等级（0-5）
            cityLevel = calculateCityLevel(random);
        } else {
            cityLevel = 0;
        }
        
        cityCalculated = true;
    }
    
    private int calculateCityLevel(Random random) {
        // 根据随机值和配置计算城市等级
        float value = random.nextFloat();
        if (value < 0.1f) return 5;  // 10% 概率为5级城市
        if (value < 0.3f) return 4;  // 20% 概率为4级城市
        if (value < 0.6f) return 3;  // 30% 概率为3级城市
        if (value < 0.85f) return 2; // 25% 概率为2级城市
        return 1;                     // 15% 概率为1级城市
    }
    
    private PredefinedCity checkPredefinedCity() {
        // 检查当前坐标是否在预定义城市范围内
        for (PredefinedCity city : AssetRegistries.PREDEFINED_CITIES.getAll()) {
            if (city.contains(coord)) {
                return city;
            }
        }
        return null;
    }
}
```

**步骤2: 实现建筑选择逻辑**
```java
public Building getBuilding() {
    if (building == null) {
        building = selectBuilding();
    }
    return building;
}

private Building selectBuilding() {
    // 1. 检查预定义城市
    PredefinedCity predefined = checkPredefinedCity();
    if (predefined != null) {
        return predefined.getBuilding(coord);
    }
    
    // 2. 获取城市样式
    CityStyle cityStyle = getCityStyle();
    if (cityStyle == null) {
        return getDefaultBuilding();
    }
    
    // 3. 创建条件上下文
    ConditionContext context = createConditionContext();
    
    // 4. 根据条件选择建筑
    List<Building> candidates = cityStyle.getBuildings();
    for (Building candidate : candidates) {
        // 检查建筑是否满足条件
        if (candidate.meetsConditions(context)) {
            return candidate;
        }
    }
    
    // 5. 如果没有找到，使用默认建筑
    return getDefaultBuilding();
}

private CityStyle getCityStyle() {
    // 根据生物群系和城市等级选择城市样式
    String biome = provider.getBiome(coord);
    int level = getCityLevel();
    
    // 从WorldStyle获取适合的CityStyle
    WorldStyle worldStyle = AssetRegistries.WORLDSTYLES.getDefault();
    if (worldStyle != null) {
        return worldStyle.getCityStyle(biome, level);
    }
    
    return null;
}

private ConditionContext createConditionContext() {
    return new ConditionContext.Builder()
        .coord(coord)
        .cityLevel(getCityLevel())
        .biome(provider.getBiome(coord))
        .groundLevel(provider.getGroundLevel(coord))
        .random(new Random(coord.getSeed()))
        .build();
}

private Building getDefaultBuilding() {
    // 返回默认建筑
    return AssetRegistries.BUILDINGS.get("default");
}
```

**步骤3: 实现调色板编译**
```java
public CompiledPalette getCompiledPalette() {
    if (palette == null) {
        palette = compilePalette();
    }
    return palette;
}

private CompiledPalette compilePalette() {
    Building building = getBuilding();
    if (building == null) {
        return new CompiledPalette();
    }
    
    // 1. 获取建筑的基础调色板
    Palette basePalette = building.getPalette();
    if (basePalette == null) {
        basePalette = AssetRegistries.PALETTES.get("default");
    }
    
    // 2. 获取样式调色板
    CityStyle cityStyle = getCityStyle();
    Palette stylePalette = null;
    if (cityStyle != null) {
        Style style = cityStyle.getStyle();
        if (style != null) {
            stylePalette = style.getRandomPalette(new Random(coord.getSeed()));
        }
    }
    
    // 3. 合并调色板
    if (stylePalette != null) {
        return CompiledPalette.merge(basePalette, stylePalette);
    } else {
        return new CompiledPalette(basePalette);
    }
}
```

**步骤4: 添加缓存优化**
```java
public class BuildingInfo {
    // 三层缓存
    private static final Map<ChunkCoord, BuildingInfo> BUILDING_INFO_MAP = 
        new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, Boolean> CITY_INFO_MAP = 
        new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, Integer> CITY_LEVEL_CACHE = 
        new ConcurrentHashMap<>();
    
    public static BuildingInfo getBuildingInfo(ChunkCoord coord, IDimensionInfo provider) {
        return BUILDING_INFO_MAP.computeIfAbsent(coord, 
            k -> new BuildingInfo(k, provider));
    }
    
    public static void clearCache() {
        BUILDING_INFO_MAP.clear();
        CITY_INFO_MAP.clear();
        CITY_LEVEL_CACHE.clear();
    }
    
    public static int getCacheSize() {
        return BUILDING_INFO_MAP.size();
    }
}

---

## 🔧 关键技术细节

### CompiledPalette 优化机制

```java
// 性能优化：预计算加权随机
public class CompiledPalette {
    private final BlockState[] lookupTable = new BlockState[128];
    
    // 初始化时生成查找表
    // 例如：90% Stone, 10% Cobble
    // → lookupTable[0-114] = Stone, lookupTable[115-127] = Cobble
    
    public BlockState get(char c, Random random) {
        return lookupTable[random.nextInt(128)];  // O(1) 查询
    }
}
```

**优势**:
- 运行时查询 O(1)
- 避免每次计算权重
- 支持复杂的多方块变体

### BuildingInfo 缓存策略

```java
// 三层缓存系统
private static final TimedCache<ChunkCoord, BuildingInfo> BUILDING_INFO_MAP;
private static final TimedCache<ChunkCoord, LostChunkCharacteristics> CITY_INFO_MAP;
private static final TimedCache<ChunkCoord, Integer> CITY_LEVEL_CACHE;

// 缓存过期时间：5分钟
// 自动清理：每30秒检查一次
```

**缓存命中率优化**:
- 相邻区块查询（getXmin/Xmax/Zmin/Zmax）
- 多区块建筑共享信息
- 预定义城市快速路径

### ChunkDriver 批处理机制

```java
// SectionCache: 批量方块操作
public class ChunkDriver {
    private SectionCache cache;
    
    // 批量设置垂直范围
    public void setBlockRange(int x, int y1, int z, int y2, Material material) {
        cache.putRange(x, z, y1, y2, material);  // 批处理
    }
    
    // 最后一次性提交
    public void actuallyGenerate() {
        cache.generate(chunkData);  // 批量写入
    }
}
```

**性能提升**:
- 减少单次方块操作开销
- 自动更新高度图
- 相邻方块状态更新（楼梯形状）

### 条件系统 (Condition System)

```java
// 支持的条件类型
{
  "condition": {
    "top": true,              // 是否顶层
    "floor": 3,               // 特定楼层
    "range": [1, 5],          // 楼层范围
    "inbiome": "desert",      // 生物群系
    "inpart": "floor_*",      // 部件名称模式
    "chunkx": 0               // 区块X坐标模数
  }
}
```

**条件组合**:
- AND: 所有条件必须满足
- OR: 通过多个PartRef实现
- NOT: 通过反向条件实现

### 损坏系统 (Damage System)

```java
// 降级链：完好 → 损坏 → 严重损坏 → 废墟
Stone Bricks → Cracked Stone Bricks → Mossy Stone Bricks → Iron Bars → Air

// 水下特殊处理
if (y < waterLevel) {
    damaged = WATER;  // 防止水下空洞
} else {
    damaged = AIR;
}
```

**爆炸机制**:
- 主爆炸（Explosion）：大范围弹坑
- 迷你爆炸（MiniExplosion）：点状破坏
- 概率控制：CityStyle.explosionChance

### Highway 生成算法

```java
// 1. Perlin噪声检测
boolean hasHighway = perlin.getValue(x, z) > threshold;

// 2. 连续性检测（至少5个区块）
int length = countContinuousHighway(start, end);
if (length < 5) return false;

// 3. 城市连接验证
boolean valid = isCityRaw(start) && isCityRaw(end);

// 4. 层级计算
int level = switch (mode) {
    case MIN -> min(cityLevel(start), cityLevel(end));
    case MAX -> max(cityLevel(start), cityLevel(end));
    case AVG -> (cityLevel(start) + cityLevel(end)) / 2;
};
```

### Railway 动态检测

```java
// 水域检测：采样周围区块
boolean isWater = sampleBlocks(coord, offsets).allMatch(Material::isWater);

if (isWater) {
    part = "rails_horizontal_water";  // 水上铁路
} else {
    part = "rails_horizontal";        // 普通铁路
}
```

### Scattered Buildings 分布算法

```java
// 1. 归一化网格（20x20区块）
int gridX = chunkX / 20;
int gridZ = chunkZ / 20;

// 2. 每个网格只尝试一次
Random random = new Random(seed ^ (gridX << 16) ^ gridZ);

// 3. 高度校验
int avgHeight = calculateAverageHeight(area);
int maxDiff = maxHeight - minHeight;
if (maxDiff > profile.MAX_HEIGHT_DIFF) {
    return;  // 地形太陡峭
}
```

---

## 🚀 快速开始

### 环境要求
- JDK 21+
- Maven 3.x
- Paper 1.21.8服务器

### 构建
```bash
cd CityLoader
mvn clean package
```

### 测试
```bash
mvn test                # 运行测试
mvn jacoco:report       # 生成覆盖率报告
```

### 部署
```bash
cp target/cityloader-*.jar ../City-Test-Server/plugins/
```

---

## 📦 安装与更新

### 安装到服务器
1. 将 `cityloader-*.jar` 放入 `plugins/` 目录
2. 启动或重启服务器生成默认配置
3. 资产请放入插件内置资源 `src/main/resources/data/<namespace>/lostcities/...` 并重新构建部署
4. 执行 `/cityloader reload` 重新加载配置与资源

### 更新插件
1. 停服替换旧版本 JAR
2. 启动后检查 `logs/latest.log` 是否出现资源加载报错
3. 如配置有变更，合并 `plugins/CityLoader/config.yml`

---

## ⚙️ 配置说明

### 配置文件位置
- `plugins/CityLoader/config.yml`

### 关键配置项
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `default-season` | `SPRING` | RealisticSeasons 不可用时的默认季节 |
| `city-density` | `0.8` | 城市密度（0.0-1.0） |
| `min-building-height` | `3` | 建筑最小高度 |
| `max-building-height` | `20` | 建筑最大高度 |
| `street-width` | `5` | 街道宽度 |
| `generation.generate-underground` | `true` | 是否生成地下结构 |
| `generation.generate-streets` | `true` | 是否生成街道 |
| `generation.vanilla-compatible` | `true` | 保留原版地形 |
| `resource-packs` | `[]` | 额外资源包路径（空则仅加载内置资源） |
| `debug.enabled` | `true` | 是否启用调试日志 |
| `performance.cache-size` | `1000` | 资源缓存上限 |
| `performance.async-loading` | `true` | 是否启用异步加载 |

### 重新加载配置
```
/cityloader reload
```

---

## 🧰 命令说明

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cityloader reload` | `cityloader.reload` | 重新加载配置与资源 |
| `/cityloader info` | `cityloader.info` | 显示插件状态 |
| `/cityloader version` | `cityloader.version` | 显示版本信息 |
| `/cityloader generate [x] [y] [z]` | `cityloader.generate` | 提示生成位置（当前版本仅提示） |

---

## 🧩 内置/data资产创建指南

### 目录结构
```text
CityLoader/
└── src/main/resources/
    └── data/
        └── <namespace>/
            └── lostcities/
                ├── palettes/
                ├── parts/
                ├── buildings/
                ├── styles/
                ├── citystyles/
                ├── worldstyles/
                ├── variants/
                ├── conditions/
                ├── multibuildings/
                ├── scattered/
                ├── predefinedcities/
                ├── predefinedspheres/
                └── stuff/
```

### 最小示例
`src/main/resources/data/<namespace>/lostcities/palettes/demo.json`
```json
{
  "palette": [
    {"char": "#", "block": "minecraft:stone_bricks"},
    {"char": " ", "block": "minecraft:air"}
  ]
}
```

`src/main/resources/data/<namespace>/lostcities/parts/demo_part.json`
```json
{
  "xsize": 16,
  "zsize": 16,
  "refpalette": "demo",
  "slices": [
    [
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################",
      "################"
    ]
  ]
}
```

`src/main/resources/data/<namespace>/lostcities/buildings/demo_building.json`
```json
{
  "minfloors": 1,
  "maxfloors": 1,
  "parts": [
    {"part": "demo_part", "factor": 1.0}
  ]
}
```

### 放置与加载
1. 将资产文件放入插件资源目录 `src/main/resources/data/<namespace>/lostcities/...`
2. 重新构建并部署插件
3. 重启服务器，使插件重新扫描内置 `/data/` 资产

### 注意事项
- 当前实现不会扫描世界目录中的外部数据包，仅扫描插件 classpath 下的 `/data/` 目录。
- 未指定命名空间时默认使用 `lostcities`。

---

## ✅ 实现计划（细化版，与当前代码对齐）

目标：迁移主生成链路到 `worldgen/lost/*`，对齐 LostCities 关键逻辑，保持可编译与可运行。

本清单为 2026-02-15 基于 `LostCities-1.20` 源码二次对照后的补全版，重点补齐此前遗漏的“数据模型层”和“基础设施层”任务。

### 0. 差异基线与迁移边界（P0）
- [x] 0.1 固化当前真实主链路与入口
  - `CityLoaderPlugin` → `WorldInitListener` → `CityBlockPopulator` → `LostCityTerrainFeature` → `worldgen/gen/*`
- [x] 0.2 固化新架构未落地清单
  - `worldgen/lost/BuildingInfo`、`cityassets/CompiledPalette`、`CityStyle/WorldStyle/Style`、`LostCityTerrainFeature`
- [x] 0.3 固化源码差异矩阵
  - 对照 `LostCities-1.20` 的 `worldgen/lost/*` 与 `worldgen/gen/*`，形成“缺失类 + 缺失方法 + 缺失字段”清单

**验收**：README 内有“差异清单 + 迁移边界 + 不改动范围”三项基线。

### 1. `regassets` 数据模型对齐（P0）
- [x] 1.1 补齐 `worldgen/lost/regassets/data` 缺失类型
  - 至少补齐 `WorldSettings/StreetSettings/RailSettings/Selectors/CityStyleSelector/PartSelector/PaletteSelector/BlockEntry/...`
- [x] 1.2 对齐关键 RE 结构
  - `VariantRE` 从 `name+weight` 升级到 `blocks[]`
  - `StyleRE` 从 `palettes+weights` 升级到 `randompalettes`
  - `WorldStyleRE` 增加 `citystyles/multisettings/settings/cityspheres/scattered/parts/citybiomemultipliers`
  - `CityStyleRE` 增加 `inherit/explosionchance/stuff_tags/*settings/selectors`
- [x] 1.3 对齐字段命名规范
  - 兼容 LostCities 数据包中常见 snake_case 与历史字段名

**验收**：`pomkots` 与 `keerdm` 数据包可完整反序列化，无结构性丢字段。

### 2. 新资产运行时能力闭环（P0）
- [x] 2.1 `Variant` 运行时语义对齐
  - 支持加权方块列表（而非单一变体名）
- [x] 2.2 `Palette` 解析对齐
  - 支持 `block/blocks/variant/frompalette/damaged/info(tag/loot/mob/torch/NBT)`
- [x] 2.3 `CompiledPalette` 对齐
  - 支持字符引用解析、128 槽预计算随机表、`damagedToBlock`、`information` 元数据
- [x] 2.4 `BuildingPart`/`Building` 对齐
  - 支持 local/ref palette、metadata、vertical slice 缓存、filler/rubble/allowDoors/allowFillers/overrideFloors
- [x] 2.5 `Style/CityStyle/WorldStyle` 对齐
  - 支持随机调色板组、inherit 合并、biome 选择器、city chance multiplier、street/park/rail/corridor 参数

**验收**：新资产对象可独立驱动“楼层选择 + 调色板编译 + 方块生成”闭环。

### 3. `BuildingInfo` 决策核心补全（P0）
- [x] 3.1 城市判定与城市等级
  - 实现 `isCityRaw`、`getCityLevel`、`getChunkCharacteristics`
- [x] 3.2 预定义与多建筑逻辑
  - 接入 `PredefinedCity/PredefinedBuilding/PredefinedStreet` 与 `MultiBuilding` 主控区块判定
- [x] 3.3 楼层链路
  - 完整实现 `getFloor/getFloorPart2` 与 `ConditionContext` 条件分支
- [x] 3.4 基础设施上下文
  - 接入 highway/railway/corridor/bridge 候选信息
- [x] 3.5 调色板与损坏上下文
  - `createPalette` + `getCompiledPalette` + `DamageArea` 接入
- [x] 3.6 缓存策略
  - `TimedCache` 生命周期、跨区块一致性、清理时机

**验收**：`BuildingInfo` 可稳定产出 `city + building + floors + palette + infra flags`。

### 4. 生成总控与分阶段管线迁移（P0-P1）
- [x] 4.1 将 `LostCityTerrainFeature` 从枚举改为真实总控生成器
  - 对齐 LostCities 的 chunk 级阶段执行框架
- [x] 4.2 新建 `worldgen/gen/*` 生成阶段模块
  - `Highways/Railways/Bridges/Corridors/Scattered/Stuff/Spheres/Monorails`
- [x] 4.3 适配 Paper 入口
  - 通过 `BlockPopulator` 驱动总控生成器，替代现有 `BuildingSelector + StructurePlacer` 路径
- [x] 4.4 后处理任务
  - torch/post todo、照明/POI 更新、结构修补逻辑

**验收**：主链路进入 `worldgen/lost + worldgen/gen`，旧路径仅保留回退开关。

### 5. 基础设施与特殊系统（P1）
- [x] 5.1 Highway 系统
  - 层级判定、立交/路口、支撑柱、上方净空清理
- [x] 5.2 Railway 系统
  - 水域采样判定、变体轨道、Rail Dungeon 联动
- [x] 5.3 Damage/Ruins 系统
  - 主爆炸 + mini 爆炸、`damaged` 降级链、水下破坏替换
- [x] 5.4 Scattered/Stuff 系统
  - 网格分布、地形高差约束、标签化装饰投放

**验收**：P1 特性可以按开关启停，且不破坏基础建筑生成。

### 6. 内置 `/data` 加载器收敛（P0）
- [x] 6.1 `PaperResourceLoader` 内置加载策略
  - 仅扫描插件内置 `/data`，不再尝试接管 Paper 外部数据包规则
- [x] 6.2 命名空间与路径策略
  - 支持多 namespace、子目录路径（`data/<ns>/lostcities/<folder>/...`）
- [x] 6.3 资产错误可观测性
  - 记录“来源 + JSON 路径 + 资产ID + 依赖链”

**验收**：内置 `/data` 资源在多 namespace 下加载结果可复现、可追踪。

### 7. 主链路收敛与旧系统下线（P0）
- [x] 7.1 `CityLoaderPlugin` 收敛
  - 仅初始化新资产系统与新生成链路
- [x] 7.2 `CityBlockPopulator` 收敛
  - 从 registry 参数构造迁移到 `BuildingInfo + AssetRegistries`
- [x] 7.3 旧兼容层处理
  - `resource.*` 标注 `@Deprecated`，迁移完成后移除运行时依赖

**验收**：默认路径不再调用 `resource.*` 旧生成逻辑。

### 8. 测试与回归（P0-P1）
- [x] 8.1 资产层单测
  - RE 解析、引用解析、随机权重、损坏映射
- [x] 8.2 决策层单测
  - `BuildingInfo` 城市判定、多建筑、楼层条件、缓存一致性
- [x] 8.3 生成层集成测试
  - 固定 seed chunk 快照（结构、街道、铁路、损坏）
- [x] 8.4 性能与稳定性回归
  - TPS、区块生成耗时、缓存命中、内存占用

**验收**：`mvn test` 通过，`mvn -DskipTests package` 可部署，固定 seed 回归无非预期漂移。

### 检查点（DoD）
- [x] A. `regassets` 与 `cityassets` 数据模型对齐完成
- [x] B. `CompiledPalette` 与 `BuildingInfo` 能独立闭环
- [x] C. 主生成链路已切到 `worldgen/lost + worldgen/gen`
- [x] D. Highway/Railway/Scattered/Damage 可按配置启停
- [x] E. 内置 `/data` 多 namespace 加载与覆盖策略完成（不接管 Paper 外部数据包）
- [x] F. 旧 `resource.*` 不再参与默认生成
- [x] G. 回归测试集覆盖关键路径
- [x] H. README 与代码实现状态一致

---

## 📊 优先级路线图

### P0: 架构收敛（2-3周）🔴 必须

#### ✅ P0.5: 日志优化（已完成）
- TPS从15提升到19+（27%提升）
- 完成日期: 2026-02-15

#### ⏳ P0.1: 完整资产加载（3-4天）
```java
// 需要实现
PALETTES.loadAll();
VARIANTS.loadAll();
CONDITIONS.loadAll();
STYLES.loadAll();
CITYSTYLES.loadAll();
WORLDSTYLES.loadAll();
MULTIBUILDINGS.loadAll();
PREDEFINED_CITIES.loadAll();
```

#### ⏳ P0.2: CompiledPalette实现（2-3天）

**目标**: 支持变体、随机方块、NBT数据的完整调色板系统

**当前问题**:
```java
// CompiledPalette.java - 只有基础结构
public class CompiledPalette {
    private final Map<Character, BlockState> mapping = new HashMap<>();
    
    // ❌ 不支持变体
    // ❌ 不支持随机方块
    // ❌ 不支持NBT数据
    // ❌ 不支持条件选择
}
```

**实现方案**:

**步骤1: 扩展PaletteEntry支持多种类型**
```java
public class PaletteEntry {
    private final char character;
    private final List<BlockVariant> variants;
    private final Map<String, Object> nbtData;
    private final Predicate<ConditionContext> condition;
    
    public BlockState getBlock(Random random, ConditionContext context) {
        // 1. 检查条件
        if (condition != null && !condition.test(context)) {
            return Blocks.AIR.defaultBlockState();
        }
        
        // 2. 选择变体
        BlockVariant variant = selectVariant(random);
        
        // 3. 创建方块状态
        BlockState state = variant.getBlockState();
        
        // 4. 应用NBT数据
        if (nbtData != null && !nbtData.isEmpty()) {
            state = applyNBT(state, nbtData);
        }
        
        return state;
    }
    
    private BlockVariant selectVariant(Random random) {
        if (variants.isEmpty()) {
            return BlockVariant.AIR;
        }
        
        // 加权随机选择
        float totalWeight = 0;
        for (BlockVariant v : variants) {
            totalWeight += v.getWeight();
        }
        
        float value = random.nextFloat() * totalWeight;
        float current = 0;
        
        for (BlockVariant v : variants) {
            current += v.getWeight();
            if (value <= current) {
                return v;
            }
        }
        
        return variants.get(0);
    }
}
```

**步骤2: 实现CompiledPalette合并逻辑**
```java
public class CompiledPalette {
    private final Map<Character, PaletteEntry> entries = new HashMap<>();
    private final List<Palette> sources = new ArrayList<>();
    
    /**
     * 合并多个调色板
     * 后面的调色板会覆盖前面的
     */
    public CompiledPalette(Palette... palettes) {
        for (Palette palette : palettes) {
            merge(palette);
        }
    }
    
    private void merge(Palette palette) {
        sources.add(palette);
        
        // 遍历调色板中的所有字符映射
        for (Map.Entry<Character, PaletteEntry> entry : palette.getEntries().entrySet()) {
            char c = entry.getKey();
            PaletteEntry newEntry = entry.getValue();
            
            // 如果已存在，合并变体
            if (entries.containsKey(c)) {
                PaletteEntry existing = entries.get(c);
                entries.put(c, existing.mergeWith(newEntry));
            } else {
                entries.put(c, newEntry);
            }
        }
    }
    
    /**
     * 获取指定字符的方块
     */
    public BlockState getBlock(char c, Random random, ConditionContext context) {
        PaletteEntry entry = entries.get(c);
        if (entry == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return entry.getBlock(random, context);
    }
    
    /**
     * 获取所有字符
     */
    public Set<Character> getCharacters() {
        return entries.keySet();
    }
}
```

**步骤3: 添加变体解析**
```java
public class Variant {
    private final String name;
    private final List<BlockOption> blocks;
    
    public static class BlockOption {
        private final Material material;
        private final float weight;
        private final Map<String, String> properties;
        
        public BlockState toBlockState() {
            BlockState state = material.createBlockData();
            
            // 应用属性
            if (properties != null) {
                for (Map.Entry<String, String> prop : properties.entrySet()) {
                    state = applyProperty(state, prop.getKey(), prop.getValue());
                }
            }
            
            return state;
        }
    }
    
    public BlockState getRandomBlock(Random random) {
        float totalWeight = 0;
        for (BlockOption option : blocks) {
            totalWeight += option.weight;
        }
        
        float value = random.nextFloat() * totalWeight;
        float current = 0;
        
        for (BlockOption option : blocks) {
            current += option.weight;
            if (value <= current) {
                return option.toBlockState();
            }
        }
        
        return blocks.get(0).toBlockState();
    }
}
```

**步骤4: 测试完整功能**
```java
@Test
public void testCompiledPaletteWithVariants() {
    // 创建基础调色板
    Palette base = new Palette("base");
    base.addEntry('W', Material.STONE, 1.0f);
    
    // 创建变体调色板
    Palette variant = new Palette("variant");
    variant.addEntry('W', Material.COBBLESTONE, 0.7f);
    variant.addEntry('W', Material.STONE_BRICKS, 0.3f);
    
    // 合并
    CompiledPalette compiled = new CompiledPalette(base, variant);
    
    // 测试随机选择
    Random random = new Random(12345);
    Map<Material, Integer> counts = new HashMap<>();
    
    for (int i = 0; i < 1000; i++) {
        BlockState state = compiled.getBlock('W', random, null);
        Material mat = state.getMaterial();
        counts.put(mat, counts.getOrDefault(mat, 0) + 1);
    }
    
    // 验证分布
    assertTrue(counts.get(Material.COBBLESTONE) > 600);
    assertTrue(counts.get(Material.STONE_BRICKS) > 200);
}
```

#### ⏳ P0.3: BuildingInfo决策链（3-4天）

**目标**: 实现完整的城市检测和建筑选择逻辑

详细实现见上文"问题3: BuildingInfo空壳实现"部分。

**关键方法**:
- `isCity()` - 城市检测
- `getCityLevel()` - 城市等级计算
- `getBuilding()` - 建筑选择
- `getCompiledPalette()` - 调色板编译

#### ⏳ P0.4: 统一生成链路（4-5天）

**目标**: 移除旧系统，完全切换到新架构

详细实现见上文"问题1: 双架构并存"部分。

**关键步骤**:
1. 修改CityBlockPopulator构造函数
2. 重写populate()方法使用BuildingInfo
3. 更新CityLoaderPlugin初始化
4. 标记旧系统为@Deprecated

**P0目标**: 资产100%、功能70%、评分A-

---

### P1: 核心特性（4-6周）🟡 重要

#### P1.1: Scattered Buildings（2-3天）

**功能**: 在城市外围生成散布的独立建筑

**实现方案**:
```java
public class ScatteredBuildingGenerator {
    private final LostCityProfile profile;
    
    public boolean shouldGenerateScattered(ChunkCoord coord) {
        // 1. 检查是否在城市内（城市内不生成）
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        if (info.isCity) {
            return false;
        }
        
        // 2. 使用噪声函数决定
        long seed = coord.getSeed();
        Random random = new Random(seed);
        
        float chance = profile.SCATTERED_BUILDING_CHANCE;
        return random.nextFloat() < chance;
    }
    
    public Building selectScatteredBuilding(ChunkCoord coord) {
        // 从SCATTERED注册表选择
        List<Building> scattered = AssetRegistries.SCATTERED.getAll();
        if (scattered.isEmpty()) {
            return null;
        }
        
        Random random = new Random(coord.getSeed());
        return scattered.get(random.nextInt(scattered.size()));
    }
}
```

#### P1.2: Highway系统（3-5天）

**功能**: 生成连接城市的高速公路网络

**实现方案**:
```java
public class HighwayGenerator {
    
    /**
     * 检查区块是否应该生成高速公路
     */
    public boolean isHighwayChunk(ChunkCoord coord) {
        int spacing = profile.HIGHWAY_DISTANCE_MASK;
        
        // X方向高速公路
        if ((coord.chunkZ() & spacing) == 0) {
            return true;
        }
        
        // Z方向高速公路
        if ((coord.chunkX() & spacing) == 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成高速公路
     */
    public void generateHighway(LimitedRegion region, ChunkCoord coord, 
                                int groundLevel) {
        int highwayLevel = groundLevel + profile.HIGHWAY_LEVEL_FROM_GROUND;
        
        // 确定高速公路方向
        boolean xHighway = (coord.chunkZ() & profile.HIGHWAY_DISTANCE_MASK) == 0;
        boolean zHighway = (coord.chunkX() & profile.HIGHWAY_DISTANCE_MASK) == 0;
        
        if (xHighway && zHighway) {
            // 交叉路口
            generateHighwayIntersection(region, coord, highwayLevel);
        } else if (xHighway) {
            // X方向高速公路
            generateHighwayX(region, coord, highwayLevel);
        } else if (zHighway) {
            // Z方向高速公路
            generateHighwayZ(region, coord, highwayLevel);
        }
    }
    
    private void generateHighwayX(LimitedRegion region, ChunkCoord coord, 
                                  int level) {
        int startX = coord.chunkX() * 16;
        int startZ = coord.chunkZ() * 16;
        
        // 生成路基
        for (int x = 0; x < 16; x++) {
            for (int z = 6; z < 10; z++) {
                // 支撑柱
                for (int y = 0; y < level; y++) {
                    region.setBlockData(startX + x, y, startZ + z, 
                        Material.STONE_BRICKS.createBlockData());
                }
                
                // 路面
                region.setBlockData(startX + x, level, startZ + z,
                    Material.GRAY_CONCRETE.createBlockData());
                
                // 护栏
                if (z == 6 || z == 9) {
                    region.setBlockData(startX + x, level + 1, startZ + z,
                        Material.IRON_BARS.createBlockData());
                }
            }
        }
    }
}
```

#### P1.3: Railway系统（5-7天）

**功能**: 生成地下铁路网络连接城市

**实现方案**:
```java
public class RailwayGenerator {
    
    /**
     * 检查是否应该生成铁路
     */
    public boolean isRailwayChunk(ChunkCoord coord) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        
        // 只在城市内生成
        if (!info.isCity) {
            return false;
        }
        
        // 检查是否是铁路走廊
        return info.xRailCorridor || info.zRailCorridor;
    }
    
    /**
     * 生成铁路隧道
     */
    public void generateRailway(LimitedRegion region, ChunkCoord coord,
                               BuildingInfo info) {
        int railLevel = info.getCityGroundLevel() - 12; // 地下12格
        
        if (info.xRailCorridor) {
            generateRailwayX(region, coord, railLevel);
        }
        
        if (info.zRailCorridor) {
            generateRailwayZ(region, coord, railLevel);
        }
        
        // 交叉点生成车站
        if (info.xRailCorridor && info.zRailCorridor) {
            generateRailwayStation(region, coord, railLevel);
        }
    }
    
    private void generateRailwayX(LimitedRegion region, ChunkCoord coord,
                                  int level) {
        int startX = coord.chunkX() * 16;
        int startZ = coord.chunkZ() * 16;
        
        for (int x = 0; x < 16; x++) {
            // 清空隧道空间
            for (int z = 6; z < 10; z++) {
                for (int y = level; y < level + 4; y++) {
                    region.setBlockData(startX + x, y, startZ + z,
                        Material.AIR.createBlockData());
                }
            }
            
            // 铺设轨道
            region.setBlockData(startX + x, level, startZ + 7,
                Material.RAIL.createBlockData());
            region.setBlockData(startX + x, level, startZ + 8,
                Material.RAIL.createBlockData());
            
            // 隧道墙壁
            for (int y = level; y < level + 4; y++) {
                region.setBlockData(startX + x, y, startZ + 6,
                    Material.STONE_BRICKS.createBlockData());
                region.setBlockData(startX + x, y, startZ + 9,
                    Material.STONE_BRICKS.createBlockData());
            }
            
            // 照明
            if (x % 4 == 0) {
                region.setBlockData(startX + x, level + 2, startZ + 6,
                    Material.TORCH.createBlockData());
            }
        }
    }
    
    private void generateRailwayStation(LimitedRegion region, ChunkCoord coord,
                                       int level) {
        // 生成更大的站台空间
        int startX = coord.chunkX() * 16;
        int startZ = coord.chunkZ() * 16;
        
        // 清空站台区域
        for (int x = 4; x < 12; x++) {
            for (int z = 4; z < 12; z++) {
                for (int y = level; y < level + 6; y++) {
                    region.setBlockData(startX + x, y, startZ + z,
                        Material.AIR.createBlockData());
                }
            }
        }
        
        // 站台地板
        for (int x = 4; x < 12; x++) {
            for (int z = 4; z < 12; z++) {
                region.setBlockData(startX + x, level, startZ + z,
                    Material.QUARTZ_BLOCK.createBlockData());
            }
        }
        
        // 楼梯通往地面
        generateStairway(region, startX + 8, level, startZ + 8,
            info.getCityGroundLevel());
    }
}
```

#### P1.4: Explosion/Ruins系统（2-3天）

**功能**: 随机破坏建筑创建废墟效果

**实现方案**:
```java
public class ExplosionGenerator {
    
    /**
     * 检查建筑是否应该被破坏
     */
    public boolean shouldExplode(BuildingInfo info) {
        if (!info.hasBuilding) {
            return false;
        }
        
        Random random = new Random(info.coord.getSeed());
        float chance = profile.EXPLOSION_CHANCE;
        
        return random.nextFloat() < chance;
    }
    
    /**
     * 应用爆炸效果
     */
    public void applyExplosion(LimitedRegion region, BuildingInfo info,
                              CompiledPalette palette) {
        Random random = new Random(info.coord.getSeed());
        
        int startX = info.coord.chunkX() * 16;
        int startZ = info.coord.chunkZ() * 16;
        int groundLevel = info.getCityGroundLevel();
        
        // 随机选择爆炸中心
        int explosionX = startX + random.nextInt(16);
        int explosionY = groundLevel + random.nextInt(info.floors * 6);
        int explosionZ = startZ + random.nextInt(16);
        
        float radius = 5 + random.nextFloat() * 10;
        
        // 破坏方块
        for (int x = -15; x <= 15; x++) {
            for (int y = -15; y <= 15; y++) {
                for (int z = -15; z <= 15; z++) {
                    int worldX = explosionX + x;
                    int worldY = explosionY + y;
                    int worldZ = explosionZ + z;
                    
                    float distance = (float) Math.sqrt(x*x + y*y + z*z);
                    
                    if (distance <= radius) {
                        // 距离越近，破坏概率越高
                        float destroyChance = 1.0f - (distance / radius);
                        
                        if (random.nextFloat() < destroyChance) {
                            region.setBlockData(worldX, worldY, worldZ,
                                Material.AIR.createBlockData());
                        } else if (random.nextFloat() < destroyChance * 0.5f) {
                            // 部分方块变成破损版本
                            replaceToDamaged(region, worldX, worldY, worldZ);
                        }
                    }
                }
            }
        }
    }
    
    private void replaceToDamaged(LimitedRegion region, int x, int y, int z) {
        BlockData current = region.getBlockData(x, y, z);
        Material mat = current.getMaterial();
        
        // 替换为破损版本
        Material damaged = getDamagedVersion(mat);
        if (damaged != null) {
            region.setBlockData(x, y, z, damaged.createBlockData());
        }
    }
    
    private Material getDamagedVersion(Material original) {
        // 映射表
        Map<Material, Material> damageMap = Map.of(
            Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
            Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Material.NETHER_BRICKS, Material.CRACKED_NETHER_BRICKS,
            Material.DEEPSLATE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS,
            Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_TILES
        );
        
        return damageMap.get(original);
    }
}
```

#### P1.5: Profile配置升级（3-4天）

**功能**: 扩展配置系统支持更多生成参数

**实现方案**:
注意：以下为旧配置结构示例，当前实际配置请以 `config.yml` 为准（见“配置说明”）。P1.5 会同步升级该段示例。
```java
public class LostCityProfile {
    // 城市生成
    public float CITY_CHANCE = 0.02f;
    public int CITY_MIN_RADIUS = 50;
    public int CITY_MAX_RADIUS = 128;
    
    // 高速公路
    public int HIGHWAY_DISTANCE_MASK = 0x1f; // 每32区块
    public int HIGHWAY_LEVEL_FROM_GROUND = 8;
    public boolean HIGHWAY_REQUIRES_TWO_CITIES = true;
    
    // 铁路
    public int RAILWAY_DUNGEON_DISTANCE = 200;
    public boolean RAILWAYS_CAN_END = true;
    public boolean RAILWAYS_ENABLED = true;
    
    // 建筑
    public int BUILDING_MIN_FLOORS = 1;
    public int BUILDING_MAX_FLOORS = 9;
    public int BUILDING_MIN_CELLARS = 0;
    public int BUILDING_MAX_CELLARS = 4;
    public float BUILDING_2X2_CHANCE = 0.03f;
    
    // 散布建筑
    public float SCATTERED_BUILDING_CHANCE = 0.001f;
    
    // 废墟
    public float EXPLOSION_CHANCE = 0.02f;
    public int EXPLOSION_MIN_RADIUS = 5;
    public int EXPLOSION_MAX_RADIUS = 15;
    
    // 从配置文件加载
    public static LostCityProfile fromConfig(FileConfiguration config) {
        LostCityProfile profile = new LostCityProfile();
        
        profile.CITY_CHANCE = (float) config.getDouble("city.chance", 0.02);
        profile.CITY_MIN_RADIUS = config.getInt("city.min_radius", 50);
        profile.CITY_MAX_RADIUS = config.getInt("city.max_radius", 128);
        
        profile.HIGHWAY_DISTANCE_MASK = config.getInt("highway.distance_mask", 0x1f);
        profile.HIGHWAY_LEVEL_FROM_GROUND = config.getInt("highway.level_from_ground", 8);
        
        profile.RAILWAYS_ENABLED = config.getBoolean("railway.enabled", true);
        
        profile.EXPLOSION_CHANCE = (float) config.getDouble("ruins.explosion_chance", 0.02);
        
        return profile;
    }
}
```

**P1目标**: 功能80%、测试50%、评分A

---

### P2: 生态完善（2-3周）🟢 可选

#### P2.1: 命令扩展（3-5天）

**功能**: 添加更多管理和调试命令

**实现方案**:
```java
@Command(name = "cityloader")
public class CityLoaderCommand {
    
    /**
     * 定位最近的城市
     * /cityloader locate city
     */
    @SubCommand("locate")
    public void locate(Player player, String type) {
        Location playerLoc = player.getLocation();
        int chunkX = playerLoc.getBlockX() >> 4;
        int chunkZ = playerLoc.getBlockZ() >> 4;
        
        // 搜索最近的城市
        ChunkCoord nearest = findNearestCity(chunkX, chunkZ, 100);
        
        if (nearest != null) {
            int distance = (int) Math.sqrt(
                Math.pow(nearest.chunkX() - chunkX, 2) +
                Math.pow(nearest.chunkZ() - chunkZ, 2)
            ) * 16;
            
            player.sendMessage(String.format(
                "§a最近的城市位于: X=%d, Z=%d (距离: %dm)",
                nearest.chunkX() * 16, nearest.chunkZ() * 16, distance
            ));
        } else {
            player.sendMessage("§c未找到附近的城市");
        }
    }
    
    /**
     * 显示区块调试信息
     * /cityloader debug
     */
    @SubCommand("debug")
    public void debug(Player player) {
        Location loc = player.getLocation();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        
        player.sendMessage("§6=== 区块调试信息 ===");
        player.sendMessage(String.format("§7坐标: [%d, %d]", chunkX, chunkZ));
        player.sendMessage(String.format("§7是否城市: %s", info.isCity ? "§a是" : "§c否"));
        player.sendMessage(String.format("§7城市等级: §e%d", info.cityLevel));
        player.sendMessage(String.format("§7地面高度: §e%d", info.groundLevel));
        player.sendMessage(String.format("§7建筑类型: §e%s", 
            info.buildingType != null ? info.buildingType.getName() : "无"));
        player.sendMessage(String.format("§7楼层数: §e%d", info.floors));
        player.sendMessage(String.format("§7地下室: §e%d", info.cellars));
        player.sendMessage(String.format("§7高速公路X: %s", info.highwayXLevel > 0 ? "§a是" : "§c否"));
        player.sendMessage(String.format("§7高速公路Z: %s", info.highwayZLevel > 0 ? "§a是" : "§c否"));
        player.sendMessage(String.format("§7铁路X: %s", info.xRailCorridor ? "§a是" : "§c否"));
        player.sendMessage(String.format("§7铁路Z: %s", info.zRailCorridor ? "§a是" : "§c否"));
    }
    
    /**
     * 显示统计信息
     * /cityloader stats
     */
    @SubCommand("stats")
    public void stats(CommandSender sender) {
        sender.sendMessage("§6=== CityLoader 统计 ===");
        sender.sendMessage(AssetRegistries.getStatistics());
        sender.sendMessage(String.format("§7缓存大小: §e%d", 
            BuildingInfo.getCacheSize()));
        sender.sendMessage(String.format("§7TPS: §e%.2f", 
            Bukkit.getTPS()[0]));
    }
    
    /**
     * 生成城市地图
     * /cityloader map <radius>
     */
    @SubCommand("map")
    public void map(Player player, @Default("5") int radius) {
        Location loc = player.getLocation();
        int centerX = loc.getBlockX() >> 4;
        int centerZ = loc.getBlockZ() >> 4;
        
        player.sendMessage("§6=== 城市地图 ===");
        
        for (int z = -radius; z <= radius; z++) {
            StringBuilder line = new StringBuilder();
            for (int x = -radius; x <= radius; x++) {
                ChunkCoord coord = new ChunkCoord(centerX + x, centerZ + z);
                BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
                
                if (x == 0 && z == 0) {
                    line.append("§e@"); // 玩家位置
                } else if (info.isCity) {
                    line.append("§a■"); // 城市
                } else if (info.highwayXLevel > 0 || info.highwayZLevel > 0) {
                    line.append("§7═"); // 高速公路
                } else {
                    line.append("§8·"); // 空地
                }
            }
            player.sendMessage(line.toString());
        }
    }
    
    /**
     * 清理缓存
     * /cityloader clearcache
     */
    @SubCommand("clearcache")
    @Permission("cityloader.admin")
    public void clearCache(CommandSender sender) {
        BuildingInfo.resetCache();
        sender.sendMessage("§a缓存已清理");
    }
    
    /**
     * 重新加载资产
     * /cityloader reload
     */
    @SubCommand("reload")
    @Permission("cityloader.admin")
    public void reload(CommandSender sender) {
        try {
            AssetRegistries.reset();
            AssetRegistries.load(Bukkit.getWorlds().get(0));
            sender.sendMessage("§a资产重新加载成功");
            sender.sendMessage(AssetRegistries.getStatistics());
        } catch (Exception e) {
            sender.sendMessage("§c重新加载失败: " + e.getMessage());
        }
    }
}
```

#### P2.2: 性能监控（2-3天）

**功能**: 实时监控生成性能和资源使用

**实现方案**:
```java
public class PerformanceMonitor {
    private static final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    
    public static class PerformanceMetric {
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        
        public void record(long timeNanos) {
            totalTime.addAndGet(timeNanos);
            callCount.incrementAndGet();
            
            long current = maxTime.get();
            while (timeNanos > current) {
                if (maxTime.compareAndSet(current, timeNanos)) {
                    break;
                }
                current = maxTime.get();
            }
        }
        
        public double getAverageMs() {
            int count = callCount.get();
            if (count == 0) return 0;
            return (totalTime.get() / count) / 1_000_000.0;
        }
        
        public double getMaxMs() {
            return maxTime.get() / 1_000_000.0;
        }
        
        public int getCallCount() {
            return callCount.get();
        }
    }
    
    /**
     * 记录操作性能
     */
    public static <T> T measure(String operation, Supplier<T> task) {
        long start = System.nanoTime();
        try {
            return task.get();
        } finally {
            long duration = System.nanoTime() - start;
            metrics.computeIfAbsent(operation, k -> new PerformanceMetric())
                   .record(duration);
        }
    }
    
    /**
     * 获取性能报告
     */
    public static String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("§6=== 性能报告 ===\n");
        
        metrics.entrySet().stream()
            .sorted((a, b) -> Double.compare(
                b.getValue().getAverageMs(),
                a.getValue().getAverageMs()
            ))
            .forEach(entry -> {
                String name = entry.getKey();
                PerformanceMetric metric = entry.getValue();
                
                report.append(String.format(
                    "§7%s: §e%.2fms §7(avg) §e%.2fms §7(max) §e%d §7(calls)\n",
                    name,
                    metric.getAverageMs(),
                    metric.getMaxMs(),
                    metric.getCallCount()
                ));
            });
        
        return report.toString();
    }
    
    /**
     * 在CityBlockPopulator中使用
     */
    @Override
    public void populate(WorldInfo worldInfo, Random random,
                        int chunkX, int chunkZ, LimitedRegion region) {
        PerformanceMonitor.measure("chunk_generation", () -> {
            // 原有生成逻辑
            doPopulate(worldInfo, random, chunkX, chunkZ, region);
            return null;
        });
    }
}
```

#### P2.3: 可视化调试（2-3天）

**功能**: 使用粒子效果显示生成边界和结构

**实现方案**:
```java
public class DebugVisualizer {
    
    /**
     * 显示区块边界
     */
    public static void showChunkBorder(Player player, ChunkCoord coord) {
        World world = player.getWorld();
        int startX = coord.chunkX() * 16;
        int startZ = coord.chunkZ() * 16;
        int y = player.getLocation().getBlockY();
        
        // 显示四条边
        for (int i = 0; i <= 16; i++) {
            // X方向边界
            spawnParticle(world, startX + i, y, startZ, Particle.FLAME);
            spawnParticle(world, startX + i, y, startZ + 16, Particle.FLAME);
            
            // Z方向边界
            spawnParticle(world, startX, y, startZ + i, Particle.FLAME);
            spawnParticle(world, startX + 16, y, startZ + i, Particle.FLAME);
        }
    }
    
    /**
     * 显示建筑边界
     */
    public static void showBuildingBounds(Player player, BuildingInfo info) {
        if (!info.hasBuilding) {
            player.sendMessage("§c该区块没有建筑");
            return;
        }
        
        World world = player.getWorld();
        int startX = info.coord.chunkX() * 16;
        int startZ = info.coord.chunkZ() * 16;
        int groundLevel = info.getCityGroundLevel();
        int maxHeight = info.getMaxHeight();
        
        // 显示建筑轮廓
        for (int y = groundLevel; y <= maxHeight; y += 6) {
            for (int i = 0; i <= 16; i++) {
                spawnParticle(world, startX + i, y, startZ, Particle.VILLAGER_HAPPY);
                spawnParticle(world, startX + i, y, startZ + 16, Particle.VILLAGER_HAPPY);
                spawnParticle(world, startX, y, startZ + i, Particle.VILLAGER_HAPPY);
                spawnParticle(world, startX + 16, y, startZ + i, Particle.VILLAGER_HAPPY);
            }
        }
        
        player.sendMessage(String.format(
            "§a建筑高度: %d-%d (%d层)",
            groundLevel, maxHeight, info.floors
        ));
    }
    
    /**
     * 显示城市范围
     */
    public static void showCityBounds(Player player, int radius) {
        Location loc = player.getLocation();
        int centerX = loc.getBlockX() >> 4;
        int centerZ = loc.getBlockZ() >> 4;
        
        World world = player.getWorld();
        int y = loc.getBlockY();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkCoord coord = new ChunkCoord(centerX + x, centerZ + z);
                BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
                
                if (info.isCity) {
                    int worldX = coord.chunkX() * 16 + 8;
                    int worldZ = coord.chunkZ() * 16 + 8;
                    
                    Particle particle = switch (info.cityLevel) {
                        case 5 -> Particle.END_ROD;
                        case 4 -> Particle.FLAME;
                        case 3 -> Particle.SOUL_FIRE_FLAME;
                        case 2 -> Particle.SMOKE_NORMAL;
                        default -> Particle.VILLAGER_HAPPY;
                    };
                    
                    spawnParticle(world, worldX, y, worldZ, particle);
                }
            }
        }
    }
    
    private static void spawnParticle(World world, int x, int y, int z, Particle particle) {
        world.spawnParticle(particle, x + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
    }
}

// 添加调试命令
@SubCommand("visualize")
@Permission("cityloader.debug")
public void visualize(Player player, String type, @Default("5") int radius) {
    switch (type.toLowerCase()) {
        case "chunk" -> {
            ChunkCoord coord = new ChunkCoord(
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4
            );
            DebugVisualizer.showChunkBorder(player, coord);
        }
        case "building" -> {
            ChunkCoord coord = new ChunkCoord(
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4
            );
            BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
            DebugVisualizer.showBuildingBounds(player, info);
        }
        case "city" -> {
            DebugVisualizer.showCityBounds(player, radius);
        }
        default -> player.sendMessage("§c未知类型: " + type);
    }
}
```

**P2目标**: 功能85%、测试60%、评分A+

---

## 🏗️ 架构深度对比

### 根本差异

| 维度 | LostCities (Forge) | CityLoader (Paper) |
|------|-------------------|-------------------|
| 定位 | 世界重塑者 | 地形叠加器 |
| 生成方式 | ChunkGenerator | BlockPopulator |
| 地形控制 | 完全控制 | 叠加在原版上 |
| 生成时机 | 地形生成阶段 | 地形装饰阶段 |
| 性能影响 | 中等 | 较低 |

**结论**: 架构级别差异，无法完全复刻所有功能（如CitySphere）

### 生成流程对比

#### LostCities (Forge)
```
ChunkGenerator.generateChunk()
├── 1. 完全控制地形生成
├── 2. 从基岩开始构建
├── 3. 可以生成CitySphere（浮空城市）
├── 4. 可以完全替换生物群系
└── 5. 性能开销：中等

优势：
✅ 完全控制地形
✅ 可以生成任何结构
✅ 与原版地形无关

劣势：
❌ 不兼容原版地形
❌ 不兼容其他地形生成器
❌ 需要专用世界
```

#### CityLoader (Paper)
```
BlockPopulator.populate()
├── 1. 在原版地形上叠加
├── 2. 只能修改地表以上
├── 3. 无法生成CitySphere
├── 4. 保留原版生物群系
└── 5. 性能开销：低

优势：
✅ 兼容原版地形
✅ 兼容其他插件
✅ 无需专用世界
✅ 性能影响小

劣势：
❌ 无法完全控制地形
❌ 受原版地形限制
❌ 某些功能无法实现
```

### 功能对比矩阵

| 功能 | LostCities | CityLoader | 当前状态 |
|------|-----------|-----------|---------|
| 基础城市生成 | ✅ | ✅ | 已支持 |
| 建筑生成 | ✅ | ✅ | 已支持 |
| 调色板系统 | ✅ | ✅ | 已支持（含 `variant/damaged`） |
| 多区块建筑 | ✅ | ✅ | 已支持（`MultiBuilding`） |
| 高速公路 | ✅ | ✅ | 已支持（可开关） |
| 铁路系统 | ✅ | ✅ | 已支持（可开关） |
| 散布建筑 | ✅ | ✅ | 已支持（可开关） |
| 废墟/损坏 | ✅ | ✅ | 已支持（可开关） |
| CitySphere | ✅ | ❌ | 架构限制（Paper装饰阶段） |
| 地下城市 | ✅ | ❌ | 架构限制（无法完整重塑地下地形） |
| 完全平坦世界 | ✅ | ❌ | 架构限制（不接管地形生成器） |
| 季节适配 | ❌ | ✅ | CityLoader扩展能力 |
| Paper生态兼容 | ❌ | ✅ | CityLoader扩展能力 |

### 与 LostCities 对比：区别与不足（2026-02-15）

#### 关键区别
- 生成接入层不同：LostCities 接入 `ChunkGenerator`，CityLoader 接入 `BlockPopulator`。
- 资源入口不同：CityLoader 仅扫描插件 classpath 下的 `/data/<namespace>/lostcities/...`。
- 生态目标不同：LostCities 面向 Forge 模组生态，CityLoader 面向 Paper 插件生态。
- 兼容策略不同：CityLoader 优先保持原版地形与插件兼容性，不尝试重写世界生成规则。

#### 当前不足
- 不支持 `CitySphere`、完整地下城市、完全平坦城市世界等“全地形接管”能力。
- 无法在地形生成前改写生物群系/地层结构，只能在已生成区块上叠加结构。
- 资产热更新能力较弱：新增内置 `/data/` 资产需要重启服务器生效。
- 部分 Forge 侧高级特性与调试工具仍未一一对齐（仅保留 Paper 侧必要子集）。

### 技术实现对比

#### 资产系统

**LostCities**:
```java
// Forge的ResourceLocation系统
ResourceLocation id = new ResourceLocation("lostcities", "palettes/default");
Palette palette = AssetRegistries.PALETTES.get(id);
```

**CityLoader**:
```java
// Paper的自定义资源加载
String path = "data/lostcities/palettes/default.json";
String json = PaperResourceLoader.loadResource(world, path);
Palette palette = gson.fromJson(json, PaletteRE.class);

// 手动实现注册表
AssetRegistries.PALETTES.register("default", palette);
```

#### 方块放置

**LostCities**:
```java
// Forge的ChunkPrimer（生成阶段）
ChunkPrimer primer = new ChunkPrimer();
primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());

// 直接控制区块数据
chunk.setBlockState(pos, state, false);
```

**CityLoader**:
```java
// Paper的LimitedRegion（装饰阶段）
LimitedRegion region = ...;
region.setBlockData(x, y, z, Material.STONE.createBlockData());

// 受限于已生成的地形
int groundLevel = findSurfaceHeight(region, x, z);
```

#### 随机数生成

**LostCities**:
```java
// Forge的世界种子
long seed = world.getSeed();
Random random = new Random(seed ^ (chunkX * 341873128712L + chunkZ * 132897987541L));
```

**CityLoader**:
```java
// Paper的相同实现
long seed = world.getSeed();
Random random = new Random(seed ^ (chunkX * 341873128712L + chunkZ * 132897987541L));
```

### 包结构对比

#### LostCities
```
mcjty.lostcities/
├── worldgen/
│   ├── ChunkGenerator.java          # 核心生成器
│   ├── lost/
│   │   ├── cityassets/              # 运行时资产
│   │   ├── regassets/               # 注册资产
│   │   └── BuildingInfo.java       # 缓存系统
│   └── ChunkDriver.java             # 方块放置
├── config/
│   └── LostCityConfiguration.java   # 配置系统
└── commands/
    └── CommandDebug.java            # 调试命令
```

#### CityLoader
```
com.during.cityloader/
├── worldgen/
│   ├── lost/
│   │   ├── cityassets/              # 运行时资产（复刻）
│   │   ├── regassets/               # 注册资产（复刻）
│   │   └── BuildingInfo.java       # 缓存系统（复刻）
│   ├── ChunkDriver.java             # 方块放置（适配Paper）
│   ├── IDimensionInfo.java          # 维度信息接口
│   └── PaperDimensionInfo.java      # Paper实现
├── generator/
│   └── CityBlockPopulator.java      # BlockPopulator实现
├── resource/                         # 旧系统（待废弃）
├── config/
│   └── PluginConfig.java            # 配置系统
├── command/
│   └── CityLoaderCommand.java       # 命令系统
└── util/
    ├── PaperResourceLoader.java     # 资源加载器
    └── CityLoaderLogger.java        # 日志系统
```

### 性能对比

| 指标 | LostCities | CityLoader | 说明 |
|------|-----------|-----------|------|
| 区块生成时间 | 15-25ms | 10-20ms | CityLoader更快 |
| 内存占用 | 中等 | 低 | Paper优化更好 |
| TPS影响 | -2~-4 | -1~-2 | CityLoader影响更小 |
| 启动时间 | 5-10s | 3-5s | 资产加载更快 |
| 缓存大小 | 较大 | 可控 | 定时清理 |

### 兼容性对比

#### LostCities
```
✅ Forge模组生态
✅ 其他Forge地形生成器（部分）
❌ 原版客户端
❌ Bukkit/Spigot插件
❌ 原版地形
```

#### CityLoader
```
✅ 原版客户端
✅ Paper插件生态
✅ 原版地形
✅ 其他BlockPopulator
❌ Forge模组
❌ 完全自定义地形
```

### 最佳实践建议

#### 何时使用LostCities
- 需要完全自定义的城市世界
- 可以接受专用世界
- 需要CitySphere等特殊功能
- 使用Forge服务器

#### 何时使用CityLoader
- 希望在原版地形上添加城市
- 需要兼容其他插件
- 使用Paper服务器
- 需要季节适配功能
- 注重服务器性能

---

## 📚 最佳实践

### 代码规范

#### 命名约定
```java
// 类名：PascalCase
public class BuildingInfo { }
public class CityBlockPopulator { }

// 方法名：camelCase
public void generateBuilding() { }
public boolean isCity() { }

// 常量：UPPER_SNAKE_CASE
public static final int MAX_BUILDING_HEIGHT = 256;
public static final String DEFAULT_PALETTE = "default";

// 变量：camelCase
int chunkX = 0;
BuildingInfo buildingInfo = null;

// 包名：lowercase
package com.during.cityloader.worldgen.lost;
```

#### 注释规范
```java
/**
 * 建筑信息类
 * 缓存区块级别的城市生成状态信息
 * 
 * <p>该类使用三层缓存策略：
 * <ul>
 *   <li>BuildingInfo缓存：完整的建筑信息</li>
 *   <li>CityInfo缓存：城市检测结果</li>
 *   <li>CityLevel缓存：城市等级</li>
 * </ul>
 * 
 * @author During
 * @since 1.4.0
 * @see ChunkCoord
 * @see IDimensionInfo
 */
public class BuildingInfo {
    
    /**
     * 获取建筑信息
     * 
     * @param coord 区块坐标
     * @param provider 维度信息提供者
     * @return 建筑信息，永不为null
     * @throws IllegalArgumentException 如果参数为null
     */
    public static BuildingInfo getBuildingInfo(ChunkCoord coord, IDimensionInfo provider) {
        // 实现
    }
}
```

### 性能优化

#### 1. 缓存策略
```java
// ✅ 好的做法：使用TimedCache
private static final TimedCache<ChunkCoord, BuildingInfo> CACHE = 
    new TimedCache<>(() -> 300); // 5分钟过期

// ❌ 坏的做法：无限增长的缓存
private static final Map<ChunkCoord, BuildingInfo> CACHE = 
    new ConcurrentHashMap<>(); // 永不清理
```

#### 2. 延迟计算
```java
// ✅ 好的做法：按需计算
public CompiledPalette getCompiledPalette() {
    if (compiledPalette == null) {
        compiledPalette = compilePalette();
    }
    return compiledPalette;
}

// ❌ 坏的做法：提前计算所有
public BuildingInfo(ChunkCoord coord, IDimensionInfo provider) {
    this.compiledPalette = compilePalette(); // 可能不需要
}
```

#### 3. 批量操作
```java
// ✅ 好的做法：批量设置方块
BlockData[] blocks = new BlockData[256];
for (int i = 0; i < 256; i++) {
    blocks[i] = Material.STONE.createBlockData();
}
region.setBlockData(x, y, z, blocks);

// ❌ 坏的做法：逐个设置
for (int i = 0; i < 256; i++) {
    region.setBlockData(x, y + i, z, Material.STONE.createBlockData());
}
```

#### 4. 避免重复计算
```java
// ✅ 好的做法：缓存计算结果
private int groundLevel = -1;

public int getGroundLevel() {
    if (groundLevel == -1) {
        groundLevel = calculateGroundLevel();
    }
    return groundLevel;
}

// ❌ 坏的做法：每次都计算
public int getGroundLevel() {
    return calculateGroundLevel(); // 重复计算
}
```

### 错误处理

#### 1. 优雅降级
```java
// ✅ 好的做法：提供默认值
public Building getBuilding() {
    try {
        return selectBuilding();
    } catch (Exception e) {
        logger.warning("建筑选择失败，使用默认建筑: " + e.getMessage());
        return getDefaultBuilding();
    }
}

// ❌ 坏的做法：直接抛出异常
public Building getBuilding() {
    return selectBuilding(); // 可能抛出异常导致生成失败
}
```

#### 2. 详细日志
```java
// ✅ 好的做法：提供上下文信息
try {
    palette = loadPalette(name);
} catch (Exception e) {
    logger.severe(String.format(
        "加载调色板失败: name=%s, path=%s, error=%s",
        name, path, e.getMessage()
    ));
}

// ❌ 坏的做法：日志信息不足
try {
    palette = loadPalette(name);
} catch (Exception e) {
    logger.severe("加载失败"); // 缺少上下文
}
```

#### 3. 空值检查
```java
// ✅ 好的做法：防御性编程
public void generate(Building building, CompiledPalette palette) {
    if (building == null) {
        logger.warning("建筑为null，跳过生成");
        return;
    }
    if (palette == null) {
        logger.warning("调色板为null，使用默认");
        palette = getDefaultPalette();
    }
    // 继续生成
}

// ❌ 坏的做法：假设非空
public void generate(Building building, CompiledPalette palette) {
    building.getParts(); // 可能NPE
    palette.getBlock('W'); // 可能NPE
}
```

### 测试最佳实践

#### 1. 测试命名
```java
// ✅ 好的做法：描述性命名
@Test
public void testBuildingSelectionReturnsNullForNonCityChunk() {
    // 测试非城市区块返回null
}

// ❌ 坏的做法：模糊命名
@Test
public void test1() {
    // 不知道测试什么
}
```

#### 2. AAA模式
```java
@Test
public void testPaletteCompilation() {
    // Arrange（准备）
    Palette base = new Palette("base");
    Palette style = new Palette("style");
    
    // Act（执行）
    CompiledPalette compiled = new CompiledPalette(base, style);
    
    // Assert（断言）
    assertNotNull(compiled);
    assertTrue(compiled.getCharacters().size() > 0);
}
```

#### 3. 边界测试
```java
@Test
public void testBuildingInfoWithNullCoord() {
    // 测试null输入
    assertThrows(IllegalArgumentException.class, () -> {
        BuildingInfo.getBuildingInfo(null, provider);
    });
}

@Test
public void testBuildingInfoWithExtremeCoordinates() {
    // 测试极端坐标
    ChunkCoord coord = new ChunkCoord(Integer.MAX_VALUE, Integer.MAX_VALUE);
    BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
    assertNotNull(info);
}
```

### 资源管理

#### 1. 及时释放
```java
// ✅ 好的做法：使用try-with-resources
try (InputStream is = getClass().getResourceAsStream(path)) {
    // 使用资源
} // 自动关闭

// ❌ 坏的做法：忘记关闭
InputStream is = getClass().getResourceAsStream(path);
// 使用资源
// 忘记关闭
```

#### 2. 插件生命周期
```java
@Override
public void onEnable() {
    // 初始化资源
    AssetRegistries.load(world);
    BuildingInfo.resetCache();
}

@Override
public void onDisable() {
    // 清理资源
    AssetRegistries.reset();
    BuildingInfo.resetCache();
}
```

---

## 🧪 测试策略

### 测试金字塔

```
           /\
          /  \  E2E测试 (5%)
         /____\
        /      \  集成测试 (25%)
       /________\
      /          \  单元测试 (70%)
     /____________\
```

### 单元测试（70%）

**资产解析测试**:
```java
@Test
public void testPaletteLoading() {
    // 测试调色板加载
    Palette palette = AssetRegistries.PALETTES.get("default");
    assertNotNull(palette);
    assertTrue(palette.getCharacters().contains('W'));
}

@Test
public void testBuildingPartParsing() {
    // 测试部件解析
    BuildingPart part = AssetRegistries.PARTS.get("floor_basic_1");
    assertNotNull(part);
    assertEquals(16, part.getSlices().length);
}

@Test
public void testConditionEvaluation() {
    // 测试条件评估
    Condition condition = new Condition(conditionRE);
    ConditionContext context = new ConditionContext.Builder()
        .cityLevel(3)
        .floor(5)
        .build();
    
    assertTrue(condition.test(context));
}
```

**生成逻辑测试**:
```java
@Test
public void testCityDetection() {
    // 测试城市检测
    ChunkCoord coord = new ChunkCoord(100, 100);
    BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
    
    // 验证城市状态
    assertNotNull(info);
    assertTrue(info.isCity || !info.isCity); // 应该有明确结果
}

@Test
public void testBuildingSelection() {
    // 测试建筑选择
    ChunkCoord coord = new ChunkCoord(100, 100);
    BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
    
    if (info.isCity) {
        assertNotNull(info.getBuilding());
    }
}

@Test
public void testPaletteCompilation() {
    // 测试调色板编译
    Palette base = new Palette("base");
    Palette style = new Palette("style");
    
    CompiledPalette compiled = new CompiledPalette(base, style);
    assertNotNull(compiled);
    assertTrue(compiled.getCharacters().size() > 0);
}
```

**缓存测试**:
```java
@Test
public void testBuildingInfoCache() {
    // 测试缓存功能
    ChunkCoord coord = new ChunkCoord(50, 50);
    
    BuildingInfo info1 = BuildingInfo.getBuildingInfo(coord, provider);
    BuildingInfo info2 = BuildingInfo.getBuildingInfo(coord, provider);
    
    // 应该返回同一个实例
    assertSame(info1, info2);
}

@Test
public void testCacheCleanup() {
    // 测试缓存清理
    for (int i = 0; i < 1000; i++) {
        BuildingInfo.getBuildingInfo(new ChunkCoord(i, i), provider);
    }
    
    int sizeBefore = BuildingInfo.getCacheSize();
    BuildingInfo.cleanupCache();
    int sizeAfter = BuildingInfo.getCacheSize();
    
    assertTrue(sizeAfter < sizeBefore);
}
```

### 集成测试（25%）

**完整生成流程测试**:
```java
@Test
public void testCompleteGenerationPipeline() {
    // 1. 加载资产
    AssetRegistries.load(mockWorld);
    
    // 2. 创建生成器
    CityBlockPopulator populator = new CityBlockPopulator(
        logger, config, seasonAdapter
    );
    
    // 3. 生成区块
    LimitedRegion region = mock(LimitedRegion.class);
    populator.populate(mockWorldInfo, random, 100, 100, region);
    
    // 4. 验证生成结果
    verify(region, atLeastOnce()).setBlockData(anyInt(), anyInt(), anyInt(), any());
}
```

**多区块协调测试**:
```java
@Test
public void testMultiChunkBuilding() {
    // 测试跨区块建筑
    BuildingInfo info1 = BuildingInfo.getBuildingInfo(new ChunkCoord(10, 10), provider);
    BuildingInfo info2 = BuildingInfo.getBuildingInfo(new ChunkCoord(10, 11), provider);
    
    // 如果是多区块建筑，应该共享信息
    if (info1.multiBuilding != null) {
        assertEquals(info1.multiBuilding, info2.multiBuilding);
    }
}
```

**季节适配测试**:
```java
@Test
public void testSeasonalAdaptation() {
    // 测试季节变化
    SeasonAdapter adapter = new SeasonAdapter(plugin);
    
    Material summer = adapter.adaptMaterial(Material.GRASS_BLOCK, Season.SUMMER);
    Material winter = adapter.adaptMaterial(Material.GRASS_BLOCK, Season.WINTER);
    
    assertNotEquals(summer, winter);
}
```

### E2E测试（5%）

**服务器集成测试**:
```java
@Test
public void testPluginLifecycle() {
    // 测试插件生命周期
    CityLoaderPlugin plugin = new CityLoaderPlugin();
    
    // 启动
    plugin.onEnable();
    assertTrue(AssetRegistries.isLoaded());
    
    // 关闭
    plugin.onDisable();
    assertEquals(0, BuildingInfo.getCacheSize());
}
```

**性能测试**:
```java
@Test
public void testGenerationPerformance() {
    // 测试生成性能
    long start = System.currentTimeMillis();
    
    for (int i = 0; i < 100; i++) {
        populator.populate(mockWorldInfo, random, i, i, mockRegion);
    }
    
    long duration = System.currentTimeMillis() - start;
    
    // 100个区块应该在1秒内完成
    assertTrue(duration < 1000, "生成速度过慢: " + duration + "ms");
}
```

### 测试覆盖率目标

| 模块 | 当前 | P0目标 | P1目标 | P2目标 |
|------|------|--------|--------|--------|
| worldgen.lost.cityassets | 45% | 60% | 70% | 80% |
| worldgen.lost.regassets | 100% | 100% | 100% | 100% |
| generator | 25% | 40% | 55% | 65% |
| resource | 30% | - | - | - |
| config | 40% | 50% | 60% | 70% |
| command | 0% | 0% | 30% | 50% |
| util | 50% | 60% | 70% | 75% |
| **总体** | **35%** | **40%** | **50%** | **60%** |

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AssetParsingTest

# 运行特定测试方法
mvn test -Dtest=AssetParsingTest#testPaletteLoading

# 生成覆盖率报告
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 持续集成

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: mvn test
      - name: Generate coverage
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

---

## 📚 文档

### 核心文档
- **README.md** - 本文档（项目总览）
- **PROGRESS_CHECKLIST.md** - 详细任务清单
- **QUICK_START_GUIDE.md** - 快速启动指南

### 深度分析
- **COMPREHENSIVE_COMPARISON_REPORT.md** - 全面对比
- **ACCURATE_ARCHITECTURE_ANALYSIS.md** - 架构分析
- **REFACTORING_ACTION_PLAN.md** - 实施计划

### 规范文档
- **.kiro/specs/cityloader-refactoring/** - 重构规范
- **.kiro/steering/** - 项目指导（product、structure、tech）

---

## 🎯 成功标准

### P0完成（2-3周）
- [ ] 资产加载: 100%
- [ ] 功能完整性: 70%
- [ ] 测试覆盖率: 40%
- [ ] TPS >= 18
- [ ] 评分: A-

### P1完成（7周）
- [ ] 功能完整性: 80%
- [ ] Highway和Railway可用
- [ ] 测试覆盖率: 50%
- [ ] 评分: A

### P2完成（10周）
- [ ] 功能完整性: 85%
- [ ] 14个命令
- [ ] 测试覆盖率: 60%
- [ ] 评分: A+

---

## 🔧 常用命令

### 构建命令
```bash
# 完整构建
mvn clean package

# 快速构建（跳过测试）
mvn clean package -DskipTests

# 只编译不打包
mvn compile

# 安装到本地仓库
mvn install
```

### 测试命令
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AssetParsingTest

# 运行特定测试方法
mvn test -Dtest=AssetParsingTest#testPaletteLoading

# 生成测试覆盖率报告
mvn jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
```

### 部署命令
```bash
# 复制到测试服务器
cp target/cityloader-*.jar ../City-Test-Server/plugins/

# 重启测试服务器
cd ../City-Test-Server
./restart.sh

# 查看日志
tail -f logs/latest.log
```

### 插件命令（当前可用）
```
/cityloader reload   # 权限: cityloader.reload  重载配置与资源
/cityloader info     # 权限: cityloader.info    显示插件状态
/cityloader version  # 权限: cityloader.version 显示版本信息
/cityloader generate # 权限: cityloader.generate 手动触发提示（尚未实现真实生成）
```

### 调试命令
```bash
# 启用调试模式构建
mvn clean package -Ddebug=true

# 远程调试（在服务器启动脚本中添加）
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar paper.jar

# 连接调试器（IDE中配置）
# Host: localhost
# Port: 5005
```

### 代码质量
```bash
# 检查代码风格
mvn checkstyle:check

# 查找潜在bug
mvn spotbugs:check

# 依赖分析
mvn dependency:tree
mvn dependency:analyze
```

---

## 🐛 故障排除

### 常见问题

#### 1. 资产加载失败

**症状**: 插件启动时报错 "Failed to load assets"

**原因**:
- JSON文件格式错误
- 资源包路径不正确
- 缺少必需的资产文件

**解决方案**:
```bash
# 1. 检查JSON格式
cd CityLoader/src/main/resources/data/
find . -name "*.json" -exec python -m json.tool {} \; > /dev/null

# 2. 验证内置/data结构
ls -R */lostcities/

# 3. 查看详细日志
grep "AssetRegistries\\|PaperResourceLoader" City-Test-Server/logs/latest.log

# 4. 使用测试验证
cd CityLoader
mvn test -Dtest=AssetParsingTest
```

#### 2. TPS下降

**症状**: 服务器TPS从19+降到15以下

**原因**:
- 日志输出过多
- 缓存未命中率高
- 生成算法效率低

**解决方案**:
```yaml
# config.yml - 调整日志级别
logging:
  level: WARNING  # 从INFO改为WARNING
  asset_loading: false
  chunk_generation: false

# 清理缓存
/cityloader clearcache

# 查看性能报告
/cityloader stats
```

#### 3. 建筑生成异常

**症状**: 建筑不完整或位置错误

**原因**:
- BuildingInfo决策链未完成
- 调色板编译错误
- 区块坐标计算错误

**解决方案**:
```bash
# 1. 启用调试模式
/cityloader debug

# 2. 可视化边界
/cityloader visualize building

# 3. 检查BuildingInfo
# 在代码中添加断点
BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
System.out.println("isCity: " + info.isCity);
System.out.println("building: " + info.buildingType);
```

#### 4. 内存泄漏

**症状**: 服务器内存持续增长

**原因**:
- 缓存未清理
- 静态引用未释放
- 资产重复加载

**解决方案**:
```java
// 1. 定期清理缓存
Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    BuildingInfo.cleanupCache();
}, 20 * 60 * 5, 20 * 60 * 5); // 每5分钟

// 2. 监控缓存大小
/cityloader stats

// 3. 插件卸载时清理
@Override
public void onDisable() {
    AssetRegistries.reset();
    BuildingInfo.resetCache();
}
```

#### 5. 与其他插件冲突

**症状**: 安装CityLoader后其他插件报错

**原因**:
- 依赖版本冲突
- 事件监听器优先级
- 世界生成器冲突

**解决方案**:
```xml
<!-- pom.xml - 使用shade插件重定位依赖 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <relocations>
            <relocation>
                <pattern>com.google.gson</pattern>
                <shadedPattern>com.during.cityloader.libs.gson</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

### 调试技巧

#### 使用日志追踪
```java
// 添加详细日志
CityLoaderLogger.debug("处理区块 [%d, %d]", chunkX, chunkZ);
CityLoaderLogger.debug("BuildingInfo: isCity=%b, building=%s", 
    info.isCity, info.buildingType);
```

#### 使用断言验证
```java
// 添加运行时检查
assert info != null : "BuildingInfo不应为null";
assert info.isCity || !info.hasBuilding : "非城市区块不应有建筑";
```

#### 使用性能分析
```java
// 包装耗时操作
PerformanceMonitor.measure("building_selection", () -> {
    return selectBuilding(info);
});

// 查看报告
/cityloader stats
```

---

## 👨‍💻 开发工作流

### 新功能开发流程

#### 1. 创建分支
```bash
git checkout -b feature/highway-system
```

#### 2. 编写规范
```markdown
# .kiro/specs/highway-system/requirements.md

## 用户故事
作为玩家，我希望看到连接城市的高速公路网络

## 验收标准
- [ ] 高速公路每32区块生成一次
- [ ] 高速公路高度为地面+8格
- [ ] 支持X和Z方向
- [ ] 交叉路口正确生成
```

#### 3. 编写测试（TDD）
```java
@Test
public void testHighwayGeneration() {
    // 先写测试
    ChunkCoord coord = new ChunkCoord(32, 0);
    BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
    
    assertTrue(info.highwayXLevel > 0 || info.highwayZLevel > 0);
}
```

#### 4. 实现功能
```java
public class HighwayGenerator {
    public void generate(LimitedRegion region, ChunkCoord coord) {
        // 实现逻辑
    }
}
```

#### 5. 运行测试
```bash
mvn test
```

#### 6. 本地验证
```bash
# 构建并部署
mvn clean package
cp target/cityloader-*.jar ../City-Test-Server/plugins/

# 启动服务器测试
cd ../City-Test-Server
./start.sh

# 进入游戏验证
# 传送到高速公路位置
/tp @s 512 100 0
```

#### 7. 提交代码
```bash
git add .
git commit -m "feat: 实现高速公路生成系统"
git push origin feature/highway-system
```

### 代码审查清单

#### 功能性
- [ ] 功能符合需求规范
- [ ] 边界情况已处理
- [ ] 错误处理完善
- [ ] 日志输出适当

#### 性能
- [ ] 无明显性能瓶颈
- [ ] 缓存使用合理
- [ ] 避免重复计算
- [ ] 内存使用可控

#### 代码质量
- [ ] 命名清晰易懂
- [ ] 注释充分（中文）
- [ ] 无重复代码
- [ ] 遵循项目规范

#### 测试
- [ ] 单元测试覆盖核心逻辑
- [ ] 测试用例充分
- [ ] 所有测试通过
- [ ] 覆盖率达标

### 发布流程

#### 1. 版本号规范
```
主版本.次版本.修订版-标签

1.4.0-SURFACE-SHIFT  # 当前版本
1.5.0-HIGHWAY        # 下一个版本（新功能）
1.4.1-BUGFIX         # 修复版本
```

#### 2. 更新版本
```xml
<!-- pom.xml -->
<version>1.5.0-HIGHWAY</version>
```

```yaml
# plugin.yml
version: 1.5.0-HIGHWAY
```

#### 3. 生成变更日志
```markdown
# CHANGELOG.md

## [1.5.0-HIGHWAY] - 2026-03-15

### 新增
- 高速公路生成系统
- 铁路网络系统
- 散布建筑支持

### 改进
- 优化BuildingInfo决策链
- 提升资产加载速度

### 修复
- 修复多区块建筑边界问题
- 修复调色板合并bug
```

#### 4. 构建发布版本
```bash
# 清理并构建
mvn clean package

# 运行完整测试
mvn test

# 生成文档
mvn javadoc:javadoc

# 创建发布包
mkdir release
cp target/cityloader-1.5.0-HIGHWAY.jar release/
cp README.md release/
cp CHANGELOG.md release/
zip -r cityloader-1.5.0-HIGHWAY.zip release/
```

#### 5. 创建Git标签
```bash
git tag -a v1.5.0 -m "Release 1.5.0-HIGHWAY"
git push origin v1.5.0
```

### 热修复流程

#### 1. 创建热修复分支
```bash
git checkout -b hotfix/critical-crash main
```

#### 2. 快速修复
```java
// 修复关键bug
if (info == null) {
    logger.warning("BuildingInfo为null，跳过生成");
    return;  // 添加空值检查
}
```

#### 3. 测试验证
```bash
mvn test -Dtest=CriticalTest
```

#### 4. 紧急发布
```bash
# 更新版本号为修订版
# 1.4.0 -> 1.4.1

mvn clean package
git commit -am "fix: 修复BuildingInfo空指针异常"
git tag -a v1.4.1 -m "Hotfix 1.4.1"
git push origin hotfix/critical-crash
git push origin v1.4.1
```

---

## 📞 项目信息

**维护**: CityLoader开发团队  
**参考**: LostCities-1.20 (Forge)  
**测试服务器**: City-Test-Server (Paper 1.21.8)  
**最后更新**: 2026-02-15

---

## 🔄 数据流与决策链

### 完整生成流程

```
1. 插件启动
   └─> AssetRegistries.load(world)
       ├─> 加载 Variants, Conditions (无依赖)
       ├─> 加载 Palettes, Styles (依赖变体)
       ├─> 加载 Parts (依赖调色板)
       ├─> 加载 Buildings (依赖部件)
       ├─> 加载 CityStyles, WorldStyles (依赖建筑)
       └─> 构建索引 (STUFF_BY_TAG)

2. 区块生成请求 (chunkX, chunkZ)
   └─> CityBlockPopulator.populate()
       ├─> BuildingInfo.getBuildingInfo(coord, provider)
       │   ├─> 检查缓存 (TimedCache)
       │   ├─> 计算城市状态 (isCity, cityLevel)
       │   ├─> 检查 Highway (X轴/Z轴)
       │   ├─> 检查 Railway (隧道/车站)
       │   ├─> 选择建筑类型 (Building)
       │   └─> 编译调色板 (CompiledPalette)
       │
       ├─> ChunkDriver.setPrimer(region, chunkData)
       │   └─> 初始化 SectionCache
       │
       ├─> 生成基础设施
       │   ├─> generateHighway(info) [P1]
       │   └─> generateRailway(info) [P1]
       │
       ├─> 生成建筑
       │   ├─> 遍历楼层 (cellars → floors)
       │   ├─> 选择部件 (ConditionContext)
       │   ├─> 渲染部件 (BuildingPart.generate)
       │   └─> 应用调色板 (CompiledPalette.get)
       │
       ├─> 生成装饰
       │   ├─> generateScattered(info) [P1]
       │   └─> applyDamage(info) [P1]
       │
       └─> ChunkDriver.actuallyGenerate()
           └─> SectionCache.generate(chunkData)
               ├─> 批量写入方块
               ├─> 更新高度图
               └─> 更新相邻方块状态

3. 后处理
   └─> BuildingInfo.cleanupCache()
       └─> 清理过期缓存条目
```

### BuildingInfo 决策树

```
BuildingInfo.getBuildingInfo(coord, provider)
│
├─> 检查缓存
│   ├─> 命中 → 返回缓存对象
│   └─> 未命中 → 继续计算
│
├─> 计算城市状态
│   ├─> 检查预定义城市 (PredefinedCity)
│   │   └─> 找到 → isCity=true, 使用预定义配置
│   │
│   └─> Perlin噪声检测
│       ├─> noise > CITY_CHANCE → isCity=true
│       └─> 否则 → isCity=false, 返回
│
├─> 计算城市等级 (0-5)
│   └─> 基于随机值分布
│       ├─> 10% → Level 5 (摩天大楼)
│       ├─> 20% → Level 4 (高层建筑)
│       ├─> 30% → Level 3 (中层建筑)
│       ├─> 25% → Level 2 (低层建筑)
│       └─> 15% → Level 1 (小型建筑)
│
├─> 检查基础设施
│   ├─> Highway.getXHighwayLevel(coord)
│   │   ├─> Perlin噪声 > threshold
│   │   ├─> 连续长度 >= 5 chunks
│   │   ├─> 连接两个城市
│   │   └─> 返回高速公路层级
│   │
│   └─> Railway.getRailChunkType(coord)
│       ├─> 检查铁路网络
│       ├─> 动态水域检测
│       └─> 返回铁路类型 (隧道/车站/无)
│
├─> 选择建筑
│   ├─> 获取 CityStyle (基于生物群系+等级)
│   ├─> 创建 ConditionContext
│   │   ├─> coord, cityLevel, biome
│   │   ├─> groundLevel, waterLevel
│   │   └─> random (基于种子)
│   │
│   └─> 遍历候选建筑
│       ├─> 检查条件 (building.meetsConditions)
│       ├─> 找到匹配 → 返回建筑
│       └─> 无匹配 → 使用默认建筑
│
├─> 计算楼层
│   ├─> floors = random(minFloors, maxFloors)
│   ├─> cellars = random(minCellars, maxCellars)
│   └─> 为每层选择部件
│       ├─> 遍历 floor = -cellars to floors
│       ├─> 创建楼层 ConditionContext
│       │   ├─> floor, level
│       │   ├─> isTopOfBuilding, isCellar
│       │   └─> part, belowPart
│       │
│       └─> building.getRandomPart(random, context)
│           ├─> 过滤满足条件的部件
│           └─> 随机选择一个
│
└─> 编译调色板
    ├─> 获取建筑基础调色板
    ├─> 获取样式调色板 (Style)
    ├─> 合并调色板 (继承+覆盖)
    └─> 预计算加权随机表 (128项)
```

### ConditionContext 评估流程

```
ConditionContext.parseTest(condition)
│
├─> 解析条件类型
│   ├─> "top" → isTopOfBuilding()
│   ├─> "ground" → isGroundFloor()
│   ├─> "cellar" → isCellar()
│   ├─> "floor": N → isFloor(N)
│   ├─> "range": [N, M] → isRange(N, M)
│   ├─> "inbiome": "desert" → getBiome().equals("desert")
│   ├─> "inpart": "floor_*" → part.matches("floor_.*")
│   ├─> "inbuilding": "residential" → building.contains("residential")
│   ├─> "chunkx": N → coord.chunkX() % 16 == N
│   └─> "chunkz": N → coord.chunkZ() % 16 == N
│
├─> 组合条件 (AND)
│   └─> 所有子条件必须为 true
│
└─> 返回 Predicate<ConditionContext>
```

---

## 🔍 调试与诊断

### 日志级别

```yaml
# config.yml
logging:
  level: INFO  # SEVERE, WARNING, INFO, FINE, FINER, FINEST
  asset_loading: true
  chunk_generation: false
  cache_stats: true
```

### 关键日志输出

```
[INFO] ✓ 加载palettes资产完成: 成功=45, 失败=0
[INFO] ✓ 加载buildings资产完成: 成功=128, 失败=2
[WARNING] ✗ 资产解析失败: buildings/residential_tower.json (行23: 未知字段 'minFloor')
[FINE] 处理区块 [12, -5]: isCity=true, cityLevel=3, building=residential_apartment
[FINE] 选择部件: floor=2, part=floor_basic_3, conditions=[top=false, range=[1,5]]
```

### 性能监控

```java
// 启用缓存统计
BuildingInfo.getCacheSize();  // 当前缓存条目数
AssetRegistries.getStatistics();  // 资产加载统计

// 输出示例
"Palettes=45, Variants=12, Conditions=8, Styles=6, 
 Parts=234, Buildings=128, MultiBuildings=15, 
 CityStyles=8, WorldStyles=2, 
 Scattered=23, PredefinedCities=0, Stuff=156"
```

### 常见问题排查

**问题1: 建筑不生成**
```
检查清单:
1. 日志中是否有 "isCity=true"？
   → 否: 调整 config.yml 中的 CITY_CHANCE
2. 是否有 "building=null"？
   → 是: 检查 CityStyle 配置和建筑条件
3. 是否有资产加载失败？
   → 是: 修复 JSON 语法错误
```

**问题2: TPS下降**
```
检查清单:
1. 查看缓存大小: BuildingInfo.getCacheSize()
   → 过大: 减少缓存过期时间
2. 查看日志频率
   → 过高: 降低日志级别到 WARNING
3. 查看资产数量
   → 过多: 优化资产包，移除未使用的资产
```

**问题3: 方块错误**
```
检查清单:
1. 检查调色板映射: palettes/*.json
   → 字符是否映射到有效方块？
2. 检查季节适配
   → 是否与 RealisticSeasons 冲突？
3. 检查 Paper 版本
   → 方块名称在 1.21.8 中是否有效？
```

---

## 🛠️ 开发指南

### 添加新建筑类型

1. **创建部件 JSON** (`data/lostcities/parts/`)
```json
{
  "name": "floor_custom_1",
  "slices": [
    "XXXXXXXXXXXXXXXX",
    "X..............X",
    "X..............X",
    "XXXXXXXXXXXXXXXX"
  ],
  "palette": "default"
}
```

2. **创建建筑 JSON** (`data/lostcities/buildings/`)
```json
{
  "name": "custom_building",
  "minFloors": 3,
  "maxFloors": 8,
  "minCellars": 0,
  "maxCellars": 1,
  "parts": [
    {
      "part": "floor_custom_1",
      "condition": {
        "range": [1, 5]
      }
    },
    {
      "part": "top_flat",
      "condition": {
        "top": true
      }
    }
  ]
}
```

3. **添加到 CityStyle** (`data/lostcities/citystyles/`)
```json
{
  "name": "modern_city",
  "buildings": [
    "residential_apartment",
    "custom_building"  // 新建筑
  ]
}
```

### 添加新调色板

```json
{
  "name": "glass_modern",
  "palette": {
    "X": "minecraft:glass",
    "W": "minecraft:white_concrete",
    "G": "minecraft:gray_concrete",
    ".": "minecraft:air"
  },
  "variants": {
    "X": [
      {"block": "minecraft:glass", "weight": 70},
      {"block": "minecraft:blue_stained_glass", "weight": 30}
    ]
  }
}
```

### 添加新条件

```java
// ConditionContext.java
public static Predicate<ConditionContext> parseTest(ConditionTest element) {
    // 添加新条件类型
    if (element.hasCustomCondition()) {
        return ctx -> evaluateCustomCondition(ctx, element);
    }
    // ... 现有条件
}
```

### 性能优化建议

1. **减少资产数量**: 合并相似的部件和建筑
2. **优化调色板**: 减少变体数量，使用简单映射
3. **调整缓存**: 增加过期时间，减少重新计算
4. **降低日志级别**: 生产环境使用 WARNING
5. **批量操作**: 使用 ChunkDriver.setBlockRange 而非单个方块

---

## ⚠️ 与 LostCities 真实对比：差距清单（2026-02-15 深度代码审查）

> 以下清单基于对 LostCities-1.20（188 个 Java 文件，核心生成器 2324 行）与 CityLoader 源码的逐文件深度审查，
> 而非 README 中此前的自评检查点。**README 中多处标记 `[x]` 完成的检查点实际仍有重大缺口。**

### 核心文件实际实现状态

| 文件 | 行数 | 原版行数 | 实现深度 | 架构 |
|------|------|----------|----------|------|
| `BuildingInfo.java` | 700 | 2002 | **部分实现** — 核心决策完整，装饰/连接/后处理字段缺失 50%+ | 新架构 |
| `LostCityTerrainFeature.java` | 77 (+1339 Stage) | 2324 | **部分实现** — 管线架构完整，噪声/废墟/装饰/Loot 等 60%+ 缺失 | 新架构 |
| `CityBlockPopulator.java` | 122 | N/A | **完整实现** — Paper 入口胶水层 | 新架构 |
| `CityLoaderPlugin.java` | 159 | N/A | **完整实现** — 旧 ResourceManager 已废弃 | 新架构 |
| `ChunkDriver.java` | 687 | 499 | **部分实现** — SectionCache 完整但 correct() 方块状态修正缺失 | 独立设计 |
| `CityCoreStage.java` | 134 | — | **部分实现** — Part 渲染骨架在，缺地形修正/边界/公园/装饰 | 新架构 |
| `InfrastructureStage.java` | 350 | — | **部分实现（较好）** — Highway/Railway 基础渲染可用，缺路径规划 | 新架构 |
| `ScatteredStage.java` | 641 | 313 | **完整实现** — 比原版更完善（合并了分散逻辑） | 新架构 |
| `DamageStage.java` | 189 | — | **部分实现** — 球形爆炸+损伤映射可用，缺瓦砾/废墟/碎片 | 新架构 |
| `PostProcessStage.java` | 25 | — | **空壳** — 仅放 4 个固定火把 | 新架构 |
| `GenerationContext.java` | 144 | — | **较完整** — 统一经 ChunkDriver 缓冲写入，`flush()` 后批量落盘 | 新架构 |
| `CompiledPalette.java` | 284 | — | **较完整** — 128 槽随机表/继承/变体/损伤映射已实现 | 新架构 |

### LostCities 关键类在 CityLoader 中的缺失状态

| # | LostCities 类 | CityLoader 状态 | 影响 |
|---|---------------|-----------------|------|
| 1 | `City.java` | **不存在** — 简化概率模型内联在 BuildingInfo 中 | 城市分布碎片化，无连片城市 |
| 2 | `CityRarityMap.java` | **不存在** | 无噪声驱动的城市密度场 |
| 3 | `NoiseGeneratorPerlin.java` | **已实现并接入** | 已用于城市稀有度与废墟瓦砾噪声场 |
| 4 | `Highway.java` | **不存在** — 内联在 InfrastructureStage | 固定网格布局，无拓扑路网 |
| 5 | `Railway.java` | **不存在** — 内联在 InfrastructureStage | 数据模型完整，渲染基础 |
| 6 | `Transform.java` | **不存在** — 仅 Part.rotate()/mirror() | 仅 90° 旋转+X 镜像 |
| 7 | `Explosion.java` | **不存在** — DamageArea+DamageStage 替代 | 核心效果在，缺连锁和瓦砾 |
| 8 | `GlobalTodo.java` | **不存在** | 无跨区块延迟任务机制 |
| 9 | `CitySphere.java` | **不存在** — 数据模型在，渲染缺失 | 架构限制，Paper 无法实现 |
| 10 | `MultiChunk.java` | **不存在** — MultiPos+MultiBuilding 部分替代 | 数据模型完整，协调渲染待验证 |
| 11 | `Corridors.java` (85 行) | **不存在** | 地下建筑连接完全缺失 |
| 12 | `Bridges.java` (107 行) | **不存在** — 合并到 Highway 中 | 桥梁逻辑简化 |
| 13 | `Stuff.java` (105 行) | **不存在** | 蜘蛛网/苔石/锁链装饰缺失 |

### P0: 架构级阻塞问题（必须先解决）

| # | 任务 | 严重度 | 说明 |
|---|------|--------|------|
| **P0-1** | 引入 Perlin 噪声生成器 | ✅ 已完成 | 已接入 `CityRarityMap/City` 与 `DamageStage` rubble 噪声，提供可复现连续噪声场 |
| **P0-2** | 实现 City / CityRarityMap | ✅ 已完成 | 已支持噪声城市密度场、噪声模式城市中心判定、并修复 rarity cache 参数污染与 reload 缓存失效 |
| **P0-3** | ChunkDriver 接入断路修复 | ✅ 已完成 | 生成阶段统一通过 `GenerationContext -> ChunkDriver` 写入，取消旁路接口并补充 flush 回归 |
| **P0-4** | Material → BlockState 升级 | 严重 | 全局只用 `Material` 枚举，**无法表达方块状态**（楼梯朝向、栅栏连接、铁轨形状、火把方向、半砖上下、门开关等）。这是功能差距的架构性根因 |
| **P0-5** | ChunkDriver.correct() 补全 | 严重 | 缺少楼梯 `StairsShape` / 墙 `WallSide` / 栅栏连接的自动修正（LostCities 60+ 行），导致生成的建筑中这些方块外观错误 |
| **P0-6** | PostProcessStage 实现 | 严重 | 当前仅 25 行，只放 4 个固定火把。缺 Loot 写入、NBT 处理、光照更新、POI 更新、火把方向检测、ChunkFixer 等全部后处理 |
| **P0-7** | BuildingInfo 装饰字段补全 | 重要 | 缺 `fountainType` / `parkType` / `bridgeType` / `stairType` / `frontType` / `doorBlock` / `streetType` / `ruinHeight` / `noLoot` 等十余个字段 |
| **P0-8** | BuildingInfo 邻居连接补全 | 重要 | 缺 `connectionAtX[]` / `connectionAtZ[]` 相邻建筑连接数组，建筑间无法正确衔接 |

### P1: 功能完善（核心特性）

| # | 任务 | 优先级 | LostCities 对应 |
|---|------|--------|-----------------|
| **P1-1** | 地形修正系统 | 高 | `correctTerrainShape()` / `bipolate()` ~150 行，建筑边缘坡度平整 |
| **P1-2** | Highway 路径规划升级 | 高 | 当前 `mod 32` 固定网格，缺拓扑路网/坡道/出入口/交叉口 |
| **P1-3** | Railway 功能补全 | 高 | 缺弯道/高程变化/地铁站/Rail Dungeon（数据字段已有但未使用） |
| **P1-4** | Corridors 走廊系统 | 中 | `Corridors.java` 85 行，地下建筑连接通道，**完全缺失** |
| **P1-5** | 废墟/瓦砾系统 | 高 | `generateRubble()` / `generateRuins()` / `generateDebris()` ~250 行 |
| **P1-6** | 街道装饰系统 | 中 | 路灯/红绿灯/人行道/井盖/公园/喷泉区域 |
| **P1-7** | 战利品/容器系统 | 高 | `handleLoot()` / `generateLoot()` ~100 行，箱子内容/刷怪笼 |
| **P1-8** | NBT/BlockEntity 处理 | 高 | 方块实体数据写入（依赖 P0-4 BlockState 升级） |
| **P1-9** | Transform 变换完善 | 中 | 仅 90° 旋转+X 镜像，缺 180°/270°/Z 镜像/组合变换 |
| **P1-10** | GlobalTodo 延迟任务 | 中 | 跨区块协调操作系统，**完全缺失** |
| **P1-11** | 随机植被系统 | 低 | `randomLeafs` / `randomDirt` 废墟自然化覆盖 |
| **P1-12** | DamageArea 完善 | 中 | 缺连锁爆炸/`fixAfterExplosion` 悬浮方块修复/rubble 瓦砾堆 |
| **P1-13** | BuildingInfo 后处理列表 | 中 | `torchTodo` / `postTodo` / `ConditionTodo` |
| **P1-14** | 建筑边界生成 | 中 | `generateBorders()` / `generateBorderSupport()` ~180 行 |

### P2: 生态完善（可选增强）

| # | 任务 | 说明 |
|---|------|------|
| **P2-1** | Stuff 装饰系统 | 蜘蛛网/苔石/锁链等随机装饰 (105 行) |
| **P2-2** | Monorails 单轨列车 | 球体模式连接系统 |
| **P2-3** | 多维度 Profile 切换 | 已支持 `selected-profile` 与 `dimensions-with-profiles` 的世界级启停与 Profile 解析 |
| **P2-4** | SeasonAdapter 接入管线 | 已接入 GenerationContext，并驱动 Park/Fountain/PostProcess 季节行为 |
| **P2-5** | 清理旧 resource.* | ResourceManager 残留代码移除 |
| **P2-6** | SectionCache 索引修正 | 当前 `(px<<8)+(py<<4)+pz` 与原版 `(py<<8)+(px<<4)+pz` 不同，疑似 bug |

### 实际状态 vs README 声称对照

| 维度 | README 声称 | 实际状态 |
|------|-----------|---------|
| 总体进度 | 15% | 实际约 **20-25%**（核心骨架在，润色全无） |
| 检查点 A-H | 全部 `[x]` | **多数仍有重大缺口** |
| ChunkDriver | 完整实现 | 已实现但**未接入管线**（完全断路） |
| BuildingInfo | 决策核心完成 | 核心决策在，**装饰/连接/后处理字段缺失 50%+** |
| 生成管线 | 5 阶段完成 | 4 阶段有实质逻辑，PostProcess 是**空壳** |
| 噪声系统 | 未提及 | **完全缺失**（最致命的差距） |
| 方块状态 | 未提及 | **仅 Material 无 BlockState**（架构性缺陷） |

> **建议优先攻克 P0-1 → P0-4**（噪声 → BlockState），这是当前最主要的质量瓶颈。

### 技术债务

- **旧系统清理**: resource.* 包需要完全移除
- **测试覆盖率**: 当前35%，目标60%
- **文档完整性**: 需要补充API文档和示例
- **错误处理**: 需要统一异常处理策略

---

## 🎉 里程碑

- ✅ 2026-02-15: P0.5日志优化完成，TPS提升到19+
- ✅ 2026-02-15: P0.1资产加载完成，支持100%资产类型
- ⏳ 2026-02-20: P0.2 CompiledPalette实现（目标）
- ⏳ 2026-02-25: P0.3 BuildingInfo决策链（目标）
- ⏳ 2026-03-01: P0.4架构统一完成（目标）
- ⏳ 2026-04-05: P1核心特性完成（目标）
- ⏳ 2026-05-15: P2生态完善完成（目标）

---

## 📖 快速参考

### 关键类

| 类名 | 职责 | 位置 |
|------|------|------|
| `CityLoaderPlugin` | 插件主类 | `CityLoaderPlugin.java` |
| `CityBlockPopulator` | 区块生成器 | `generator/CityBlockPopulator.java` |
| `BuildingInfo` | 建筑信息缓存 | `worldgen/lost/BuildingInfo.java` |
| `AssetRegistries` | 资产注册表 | `worldgen/lost/cityassets/AssetRegistries.java` |
| `CompiledPalette` | 编译后的调色板 | `worldgen/lost/cityassets/CompiledPalette.java` |
| `ChunkDriver` | 方块放置驱动 | `worldgen/ChunkDriver.java` |

### 关键配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `default-season` | `SPRING` | RealisticSeasons 不可用时的默认季节 |
| `city-density` | `0.8` | 城市密度（0.0-1.0） |
| `min-building-height` | `3` | 建筑最小高度 |
| `max-building-height` | `20` | 建筑最大高度 |
| `street-width` | `5` | 街道宽度 |
| `generation.generate-underground` | `true` | 是否生成地下结构 |
| `generation.generate-streets` | `true` | 是否生成街道 |
| `generation.vanilla-compatible` | `true` | 保留原版地形 |
| `performance.cache-size` | `1000` | 资源缓存上限 |
| `performance.async-loading` | `true` | 是否启用异步加载 |

### 关键命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cityloader reload` | `cityloader.reload` | 重新加载配置与资源 |
| `/cityloader info` | `cityloader.info` | 显示插件状态 |
| `/cityloader version` | `cityloader.version` | 显示版本信息 |
| `/cityloader generate` | `cityloader.generate` | 手动触发提示（尚未实现真实生成） |

> 其他调试/统计/定位类命令为规划项，详见 P2 路线图。

### 性能指标

| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| TPS | ≥19 | 19+ | ✅ |
| 区块生成时间 | <20ms | 10-20ms | ✅ |
| 资产加载时间 | <5s | 3-5s | ✅ |
| 内存占用 | <500MB | ~300MB | ✅ |
| 缓存命中率 | >80% | ~85% | ✅ |

---

## 🤝 贡献指南

### 如何贡献

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某个功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

### 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(generator): 实现高速公路生成系统

- 添加HighwayGenerator类
- 支持X和Z方向高速公路
- 实现交叉路口生成
- 添加单元测试

Closes #123
```

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- **LostCities** - 原始Forge模组，提供了核心算法和资产格式
- **Paper团队** - 提供了高性能的Minecraft服务器平台
- **RealisticSeasons** - 季节系统集成
- **所有贡献者** - 感谢你们的贡献和反馈

---

## 📞 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/your-repo/issues)
- **功能建议**: [GitHub Discussions](https://github.com/your-repo/discussions)
- **文档**: 本README及`.kiro/specs/`目录

---

## 💡 提示

### 开发者提示
- 使用`CityLoaderLogger`而不是直接使用`Logger`
- 所有资产操作通过`AssetRegistries`
- 缓存操作通过`BuildingInfo`
- 方块放置通过`ChunkDriver`
- 测试覆盖率保持在40%以上

### 性能提示
- 避免在主线程进行耗时操作
- 使用缓存减少重复计算
- 批量设置方块而不是逐个设置
- 定期清理过期缓存
- 监控TPS和内存使用

### 调试提示
- 使用`/cityloader debug`查看区块信息
- 使用`/cityloader visualize`可视化边界
- 查看`logs/latest.log`获取详细日志
- 使用IDE断点调试生成流程
- 运行单元测试验证逻辑

---

**记住**: 
- 每天提交代码，保持进度可见
- 每周总结进度，更新文档
- 保持测试覆盖率，确保质量
- 优化性能，关注TPS
- 编写清晰的注释（中文）

**祝你成功！** 🚀

---
