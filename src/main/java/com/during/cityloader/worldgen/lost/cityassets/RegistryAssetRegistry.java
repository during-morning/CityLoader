package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.exception.AssetNotFoundException;
import com.during.cityloader.exception.AssetParseException;
import com.during.cityloader.util.CityLoaderLogger;
import com.during.cityloader.util.PaperResourceLoader;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.IAsset;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 注册资产注册表泛型类
 * 用于管理特定类型资产的注册和缓存
 * 
 * @param <T> 资产类型（继承自ILostCityAsset）
 * @param <R> 注册实体类型（继承自IAsset）
 * 
 * @author During
 * @since 1.4.0
 */
public class RegistryAssetRegistry<T extends ILostCityAsset, R extends IAsset> {

    private final Map<ResourceLocation, T> cache = new ConcurrentHashMap<>();
    private final String registryKey;
    private final Function<R, T> constructor;
    private final Class<R> registryClass;
    private boolean loaded = false;
    private int lastLoadSuccessCount = 0;
    private int lastLoadFailureCount = 0;
    
    // 日志记录器（可选，如果未设置则不记录日志）
    private CityLoaderLogger logger;

    /**
     * 构造注册资产注册表
     * 
     * @param registryKey 注册表键
     * @param constructor 从注册实体构造资产的函数
     * @param registryClass 注册实体的Class对象（用于JSON反序列化）
     */
    public RegistryAssetRegistry(String registryKey, Function<R, T> constructor, Class<R> registryClass) {
        this.registryKey = registryKey;
        this.constructor = constructor;
        this.registryClass = registryClass;
    }
    
    /**
     * 设置日志记录器
     * 
     * @param logger 日志记录器
     */
    public void setLogger(CityLoaderLogger logger) {
        this.logger = logger;
    }

    /**
     * 根据名称获取资产
     * 
     * @param level 世界
     * @param name 资产名称
     * @return 资产对象，如果不存在则返回null
     */
    public T get(World level, String name) {
        if (name == null) {
            return null;
        }
        return get(level, new ResourceLocation(name));
    }

    /**
     * 根据资源位置获取资产
     * 
     * @param level 世界
     * @param name 资源位置
     * @return 资产对象，如果不存在则返回null
     */
    public T get(World level, ResourceLocation name) {
        if (name == null) {
            return null;
        }
        
        T asset = cache.get(name);
        if (asset == null) {
            // 尝试延迟加载
            try {
                asset = loadAsset(level, name);
                if (asset != null) {
                    cache.put(name, asset);
                    if (logger != null) {
                        logger.logAssetLoad(registryKey, name.toString(), "延迟加载");
                    }
                }
            } catch (AssetParseException e) {
                if (logger != null) {
                    logger.logAssetError(registryKey, name.toString(), e.getFilePath(), 
                            "解析失败", e);
                }
                // 返回null而不是抛出异常，允许系统继续运行
                return null;
            } catch (Exception e) {
                if (logger != null) {
                    logger.logAssetError(registryKey, name.toString(), name.toString(), 
                            "加载失败: " + e.getMessage(), e);
                }
                return null;
            }
        }
        
        return asset;
    }

    /**
     * 根据名称获取资产，如果不存在则抛出异常
     * 
     * @param level 世界
     * @param name 资产名称
     * @return 资产对象
     * @throws AssetNotFoundException 如果资产不存在
     */
    public T getOrThrow(World level, String name) {
        if (name == null) {
            throw new AssetNotFoundException(registryKey, "null");
        }
        T result = get(level, name);
        if (result == null) {
            throw new AssetNotFoundException(registryKey, name);
        }
        return result;
    }

    /**
     * 获取所有资产的可迭代对象
     * 
     * @return 资产集合
     */
    public Iterable<T> getIterable() {
        return cache.values();
    }

    /**
     * 加载所有资产
     * 
     * @param level 世界
     */
    public void loadAll(World level) {
        if (loaded) {
            return;
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        try {
            // 扫描并加载所有命名空间资产（来源为插件内置/data目录与外部data目录）
            List<PaperResourceLoader.AssetDescriptor> assetDescriptors = PaperResourceLoader.scanAssets(level, registryKey);
            if (logger != null) {
                List<PaperResourceLoader.AssetConflict> conflicts = PaperResourceLoader.getLastScanConflicts();
                if (conflicts != null && !conflicts.isEmpty()) {
                    logger.logAssetConflictSummary(registryKey, conflicts.size());
                    for (PaperResourceLoader.AssetConflict conflict : conflicts) {
                        String overridden = conflict.getOverriddenSourcePack() + ":" + conflict.getOverriddenResourcePath();
                        String overriding = conflict.getOverridingSourcePack() + ":" + conflict.getOverridingResourcePath();
                        logger.logAssetConflict(registryKey, conflict.getLocation().toString(), overridden, overriding);
                    }
                }
            }

            for (PaperResourceLoader.AssetDescriptor descriptor : assetDescriptors) {
                ResourceLocation name = descriptor.getLocation();
                if (!cache.containsKey(name)) {
                    try {
                        T asset = loadAsset(descriptor);
                        if (asset != null) {
                            cache.put(name, asset);
                            successCount++;
                            if (logger != null) {
                                logger.logAssetLoad(registryKey, name.toString(), 
                                        descriptor.describeSource());
                            }
                        } else {
                            failureCount++;
                        }
                    } catch (AssetParseException e) {
                        failureCount++;
                        if (logger != null) {
                            logger.logAssetError(registryKey, name.toString(), e.getFilePath(), 
                                    "解析失败", e);
                        }
                        // 继续加载其他资产
                    } catch (Exception e) {
                        failureCount++;
                        if (logger != null) {
                            logger.logAssetError(registryKey, name.toString(), name.toString(), 
                                    "加载失败: " + e.getMessage(), e);
                        }
                        // 继续加载其他资产
                    }
                }
            }
            
            if (logger != null && (successCount > 0 || failureCount > 0)) {
                logger.info(String.format("✓ 加载%s资产完成: 成功=%d, 失败=%d", 
                        registryKey, successCount, failureCount));
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error(String.format("✗ 扫描%s资产失败: %s", registryKey, e.getMessage()), e);
            }
        }
        
        lastLoadSuccessCount = successCount;
        lastLoadFailureCount = failureCount;
        loaded = true;
    }

    /**
     * 从数据包加载单个资产
     * 
     * @param level 世界
     * @param name 资源位置
     * @return 加载的资产，如果失败则返回null
     */
    private T loadAsset(World level, ResourceLocation name) {
        if (registryClass == null) {
            if (logger != null) {
                logger.warning("无法加载资产 " + name + ": registryClass为null");
            }
            return null;
        }
        
        try {
            // 使用PaperResourceLoader加载JSON
            R registryEntity = PaperResourceLoader.loadAsset(
                    level, 
                    name.getNamespace(), 
                    registryKey, 
                    name.getPath(), 
                    registryClass,
                    registryKey + " -> " + name
            );
            
            if (registryEntity == null) {
                return null;
            }
            
            // 设置注册名称
            registryEntity.setRegistryName(name);
            
            // 使用构造函数转换为资产对象
            return constructor.apply(registryEntity);
            
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("加载资产失败 " + name + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 按已解析数据源加载资产
     *
     * @param descriptor 资产描述信息
     * @return 资产对象
     */
    private T loadAsset(PaperResourceLoader.AssetDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        if (registryClass == null) {
            if (logger != null) {
                logger.warning("无法加载资产 " + descriptor.getLocation() + ": registryClass为null");
            }
            return null;
        }

        ResourceLocation name = descriptor.getLocation();

        try {
            R registryEntity = PaperResourceLoader.loadAsset(
                    descriptor,
                    registryClass,
                    registryKey + " -> " + name);

            if (registryEntity == null) {
                return null;
            }

            registryEntity.setRegistryName(name);
            return constructor.apply(registryEntity);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("加载资产失败 " + name + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 重置注册表，清除所有缓存
     */
    public void reset() {
        cache.clear();
        loaded = false;
        lastLoadSuccessCount = 0;
        lastLoadFailureCount = 0;
    }

    /**
     * 手动注册资产（用于测试或动态加载）
     * 
     * @param name 资源位置
     * @param registryEntity 注册实体
     */
    public void register(ResourceLocation name, R registryEntity) {
        try {
            registryEntity.setRegistryName(name);
            T asset = constructor.apply(registryEntity);
            cache.put(name, asset);
            if (logger != null) {
                logger.logAssetLoad(registryKey, name.toString(), "手动注册");
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.logAssetError(registryKey, name.toString(), name.toString(), 
                        "注册失败: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * 检查是否已加载
     * 
     * @return 如果已加载返回true
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 获取缓存的资产数量
     * 
     * @return 资产数量
     */
    public int size() {
        return cache.size();
    }

    public int getLastLoadSuccessCount() {
        return lastLoadSuccessCount;
    }

    public int getLastLoadFailureCount() {
        return lastLoadFailureCount;
    }
}
