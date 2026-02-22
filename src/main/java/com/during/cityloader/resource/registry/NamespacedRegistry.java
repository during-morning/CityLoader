package com.during.cityloader.resource.registry;

import com.during.cityloader.util.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命名空间注册表基类
 * 提供Map-based命名空间隔离，支持ResourceLocation
 * 
 * @param <T> 注册的资源类型
 * @author During
 * @since 1.5.0
 */
public abstract class NamespacedRegistry<T> {

    // 嵌套Map: namespace → (path → resource)
    protected final Map<String, Map<String, T>> namespaceResources;

    // 是否线程安全
    private final boolean threadSafe;

    /**
     * 构造函数
     * 
     * @param threadSafe 是否需要线程安全
     */
    protected NamespacedRegistry(boolean threadSafe) {
        this.threadSafe = threadSafe;
        if (threadSafe) {
            this.namespaceResources = new ConcurrentHashMap<>();
        } else {
            this.namespaceResources = new HashMap<>();
        }
    }

    /**
     * 注册资源（使用ResourceLocation）
     * 
     * @param id       资源位置
     * @param resource 资源对象
     * @throws IllegalArgumentException 如果资源已存在
     */
    public void register(ResourceLocation id, T resource) {
        if (id == null || resource == null) {
            throw new IllegalArgumentException("ResourceLocation和资源不能为null");
        }

        String namespace = id.getNamespace();
        String path = id.getPath();

        // 获取或创建命名空间Map
        Map<String, T> resources = getOrCreateNamespaceMap(namespace);

        // 检查是否已存在
        if (resources.containsKey(path)) {
            throw new IllegalArgumentException("资源ID已存在: " + id);
        }

        resources.put(path, resource);
    }

    /**
     * 注册资源（使用String ID，向后兼容）
     * 
     * @param id       资源ID（可能包含或不包含命名空间）
     * @param resource 资源对象
     */
    public void register(String id, T resource) {
        ResourceLocation location = new ResourceLocation(id);
        register(location, resource);
    }

    /**
     * 获取资源（使用ResourceLocation）
     * 
     * @param id 资源位置
     * @return 资源对象，如果不存在返回null
     */
    public T get(ResourceLocation id) {
        if (id == null) {
            return null;
        }

        Map<String, T> resources = namespaceResources.get(id.getNamespace());
        return resources != null ? resources.get(id.getPath()) : null;
    }

    /**
     * 获取资源（使用String ID，向后兼容）
     * 
     * @param id 资源ID
     * @return 资源对象，如果不存在返回null
     */
    public T get(String id) {
        if (id == null) {
            return null;
        }
        return get(new ResourceLocation(id));
    }

    /**
     * 检查资源是否存在
     * 
     * @param id 资源位置
     * @return 如果存在返回true
     */
    public boolean contains(ResourceLocation id) {
        return get(id) != null;
    }

    /**
     * 检查资源是否存在（向后兼容）
     * 
     * @param id 资源ID
     * @return 如果存在返回true
     */
    public boolean contains(String id) {
        return get(id) != null;
    }

    /**
     * 检查资源是否存在（命名空间和路径）
     * 
     * @param namespace 命名空间
     * @param path      路径
     * @return 如果存在返回true
     */
    public boolean contains(String namespace, String path) {
        Map<String, T> resources = namespaceResources.get(namespace);
        return resources != null && resources.containsKey(path);
    }

    /**
     * 别名方法
     */
    public boolean exists(ResourceLocation id) {
        return contains(id);
    }

    public boolean exists(String id) {
        return contains(id);
    }

    /**
     * 清空所有资源
     */
    public void clear() {
        namespaceResources.clear();
    }

    /**
     * 清空指定命名空间的资源
     * 
     * @param namespace 命名空间
     */
    public void clearNamespace(String namespace) {
        namespaceResources.remove(namespace);
    }

    /**
     * 获取所有资源数量
     * 
     * @return 总资源数
     */
    public int size() {
        int total = 0;
        for (Map<String, T> resources : namespaceResources.values()) {
            total += resources.size();
        }
        return total;
    }

    /**
     * 获取指定命名空间的资源数量
     * 
     * @param namespace 命名空间
     * @return 该命名空间的资源数
     */
    public int sizeOf(String namespace) {
        Map<String, T> resources = namespaceResources.get(namespace);
        return resources != null ? resources.size() : 0;
    }

    /**
     * 获取所有命名空间
     * 
     * @return 命名空间集合
     */
    public Set<String> getNamespaces() {
        return new HashSet<>(namespaceResources.keySet());
    }

    /**
     * 获取所有资源
     * 
     * @return 所有资源的集合
     */
    public Collection<T> getAll() {
        List<T> allResources = new ArrayList<>();
        for (Map<String, T> resources : namespaceResources.values()) {
            allResources.addAll(resources.values());
        }
        return allResources;
    }

    /**
     * 获取指定命名空间的所有资源
     * 
     * @param namespace 命名空间
     * @return 该命名空间的资源集合
     */
    public Collection<T> getAllFrom(String namespace) {
        Map<String, T> resources = namespaceResources.get(namespace);
        return resources != null ? new ArrayList<>(resources.values()) : Collections.emptyList();
    }

    /**
     * 移除资源
     * 
     * @param id 资源位置
     * @return 被移除的资源，如果不存在返回null
     */
    public T remove(ResourceLocation id) {
        if (id == null) {
            return null;
        }

        Map<String, T> resources = namespaceResources.get(id.getNamespace());
        return resources != null ? resources.remove(id.getPath()) : null;
    }

    /**
     * 移除资源（向后兼容）
     * 
     * @param id 资源ID
     * @return 被移除的资源，如果不存在返回null
     */
    public T remove(String id) {
        return remove(new ResourceLocation(id));
    }

    /**
     * 获取或创建命名空间Map
     * 
     * @param namespace 命名空间
     * @return 命名空间对应的Map
     */
    protected Map<String, T> getOrCreateNamespaceMap(String namespace) {
        return namespaceResources.computeIfAbsent(namespace, k -> createResourceMap());
    }

    /**
     * 创建资源存储Map（子类可覆盖以使用不同的Map实现）
     * 
     * @return 新的Map实例
     */
    protected Map<String, T> createResourceMap() {
        if (threadSafe) {
            return new ConcurrentHashMap<>();
        } else {
            return new HashMap<>();
        }
    }

    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ").append(getRegistryName()).append(" 统计 ===\n");
        stats.append("总资源数: ").append(size()).append("\n");
        stats.append("命名空间数: ").append(namespaceResources.size()).append("\n\n");

        for (String namespace : getNamespaces()) {
            stats.append("  [").append(namespace).append("]: ")
                    .append(sizeOf(namespace)).append(" 个资源\n");
        }

        return stats.toString();
    }

    /**
     * 获取注册表名称（子类实现）
     * 
     * @return 注册表名称
     */
    protected abstract String getRegistryName();
}
