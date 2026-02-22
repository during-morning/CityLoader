package com.during.cityloader.util;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PaperResourceLoader 测试
 */
class PaperResourceLoaderTest {

    @Test
    void loadAssetShouldReadFromClasspathDataFolder() {
        JsonObject variant = PaperResourceLoader.loadAsset(
                null,
                "lostcities",
                "variants",
                "stonebrick",
                JsonObject.class);

        assertNotNull(variant, "应能从内置/data目录加载资产");
        assertTrue(variant.has("blocks"), "stonebrick变体应包含blocks字段");
    }

    @Test
    void scanAssetsShouldIncludeNamespacesAndNestedPaths() {
        List<PaperResourceLoader.AssetDescriptor> buildings = PaperResourceLoader.scanAssets(null, "buildings");

        assertTrue(contains(buildings, "lostcities:building1"), "应包含lostcities命名空间建筑");
        assertTrue(buildings.stream().anyMatch(descriptor ->
                        descriptor.getLocation().getNamespace().equals("pomkotsmechs")
                                && descriptor.getLocation().getPath().startsWith("ships/")),
                "应支持子目录路径并扫描到pomkotsmechs命名空间资产");
    }

    @Test
    void scanAssetsWithNamespaceShouldFilter() {
        List<ResourceLocation> variants = PaperResourceLoader.scanAssets(null, "pomkotsmechs", "variants");

        assertTrue(variants.contains(new ResourceLocation("pomkotsmechs", "stonebrick")),
                "应能按命名空间筛选资产");
        assertTrue(variants.size() > 1, "pomkotsmechs变体数量应大于1");
    }

    @Test
    void lastScanConflictsShouldBeReadable() {
        PaperResourceLoader.scanAssets(null, "variants");
        assertNotNull(PaperResourceLoader.getLastScanConflicts(), "冲突列表应可读取");
    }

    private boolean contains(List<PaperResourceLoader.AssetDescriptor> descriptors, String id) {
        return descriptors.stream().anyMatch(descriptor -> descriptor.getLocation().toString().equals(id));
    }
}
