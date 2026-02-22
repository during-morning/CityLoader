# CityLoader

Paper 1.21+ 城市生成插件，目标是把 LostCities 风格迁移到 Bukkit/Paper 生态，并支持季节适配。

- 当前版本: `1.4.0-SURFACE-SHIFT`
- 主类: `com.during.cityloader.CityLoaderPlugin`
- 命令前缀: `/cityloader`（别名: `/cl`, `/city`）

## 1. 系统总览

CityLoader 的核心由以下系统组成：

- 生成入口系统: `CityBlockPopulator` + `LostCityTerrainFeature`
- 分阶段生成系统: `GenerationStage` 管线按顺序执行
- 资产系统: `AssetRegistries` 加载 `palettes/parts/buildings/styles`
- 配置系统: `ConfigManager` 读取 `plugins/CityLoader/config.yml`
- 维度/高度图系统: `PaperDimensionInfo` + `ChunkHeightmap`
- 季节系统: `SeasonAdapter`（可对接 `RealisticSeasons`）
- 命令系统: `CommandHandler` + 子命令实现
- 后处理系统: `PostProcessStage` + `ChunkFixer`

## 2. 生成阶段（Pipeline）

基础阶段（总是执行）:

1. `CityCoreStage`
2. `InfrastructureStage`
3. `CorridorStage`
4. `BridgeStage`
5. `PostProcessStage`

扩展阶段（`safeMode=false` 时执行）:

1. `ScatteredStage`
2. `CitySphereStage`
3. `MonorailStage`
4. `DamageStage`
5. `RailDungeonStage`
6. `ParkStage`
7. `FountainStage`
8. `MegaSolarStage`
9. `QuarryStage`
10. `OffshoreStage`
11. `LootStage`
12. `SpawnerStage`

运行参数:

- `-Dcityloader.safeMode=true|false` 控制是否仅运行核心阶段
- `-Dcityloader.maxChunkGenMs=<毫秒>` 控制单区块阶段预算（超时跳过后续阶段）

## 3. 已修复的地形问题（本次）

针对“地形无过渡、建筑与地形重合”做了三类修复：

- 建筑净空改为“边缘保留过渡带”，不再粗暴整块清空
- 街区地表修正提高可挖深度，避免道路/建筑底部被山体穿入
- 道路增加净空清理，保证路面上方不会残留原地形穿模

相关代码:

- `src/main/java/com/during/cityloader/worldgen/gen/CityCoreStage.java`

## 4. 命令与权限

### 4.1 主命令

- `/cityloader` 显示帮助
- `/cityloader reload` 重载配置和资产
- `/cityloader info` 显示插件状态、季节、资产统计
- `/cityloader inspect [chunkX chunkZ]` 输出当前世界区块决策摘要（玩家）
- `/cityloader inspect <world> <chunkX> <chunkZ>` 输出指定世界区块决策摘要（控制台/玩家）
- `/cityloader version` 显示版本与服务器信息
- `/cityloader generate [x] [y] [z]` 生成提示命令（当前主要用于引导，实际城市在新区块自动生成）

### 4.2 权限节点

- `cityloader.admin`
- `cityloader.reload`
- `cityloader.info`
- `cityloader.inspect`
- `cityloader.version`
- `cityloader.generate`

## 5. 安装与升级

1. 将构建产物放入 `plugins/`
2. 启动服务器
3. 首次启动会生成 `plugins/CityLoader/config.yml`
4. 资产目录为 `plugins/CityLoader/data/`

升级建议:

1. 备份旧配置与数据目录
2. 替换新 JAR
3. 启动后执行 `/cityloader reload`
4. 用 `/cityloader info` 检查资源加载计数

## 6. 构建、测试、发布命令

在项目根目录 `CityLoader/` 执行。

### 6.1 本地构建

```bash
mvn clean package -DskipTests
```

产物:

- `target/cityloader-1.4.0-SURFACE-SHIFT.jar`（可部署）
- `target/original-cityloader-1.4.0-SURFACE-SHIFT.jar`（未重定位原始产物）

### 6.2 测试

```bash
mvn test
```

仅跑单个测试类:

```bash
mvn -Dtest=CityCoreStageSurfaceEmbeddingTest test
```

### 6.3 自动发布脚本

```bash
./new.sh [tag] [commit_message]
```

脚本流程:

1. Maven 打包
2. 提交变更
3. 打 tag
4. 推送 `main` 和 tag
5. 创建 GitHub Release 并上传 JAR

示例:

```bash
./new.sh v1.4.0-surface-shift "release: terrain transition fix"
```

## 7. 配置要点

主配置文件: `plugins/CityLoader/config.yml`

重点配置块:

- `profiles.selected-profile`: 全局默认 profile
- `profiles.dimensions-with-profiles`: 按维度绑定 profile
- `profiles.definitions`: profile 定义（城市密度、高度、地形修正、战利品、桥梁等）
- `resource-packs`: 外部资产 overlay 目录（按列表顺序叠加，后者覆盖前者）

建议优先调的地形参数（profile 内）:

- `terrain-fix-lower-min-offset`
- `terrain-fix-lower-max-offset`
- `terrain-fix-upper-min-offset`
- `terrain-fix-upper-max-offset`

## 8. 常见问题

### 8.1 编译报错 `Material.GRASS` 不存在

原因: 新版 Bukkit/Paper 不保证保留 `Material.GRASS` 常量。

处理: 不直接引用 `Material.GRASS`，改为名称兼容判断（`"GRASS".equals(material.name())`）。

### 8.2 城市不生成

按顺序排查:

1. 世界是否启用了 profile 映射
2. `/cityloader info` 中资源计数是否正常
3. 是否启用了过于严格的安全参数（如 `safeMode`/预算）
4. 是否在已生成老区块观察（建议传送到远处新区块）

---

最后更新: `2026-02-22`
