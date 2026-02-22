package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.*;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AssetRegistries 测试
 * 测试新的资产注册系统
 * 
 * @author During
 * @since 1.4.0
 */
public class AssetRegistriesTest {

    @Mock
    private World mockWorld;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 重置所有注册表
        AssetRegistries.reset();
        
        // 模拟World的基本行为
        when(mockWorld.getName()).thenReturn("test_world");
        when(mockWorld.getSeed()).thenReturn(12345L);
    }

    /**
     * 测试注册表重置功能
     */
    @Test
    void testReset() {
        // 重置应该清空所有注册表
        AssetRegistries.reset();
        
        // 验证所有注册表都是空的
        assertEquals(0, AssetRegistries.PALETTES.size(), "调色板注册表应该为空");
        assertEquals(0, AssetRegistries.VARIANTS.size(), "变体注册表应该为空");
        assertEquals(0, AssetRegistries.CONDITIONS.size(), "条件注册表应该为空");
        assertEquals(0, AssetRegistries.PARTS.size(), "部件注册表应该为空");
        assertEquals(0, AssetRegistries.BUILDINGS.size(), "建筑注册表应该为空");
    }

    /**
     * 测试手动注册资产
     */
    @Test
    void testManualRegistration() {
        // 创建测试资产
        PaletteRE paletteRE = new PaletteRE();
        ResourceLocation name = new ResourceLocation("test", "manual_palette");
        
        // 手动注册
        AssetRegistries.PALETTES.register(name, paletteRE);
        
        // 验证
        assertEquals(1, AssetRegistries.PALETTES.size(), "应该有1个调色板");
        assertNotNull(AssetRegistries.PALETTES.get(mockWorld, name), "应该能获取注册的调色板");
    }

    /**
     * 测试资产获取 - 不存在的资产
     */
    @Test
    void testGetNonExistentAsset() {
        // 获取不存在的资产应该返回null
        Palette palette = AssetRegistries.PALETTES.get(mockWorld, "nonexistent");
        assertNull(palette, "不存在的资产应该返回null");
    }

    /**
     * 测试资产获取 - 使用ResourceLocation
     */
    @Test
    void testGetAssetWithResourceLocation() {
        // 创建并注册测试资产
        PaletteRE paletteRE = new PaletteRE();
        ResourceLocation name = new ResourceLocation("test", "test_palette");
        AssetRegistries.PALETTES.register(name, paletteRE);
        
        // 使用ResourceLocation获取
        Palette palette = AssetRegistries.PALETTES.get(mockWorld, name);
        assertNotNull(palette, "应该能通过ResourceLocation获取资产");
    }

    /**
     * 测试资产获取 - 使用字符串
     */
    @Test
    void testGetAssetWithString() {
        // 创建并注册测试资产
        PaletteRE paletteRE = new PaletteRE();
        ResourceLocation name = new ResourceLocation("test", "test_palette");
        AssetRegistries.PALETTES.register(name, paletteRE);
        
        // 使用字符串获取
        Palette palette = AssetRegistries.PALETTES.get(mockWorld, "test:test_palette");
        assertNotNull(palette, "应该能通过字符串获取资产");
    }

    /**
     * 测试getOrThrow - 存在的资产
     */
    @Test
    void testGetOrThrowExisting() {
        // 创建并注册测试资产
        PaletteRE paletteRE = new PaletteRE();
        ResourceLocation name = new ResourceLocation("test", "test_palette");
        AssetRegistries.PALETTES.register(name, paletteRE);
        
        // getOrThrow应该成功返回
        assertDoesNotThrow(() -> {
            Palette palette = AssetRegistries.PALETTES.getOrThrow(mockWorld, "test:test_palette");
            assertNotNull(palette, "应该返回资产");
        });
    }

    /**
     * 测试getOrThrow - 不存在的资产
     */
    @Test
    void testGetOrThrowNonExistent() {
        // getOrThrow应该抛出异常
        assertThrows(Exception.class, () -> {
            AssetRegistries.PALETTES.getOrThrow(mockWorld, "nonexistent");
        }, "不存在的资产应该抛出异常");
    }

    /**
     * 测试注册表迭代
     */
    @Test
    void testRegistryIteration() {
        // 注册多个资产
        for (int i = 1; i <= 3; i++) {
            PaletteRE paletteRE = new PaletteRE();
            ResourceLocation name = new ResourceLocation("test", "palette_" + i);
            AssetRegistries.PALETTES.register(name, paletteRE);
        }
        
        // 验证可以迭代
        int count = 0;
        for (Palette palette : AssetRegistries.PALETTES.getIterable()) {
            count++;
        }
        
        assertEquals(3, count, "应该能迭代3个资产");
    }

    /**
     * 测试多个注册表独立性
     */
    @Test
    void testMultipleRegistriesIndependence() {
        // 在不同注册表中注册资产
        PaletteRE paletteRE = new PaletteRE();
        VariantRE variantRE = new VariantRE();
        
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "asset1"), paletteRE);
        AssetRegistries.VARIANTS.register(new ResourceLocation("test", "asset2"), variantRE);
        
        // 验证注册表独立
        assertEquals(1, AssetRegistries.PALETTES.size(), "调色板注册表应该有1个资产");
        assertEquals(1, AssetRegistries.VARIANTS.size(), "变体注册表应该有1个资产");
        assertEquals(0, AssetRegistries.CONDITIONS.size(), "条件注册表应该为空");
    }

    /**
     * 测试STUFF_BY_TAG索引
     */
    @Test
    void testStuffByTagIndex() {
        // 重置应该清空标签索引
        AssetRegistries.reset();
        assertTrue(AssetRegistries.STUFF_BY_TAG.isEmpty(), "标签索引应该为空");
        
        // 注意：实际的标签索引构建在load()方法中
        // 这里只测试索引结构的存在性
    }

    /**
     * 测试isLoaded状态
     */
    @Test
    void testIsLoadedStatus() {
        // 初始状态应该是未加载
        assertFalse(AssetRegistries.isLoaded(), "初始状态应该是未加载");
        
        // 注意：实际的加载状态在load()方法中设置
        // 这里只测试状态查询方法的存在性
    }

    /**
     * 测试isPredefinedLoaded状态
     */
    @Test
    void testIsPredefinedLoadedStatus() {
        // 初始状态应该是未加载
        assertFalse(AssetRegistries.isPredefinedLoaded(), "初始状态应该是未加载");
        
        // 注意：实际的加载状态在loadPredefinedStuff()方法中设置
        // 这里只测试状态查询方法的存在性
    }

    /**
     * 测试注册表大小
     */
    @Test
    void testRegistrySize() {
        // 初始大小应该为0
        assertEquals(0, AssetRegistries.PALETTES.size(), "初始大小应该为0");
        
        // 注册资产后大小应该增加
        PaletteRE paletteRE = new PaletteRE();
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "palette1"), paletteRE);
        assertEquals(1, AssetRegistries.PALETTES.size(), "注册后大小应该为1");
        
        // 再注册一个
        AssetRegistries.PALETTES.register(new ResourceLocation("test", "palette2"), paletteRE);
        assertEquals(2, AssetRegistries.PALETTES.size(), "再注册后大小应该为2");
    }

    /**
     * 测试重复注册
     */
    @Test
    void testDuplicateRegistration() {
        PaletteRE paletteRE1 = new PaletteRE();
        PaletteRE paletteRE2 = new PaletteRE();
        ResourceLocation name = new ResourceLocation("test", "duplicate");
        
        // 第一次注册
        AssetRegistries.PALETTES.register(name, paletteRE1);
        assertEquals(1, AssetRegistries.PALETTES.size(), "第一次注册后大小应该为1");
        
        // 第二次注册相同名称（应该覆盖）
        AssetRegistries.PALETTES.register(name, paletteRE2);
        assertEquals(1, AssetRegistries.PALETTES.size(), "重复注册不应该增加大小");
    }
}
