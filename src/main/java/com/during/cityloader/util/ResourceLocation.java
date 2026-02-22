package com.during.cityloader.util;

/**
 * Minecraft风格的资源位置（命名空间ID）
 * 格式: namespace:path
 * 示例: "minecraft:stone", "lostcities:chests/raildungeonchest"
 * 
 * @author During
 * @since 1.5.0
 */
public class ResourceLocation {

    public static final String DEFAULT_NAMESPACE = "lostcities";
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final char PATH_SEPARATOR = '/';

    private final String namespace;
    private final String path;

    /**
     * 完整构造函数
     * 
     * @param namespace 命名空间（如 "minecraft", "lostcities"）
     * @param path      路径（如 "stone", "chests/raildungeonchest"）
     */
    public ResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("命名空间不能为空");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }

        this.namespace = namespace.toLowerCase();
        this.path = path.toLowerCase();
    }

    /**
     * 从完整ID解析（如 "minecraft:stone"）
     * 如果没有命名空间前缀，使用默认命名空间 "lostcities"
     * 
     * @param id 完整ID字符串
     */
    public ResourceLocation(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }

        id = id.toLowerCase();
        int separatorIndex = id.indexOf(NAMESPACE_SEPARATOR);

        if (separatorIndex < 0) {
            // 没有命名空间，使用默认
            this.namespace = DEFAULT_NAMESPACE;
            this.path = id;
        } else if (separatorIndex == 0) {
            // 格式错误: ":path"
            throw new IllegalArgumentException("无效的ResourceLocation格式: " + id);
        } else {
            this.namespace = id.substring(0, separatorIndex);
            this.path = id.substring(separatorIndex + 1);

            if (this.path.isEmpty()) {
                throw new IllegalArgumentException("路径不能为空: " + id);
            }
        }
    }

    /**
     * 解析ResourceLocation（静态工厂方法）
     * 
     * @param id ID字符串
     * @return ResourceLocation实例，如果ID无效返回null
     */
    public static ResourceLocation parse(String id) {
        try {
            return new ResourceLocation(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 检查ID格式是否有效
     * 
     * @param id ID字符串
     * @return 如果格式有效返回true
     */
    public static boolean isValid(String id) {
        return parse(id) != null;
    }

    /**
     * 获取命名空间
     * 
     * @return 命名空间字符串
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 获取路径
     * 
     * @return 路径字符串
     */
    public String getPath() {
        return path;
    }

    /**
     * 检查是否为默认命名空间
     * 
     * @return 如果是默认命名空间返回true
     */
    public boolean isDefaultNamespace() {
        return DEFAULT_NAMESPACE.equals(namespace);
    }

    /**
     * 获取完整ID字符串
     * 
     * @return "namespace:path"格式字符串
     */
    @Override
    public String toString() {
        return namespace + NAMESPACE_SEPARATOR + path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ResourceLocation))
            return false;

        ResourceLocation other = (ResourceLocation) obj;
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }
}
