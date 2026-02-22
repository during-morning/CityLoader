package com.during.cityloader.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Paper资源加载器
 * 仅从插件内置的/data目录加载JSON资产，不依赖world/datapacks目录。
 *
 * @author During
 * @since 1.4.0
 */
public class PaperResourceLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger LOGGER = Logger.getLogger("CityLoader");
    private static final String DATA_ROOT = "data";
    private static final String LOSTCITIES_SCOPE = "lostcities";
    private static volatile List<Path> externalDataRoots = List.of();
    private static volatile List<AssetConflict> lastScanConflicts = List.of();

    /**
     * 资产描述信息
     */
    public static final class AssetDescriptor {
        private final ResourceLocation location;
        private final String folder;
        private final String name;
        private final String sourcePack;
        private final String resourcePath;
        private final int priority;
        private final boolean external;

        private AssetDescriptor(ResourceLocation location, String folder, String name, String sourcePack,
                String resourcePath, int priority, boolean external) {
            this.location = location;
            this.folder = folder;
            this.name = name;
            this.sourcePack = sourcePack;
            this.resourcePath = resourcePath;
            this.priority = priority;
            this.external = external;
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public String getFolder() {
            return folder;
        }

        public String getName() {
            return name;
        }

        public String getSourcePack() {
            return sourcePack;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isExternal() {
            return external;
        }

        public String getAssetId() {
            return location + " (" + folder + "/" + name + ")";
        }

        public String describeSource() {
            String type = external ? "external" : "classpath";
            return type + ":" + sourcePack + ":" + resourcePath;
        }
    }

    /**
     * 资产覆盖冲突记录
     */
    public static final class AssetConflict {
        private final ResourceLocation location;
        private final String folder;
        private final String name;
        private final String overriddenSourcePack;
        private final String overriddenResourcePath;
        private final String overridingSourcePack;
        private final String overridingResourcePath;

        private AssetConflict(ResourceLocation location, String folder, String name, String overriddenSourcePack,
                String overriddenResourcePath, String overridingSourcePack, String overridingResourcePath) {
            this.location = location;
            this.folder = folder;
            this.name = name;
            this.overriddenSourcePack = overriddenSourcePack;
            this.overriddenResourcePath = overriddenResourcePath;
            this.overridingSourcePack = overridingSourcePack;
            this.overridingResourcePath = overridingResourcePath;
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public String getFolder() {
            return folder;
        }

        public String getName() {
            return name;
        }

        public String getOverriddenSourcePack() {
            return overriddenSourcePack;
        }

        public String getOverriddenResourcePath() {
            return overriddenResourcePath;
        }

        public String getOverridingSourcePack() {
            return overridingSourcePack;
        }

        public String getOverridingResourcePath() {
            return overridingResourcePath;
        }
    }

    /**
     * 从内置/data目录加载JSON资产
     *
     * @param world 世界（兼容参数，当前不使用）
     * @param namespace 命名空间（例如"lostcities"）
     * @param folder 文件夹（例如"palettes"）
     * @param name 资产名称（不含扩展名）
     * @param clazz 目标类
     * @param <T> 资产类型
     * @return 加载结果，失败返回null
     */
    public static <T> T loadAsset(World world, String namespace, String folder, String name, Class<T> clazz) {
        return loadAsset(world, namespace, folder, name, clazz, "direct");
    }

    /**
     * 从内置/data目录加载JSON资产，并记录依赖链路
     *
     * @param world 世界（兼容参数，当前不使用）
     * @param namespace 命名空间
     * @param folder 文件夹
     * @param name 资产名称
     * @param clazz 目标类型
     * @param dependencyChain 依赖链路标记
     * @param <T> 资产类型
     * @return 加载结果，失败返回null
     */
    public static <T> T loadAsset(World world, String namespace, String folder, String name, Class<T> clazz,
            String dependencyChain) {
        if (namespace == null || folder == null || name == null || clazz == null) {
            return null;
        }

        String cleanNamespace = namespace.toLowerCase(Locale.ROOT);
        String cleanFolder = normalizePathComponent(folder);
        String cleanName = normalizePathComponent(name);
        String dependency = dependencyChain == null || dependencyChain.isBlank() ? "direct" : dependencyChain;

        List<String> candidates = buildResourceCandidates(cleanNamespace, cleanFolder, cleanName);
        String assetId = cleanNamespace + ":" + cleanName;
        for (String candidate : candidates) {
            T loaded = loadAssetFromResourcePath(candidate, clazz, assetId, dependency);
            if (loaded != null) {
                return loaded;
            }
        }

        return null;
    }

    /**
     * 按已解析来源加载资产
     *
     * @param descriptor 资产描述
     * @param clazz 目标类型
     * @param dependencyChain 依赖链路标记
     * @param <T> 资产类型
     * @return 资产对象，失败返回null
     */
    public static <T> T loadAsset(AssetDescriptor descriptor, Class<T> clazz, String dependencyChain) {
        if (descriptor == null || clazz == null) {
            return null;
        }
        String dependency = dependencyChain == null || dependencyChain.isBlank() ? "direct" : dependencyChain;
        return loadAssetFromResourcePath(descriptor.getResourcePath(), clazz, descriptor.getLocation().toString(), dependency);
    }

    /**
     * 扫描指定命名空间文件夹中的资产
     *
     * @param world 世界（兼容参数，当前不使用）
     * @param namespace 命名空间
     * @param folder 文件夹
     * @return 资产列表
     */
    public static List<ResourceLocation> scanAssets(World world, String namespace, String folder) {
        if (namespace == null || namespace.isBlank() || folder == null || folder.isBlank()) {
            return List.of();
        }

        String expectedNamespace = namespace.toLowerCase(Locale.ROOT);
        List<ResourceLocation> results = new ArrayList<>();
        for (AssetDescriptor descriptor : scanAssets(world, folder)) {
            if (descriptor.getLocation().getNamespace().equals(expectedNamespace)) {
                results.add(descriptor.getLocation());
            }
        }
        return results;
    }

    /**
     * 扫描指定类型资产，来源为插件内置/data目录
     *
     * @param world 世界（兼容参数，当前不使用）
     * @param folder 资产目录（如"palettes"）
     * @return 资产描述列表
     */
    public static List<AssetDescriptor> scanAssets(World world, String folder) {
        if (folder == null || folder.isBlank()) {
            lastScanConflicts = List.of();
            return List.of();
        }

        String cleanFolder = normalizePathComponent(folder);
        Map<String, AssetDescriptor> merged = new LinkedHashMap<>();
        List<AssetConflict> conflicts = new ArrayList<>();

        List<AssetDescriptor> descriptors = new ArrayList<>();
        for (ClasspathJsonResource jsonResource : scanClasspathJsonResources()) {
            AssetDescriptor descriptor = toDescriptor(jsonResource, cleanFolder);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }

        for (ExternalJsonResource jsonResource : scanExternalJsonResources()) {
            AssetDescriptor descriptor = toDescriptor(jsonResource, cleanFolder);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }

        for (AssetDescriptor descriptor : descriptors) {
            String key = descriptor.getLocation().toString();
            AssetDescriptor previous = merged.get(key);
            if (previous == null) {
                merged.put(key, descriptor);
                continue;
            }

            if (descriptor.getPriority() >= previous.getPriority()) {
                conflicts.add(new AssetConflict(
                        descriptor.getLocation(),
                        descriptor.getFolder(),
                        descriptor.getName(),
                        previous.getSourcePack(),
                        previous.getResourcePath(),
                        descriptor.getSourcePack(),
                        descriptor.getResourcePath()));
                merged.put(key, descriptor);
            }
        }

        List<AssetDescriptor> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(d -> d.getLocation().toString()));
        lastScanConflicts = List.copyOf(conflicts);
        return result;
    }

    /**
     * 获取最近一次扫描的覆盖冲突信息
     *
     * @return 冲突列表
     */
    public static List<AssetConflict> getLastScanConflicts() {
        return lastScanConflicts;
    }

    /**
     * 设置外部数据根目录（plugins/CityLoader/data）
     *
     * @param dataRoot 外部数据根目录
     */
    public static void setExternalDataRoot(Path dataRoot) {
        if (dataRoot == null) {
            externalDataRoots = List.of();
            return;
        }
        setExternalDataRoots(List.of(dataRoot));
    }

    /**
     * 设置外部数据目录列表（按顺序叠加，后者覆盖前者）。
     *
     * @param dataRoots 外部数据目录列表
     */
    public static void setExternalDataRoots(List<Path> dataRoots) {
        if (dataRoots == null || dataRoots.isEmpty()) {
            externalDataRoots = List.of();
            return;
        }

        List<Path> normalized = new ArrayList<>();
        for (Path root : dataRoots) {
            if (root == null) {
                continue;
            }
            Path absolute = root.toAbsolutePath().normalize();
            if (!normalized.contains(absolute)) {
                normalized.add(absolute);
            }
        }
        externalDataRoots = normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    /**
     * 保留兼容方法：已不再使用world/datapacks机制。
     *
     * @param world 世界
     * @return 恒为null
     */
    public static Path getDataPackPath(World world) {
        List<Path> roots = externalDataRoots;
        return roots.isEmpty() ? null : roots.get(0);
    }

    /**
     * 获取外部数据目录列表（按叠加顺序返回）。
     *
     * @return 外部目录列表
     */
    public static List<Path> getExternalDataRoots() {
        return externalDataRoots;
    }

    private static <T> T loadAssetFromResourcePath(String resourcePath, Class<T> clazz, String assetId,
            String dependency) {
        Path externalPath = null;
        if (resourcePath != null && resourcePath.startsWith("external:")) {
            externalPath = Paths.get(resourcePath.substring("external:".length()));
        }

        if (externalPath != null) {
            try (InputStream stream = Files.newInputStream(externalPath)) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, clazz);
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warning("加载资产失败: source=external path=" + externalPath
                        + " asset=" + assetId
                        + " dependency=" + dependency
                        + " error=" + e.getMessage());
                return null;
            }
        }

        try (InputStream stream = PaperResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, clazz);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warning("加载资产失败: source=classpath path=" + resourcePath
                    + " asset=" + assetId
                    + " dependency=" + dependency
                    + " error=" + e.getMessage());
            return null;
        }
    }

    private static AssetDescriptor toDescriptor(ClasspathJsonResource resource, String cleanFolder) {
        String resourcePath = normalizePathComponent(resource.resourcePath());
        if (!resourcePath.startsWith(DATA_ROOT + "/") || !resourcePath.endsWith(".json")) {
            return null;
        }

        String[] parts = resourcePath.split("/");
        if (parts.length < 4) {
            return null;
        }

        String namespace = parts[1];
        int folderIndex;
        int nameStartIndex;
        int priority;

        if (parts.length >= 5 && LOSTCITIES_SCOPE.equals(parts[2])) {
            folderIndex = 3;
            nameStartIndex = 4;
            priority = 2;
        } else {
            folderIndex = 2;
            nameStartIndex = 3;
            priority = 1;
        }

        if (!cleanFolder.equals(parts[folderIndex])) {
            return null;
        }

        String name = trimJsonSuffix(String.join("/", Arrays.copyOfRange(parts, nameStartIndex, parts.length)));
        if (name.isBlank()) {
            return null;
        }

        ResourceLocation location;
        try {
            location = new ResourceLocation(namespace, name);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("忽略非法资产路径: " + resourcePath + " error=" + e.getMessage());
            return null;
        }

        return new AssetDescriptor(
                location,
                cleanFolder,
                name,
                resource.sourcePack(),
                resourcePath,
                priority,
                false);
    }

    private static AssetDescriptor toDescriptor(ExternalJsonResource resource, String cleanFolder) {
        String resourcePath = normalizePathComponent(resource.resourcePath());
        if (!resourcePath.endsWith(".json")) {
            return null;
        }

        String[] parts = resourcePath.split("/");
        if (parts.length < 3) {
            return null;
        }

        String namespace = parts[0];
        int folderIndex;
        int nameStartIndex;
        int priority;

        if (parts.length >= 4 && LOSTCITIES_SCOPE.equals(parts[1])) {
            folderIndex = 2;
            nameStartIndex = 3;
            priority = 2;
        } else {
            folderIndex = 1;
            nameStartIndex = 2;
            priority = 2;
        }

        if (!cleanFolder.equals(parts[folderIndex])) {
            return null;
        }

        String name = trimJsonSuffix(String.join("/", Arrays.copyOfRange(parts, nameStartIndex, parts.length)));
        if (name.isBlank()) {
            return null;
        }

        ResourceLocation location;
        try {
            location = new ResourceLocation(namespace, name);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("忽略非法外部资产路径: " + resourcePath + " error=" + e.getMessage());
            return null;
        }

        String absolutePath = "external:" + resource.absolutePath();
        return new AssetDescriptor(
                location,
                cleanFolder,
                name,
                resource.sourcePack(),
                absolutePath,
                Math.max(priority, resource.priority()),
                true);
    }

    private static List<ClasspathJsonResource> scanClasspathJsonResources() {
        Path codeSource = getCodeSourcePath();
        if (codeSource == null) {
            return List.of();
        }

        if (Files.isDirectory(codeSource)) {
            return scanJsonFromDirectory(codeSource);
        }
        if (Files.isRegularFile(codeSource) && codeSource.toString().endsWith(".jar")) {
            return scanJsonFromJar(codeSource);
        }

        return List.of();
    }

    private static List<ExternalJsonResource> scanExternalJsonResources() {
        List<Path> roots = externalDataRoots;
        if (roots.isEmpty()) {
            return List.of();
        }

        List<ExternalJsonResource> resources = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            Path root = roots.get(i);
            if (!Files.isDirectory(root)) {
                continue;
            }

            String sourcePack = root.toString();
            int priority = 100 + i;
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .sorted()
                        .forEach(path -> {
                            String relative = root.relativize(path).toString().replace('\\', '/');
                            resources.add(new ExternalJsonResource(sourcePack, relative, path.toAbsolutePath().toString(), priority));
                        });
            } catch (IOException e) {
                LOGGER.warning("扫描外部data目录失败: " + root + " error=" + e.getMessage());
            }
        }
        return resources;
    }

    private static Path getCodeSourcePath() {
        try {
            return Paths.get(PaperResourceLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | RuntimeException e) {
            LOGGER.warning("获取类路径根目录失败: " + e.getMessage());
            return null;
        }
    }

    private static List<ClasspathJsonResource> scanJsonFromDirectory(Path classpathRoot) {
        Path dataRoot = classpathRoot.resolve(DATA_ROOT);
        if (!Files.isDirectory(dataRoot)) {
            return List.of();
        }

        String sourcePack = classpathRoot.getFileName() == null ? "classes" : classpathRoot.getFileName().toString();

        try (Stream<Path> stream = Files.walk(dataRoot)) {
            List<ClasspathJsonResource> resources = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> {
                        String relative = dataRoot.relativize(path).toString().replace('\\', '/');
                        resources.add(new ClasspathJsonResource(sourcePack, DATA_ROOT + "/" + relative));
                    });
            return resources;
        } catch (IOException e) {
            LOGGER.warning("扫描内置data目录失败: " + dataRoot + " error=" + e.getMessage());
            return List.of();
        }
    }

    private static List<ClasspathJsonResource> scanJsonFromJar(Path jarPath) {
        String sourcePack = jarPath.getFileName() == null ? "jar" : jarPath.getFileName().toString();
        List<ClasspathJsonResource> resources = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(DATA_ROOT + "/"))
                    .filter(name -> name.endsWith(".json"))
                    .sorted()
                    .forEach(name -> resources.add(new ClasspathJsonResource(sourcePack, name)));
        } catch (IOException e) {
            LOGGER.warning("扫描jar内置data目录失败: " + jarPath + " error=" + e.getMessage());
            return List.of();
        }

        return resources;
    }

    private static List<String> buildResourceCandidates(String namespace, String folder, String name) {
        List<String> candidates = new ArrayList<>();

        if (!externalDataRoots.isEmpty()) {
            List<String> externalCandidates = buildExternalCandidates(namespace, folder, name);
            candidates.addAll(externalCandidates);
        }

        // 优先LostCities标准层级: data/<namespace>/lostcities/<folder>/<name>.json
        candidates.add(DATA_ROOT + "/" + namespace + "/" + LOSTCITIES_SCOPE + "/" + folder + "/" + name + ".json");

        // 兼容层级: data/<namespace>/<folder>/<name>.json
        if (!folder.startsWith(LOSTCITIES_SCOPE + "/")) {
            candidates.add(DATA_ROOT + "/" + namespace + "/" + folder + "/" + name + ".json");
        }

        return candidates;
    }

    private static List<String> buildExternalCandidates(String namespace, String folder, String name) {
        List<String> candidates = new ArrayList<>();
        List<Path> roots = externalDataRoots;
        if (roots.isEmpty()) {
            return candidates;
        }

        for (int i = roots.size() - 1; i >= 0; i--) {
            Path root = roots.get(i);
            Path lostcitiesPath = root.resolve(namespace)
                    .resolve(LOSTCITIES_SCOPE)
                    .resolve(folder)
                    .resolve(name + ".json");
            if (Files.isRegularFile(lostcitiesPath)) {
                candidates.add("external:" + lostcitiesPath.toAbsolutePath());
            }

            Path fallbackPath = root.resolve(namespace)
                    .resolve(folder)
                    .resolve(name + ".json");
            if (Files.isRegularFile(fallbackPath)) {
                candidates.add("external:" + fallbackPath.toAbsolutePath());
            }
        }

        return candidates;
    }

    private static String trimJsonSuffix(String value) {
        if (value.endsWith(".json")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private static String normalizePathComponent(String value) {
        String normalized = Objects.toString(value, "").trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 获取Gson实例
     *
     * @return Gson实例
     */
    public static Gson getGson() {
        return GSON;
    }

    private record ClasspathJsonResource(String sourcePack, String resourcePath) {
    }

    private record ExternalJsonResource(String sourcePack, String resourcePath, String absolutePath, int priority) {
    }
}
