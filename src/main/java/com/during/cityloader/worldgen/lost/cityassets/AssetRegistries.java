package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.CityLoaderLogger;
import com.during.cityloader.worldgen.lost.regassets.*;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产注册表中心类
 * 管理所有城市生成资产的中央注册系统
 * 
 * @author During
 * @since 1.4.0
 */
public class AssetRegistries {

    // 静态注册表实例
    public static final RegistryAssetRegistry<Variant, VariantRE> VARIANTS = 
            new RegistryAssetRegistry<>("variants", Variant::new, VariantRE.class);
    
    public static final RegistryAssetRegistry<Condition, ConditionRE> CONDITIONS = 
            new RegistryAssetRegistry<>("conditions", Condition::new, ConditionRE.class);
    
    public static final RegistryAssetRegistry<WorldStyle, WorldStyleRE> WORLDSTYLES = 
            new RegistryAssetRegistry<>("worldstyles", WorldStyle::new, WorldStyleRE.class);
    
    public static final RegistryAssetRegistry<CityStyle, CityStyleRE> CITYSTYLES = 
            new RegistryAssetRegistry<>("citystyles", CityStyle::new, CityStyleRE.class);
    
    public static final RegistryAssetRegistry<BuildingPart, BuildingPartRE> PARTS = 
            new RegistryAssetRegistry<>("parts", BuildingPart::new, BuildingPartRE.class);
    
    public static final RegistryAssetRegistry<Building, BuildingRE> BUILDINGS = 
            new RegistryAssetRegistry<>("buildings", Building::new, BuildingRE.class);
    
    public static final RegistryAssetRegistry<MultiBuilding, MultiBuildingRE> MULTI_BUILDINGS = 
            new RegistryAssetRegistry<>("multibuildings", MultiBuilding::new, MultiBuildingRE.class);
    
    public static final RegistryAssetRegistry<Style, StyleRE> STYLES = 
            new RegistryAssetRegistry<>("styles", Style::new, StyleRE.class);
    
    public static final RegistryAssetRegistry<Palette, PaletteRE> PALETTES = 
            new RegistryAssetRegistry<>("palettes", Palette::new, PaletteRE.class);
    
    public static final RegistryAssetRegistry<ScatteredBuilding, ScatteredRE> SCATTERED = 
            new RegistryAssetRegistry<>("scattered", ScatteredBuilding::new, ScatteredRE.class);
    
    public static final RegistryAssetRegistry<PredefinedCity, PredefinedCityRE> PREDEFINED_CITIES = 
            new RegistryAssetRegistry<>("predefinedcities", PredefinedCity::new, PredefinedCityRE.class);
    
    public static final RegistryAssetRegistry<PredefinedSphere, PredefinedSphereRE> PREDEFINED_SPHERES = 
            new RegistryAssetRegistry<>("predefinedspheres", PredefinedSphere::new, PredefinedSphereRE.class);
    
    public static final RegistryAssetRegistry<StuffObject, StuffSettingsRE> STUFF = 
            new RegistryAssetRegistry<>("stuff", StuffObject::new, StuffSettingsRE.class);

    // 按标签索引的装饰物
    public static final Map<String, List<StuffObject>> STUFF_BY_TAG = new HashMap<>();

    private static boolean loaded = false;
    private static boolean loadedPredefined = false;

    /**
     * 设置资产加载日志记录器
     *
     * @param logger 日志记录器
     */
    public static void setLogger(CityLoaderLogger logger) {
        VARIANTS.setLogger(logger);
        CONDITIONS.setLogger(logger);
        WORLDSTYLES.setLogger(logger);
        CITYSTYLES.setLogger(logger);
        PARTS.setLogger(logger);
        BUILDINGS.setLogger(logger);
        MULTI_BUILDINGS.setLogger(logger);
        STYLES.setLogger(logger);
        PALETTES.setLogger(logger);
        SCATTERED.setLogger(logger);
        PREDEFINED_CITIES.setLogger(logger);
        PREDEFINED_SPHERES.setLogger(logger);
        STUFF.setLogger(logger);
    }

    /**
     * 重置所有注册表
     * 清除所有缓存的资产
     */
    public static void reset() {
        VARIANTS.reset();
        CONDITIONS.reset();
        WORLDSTYLES.reset();
        CITYSTYLES.reset();
        PARTS.reset();
        BUILDINGS.reset();
        MULTI_BUILDINGS.reset();
        STYLES.reset();
        PALETTES.reset();
        SCATTERED.reset();
        PREDEFINED_CITIES.reset();
        PREDEFINED_SPHERES.reset();
        STUFF.reset();
        STUFF_BY_TAG.clear();
        loaded = false;
        loadedPredefined = false;
    }

    /**
     * 加载所有资产
     * 按照依赖关系顺序加载所有资产类型
     * 
     * @param level 世界
     */
    public static void load(World level) {
        if (loaded) {
            return;
        }
        
        // 第一层：基础资产（无依赖）
        // 这些资产不依赖其他资产，可以首先加载
        VARIANTS.loadAll(level);      // 方块变体
        CONDITIONS.loadAll(level);    // 条件系统
        
        // 第二层：调色板和样式（依赖变体）
        PALETTES.loadAll(level);      // 调色板（可能引用变体）
        STYLES.loadAll(level);        // 样式（可能引用调色板和变体）
        
        // 第三层：建筑部件（依赖调色板）
        PARTS.loadAll(level);         // 建筑部件（已实现）
        
        // 第四层：建筑和多建筑（依赖部件和调色板）
        BUILDINGS.loadAll(level);     // 建筑（已实现）
        MULTI_BUILDINGS.loadAll(level); // 多建筑
        
        // 第五层：城市样式（依赖建筑和样式）
        CITYSTYLES.loadAll(level);    // 城市样式
        WORLDSTYLES.loadAll(level);   // 世界样式
        
        // 第六层：特殊资产（依赖建筑）
        SCATTERED.loadAll(level);     // 散布建筑
        STUFF.loadAll(level);         // 装饰物（已实现）

        // 第七层：预定义资产
        PREDEFINED_CITIES.loadAll(level);
        PREDEFINED_SPHERES.loadAll(level);

        // 资产引用审计：提前发现缺失引用，减少运行期静默失败
        AssetReferenceAuditor.audit(level);
        
        // 构建装饰物标签索引
        STUFF.getIterable().forEach(stuff -> {
            if (stuff.getSettings() != null && stuff.getSettings().getTags() != null) {
                stuff.getSettings().getTags().forEach(tag -> {
                    List<StuffObject> list = STUFF_BY_TAG.computeIfAbsent(tag, k -> new ArrayList<>());
                    list.add(stuff);
                });
            }
        });
        
        loaded = true;
        loadedPredefined = true;
    }

    /**
     * 加载预定义的资产
     * 
     * @param level 世界
     */
    public static void loadPredefinedStuff(World level) {
        if (loadedPredefined) {
            return;
        }
        
        PREDEFINED_CITIES.loadAll(level);
        PREDEFINED_SPHERES.loadAll(level);
        
        loadedPredefined = true;
    }

    /**
     * 获取资产加载统计信息
     * 
     * @return 统计字符串
     */
    public static String getStatistics() {
        return String.format(
                "Palettes=%d, Variants=%d, Conditions=%d, Styles=%d, Parts=%d, " +
                "Buildings=%d, MultiBuildings=%d, CityStyles=%d, WorldStyles=%d, " +
                "Scattered=%d, Stuff=%d, PredefinedCities=%d, PredefinedSpheres=%d",
                PALETTES.size(), VARIANTS.size(), CONDITIONS.size(), STYLES.size(), PARTS.size(),
                BUILDINGS.size(), MULTI_BUILDINGS.size(), CITYSTYLES.size(), WORLDSTYLES.size(),
                SCATTERED.size(), STUFF.size(), PREDEFINED_CITIES.size(), PREDEFINED_SPHERES.size());
    }

    /**
     * 检查是否已加载
     * 
     * @return 如果已加载返回true
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * 检查预定义资产是否已加载
     * 
     * @return 如果已加载返回true
     */
    public static boolean isPredefinedLoaded() {
        return loadedPredefined;
    }
}
