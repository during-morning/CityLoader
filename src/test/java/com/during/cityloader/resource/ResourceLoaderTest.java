package com.during.cityloader.resource;

import com.during.cityloader.resource.loader.ResourceLoader;
import com.during.cityloader.resource.registry.BuildingRegistry;
import com.during.cityloader.resource.registry.PaletteRegistry;
import com.during.cityloader.resource.registry.PartRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源加载器测试
 * 测试资源加载是否正常工作
 * 
 * @deprecated 此测试使用旧的resource.*包系统，正在迁移到新的AssetRegistries系统
 * @see com.during.cityloader.worldgen.lost.cityassets.AssetRegistriesTest
 * 
 * @author During
 * @since 1.4.0
 */
@Disabled("正在迁移到新的AssetRegistries系统 - 见任务9")
public class ResourceLoaderTest {

    private ResourceLoader loader;
    private PaletteRegistry paletteRegistry;
    private PartRegistry partRegistry;
    private BuildingRegistry buildingRegistry;
    private Logger logger;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("TestLogger");
        loader = new ResourceLoader(logger);
        paletteRegistry = new PaletteRegistry();
        partRegistry = new PartRegistry();
        buildingRegistry = new BuildingRegistry();
    }

    /**
     * 测试加载有效的调色板
     */
    @Test
    void testLoadValidPalette() throws IOException {
        // 创建测试调色板文件
        File paletteDir = tempDir.resolve("palettes").toFile();
        paletteDir.mkdirs();

        File paletteFile = new File(paletteDir, "test_palette.json");
        try (FileWriter writer = new FileWriter(paletteFile)) {
            writer.write("{\n");
            writer.write("  \"id\": \"test_palette\",\n");
            writer.write("  \"blocks\": {\n");
            writer.write("    \"w\": \"STONE\",\n");
            writer.write("    \"g\": \"GLASS\"\n");
            writer.write("  }\n");
            writer.write("}");
        }

        // 加载调色板
        int loaded = loader.loadPalettes(paletteDir, paletteRegistry, "test");

        // 验证
        assertEquals(1, loaded, "应该加载1个调色板");
        assertTrue(paletteRegistry.contains("test_palette"), "注册表应该包含test_palette");

        Palette palette = paletteRegistry.get("test_palette");
        assertNotNull(palette, "调色板不应为null");
        assertEquals("test_palette", palette.getId(), "调色板ID应该匹配");
    }

    /**
     * 测试加载有效的部件
     */
    @Test
    void testLoadValidPart() throws IOException {
        // 创建测试部件文件
        File partDir = tempDir.resolve("parts").toFile();
        partDir.mkdirs();

        File partFile = new File(partDir, "test_part.json");
        try (FileWriter writer = new FileWriter(partFile)) {
            writer.write("{\n");
            writer.write("  \"id\": \"test_part\",\n");
            writer.write("  \"palettes\": [\"test_palette\"],\n");
            writer.write("  \"width\": 3,\n");
            writer.write("  \"height\": 2,\n");
            writer.write("  \"depth\": 3,\n");
            writer.write("  \"structure\": [\n");
            writer.write("    [\"www\", \"w.w\", \"www\"],\n");
            writer.write("    [\"www\", \"w.w\", \"www\"]\n");
            writer.write("  ]\n");
            writer.write("}");
        }

        // 加载部件
        int loaded = loader.loadParts(partDir, partRegistry, "test");

        // 验证
        assertEquals(1, loaded, "应该加载1个部件");
        assertTrue(partRegistry.contains("test_part"), "注册表应该包含test_part");

        Part part = partRegistry.get("test_part");
        assertNotNull(part, "部件不应为null");
        assertEquals("test_part", part.getId(), "部件ID应该匹配");
        assertEquals(3, part.getWidth(), "宽度应该为3");
        assertEquals(2, part.getHeight(), "高度应该为2");
        assertEquals(3, part.getDepth(), "深度应该为3");
    }

    /**
     * 测试加载有效的建筑
     */
    @Test
    void testLoadValidBuilding() throws IOException {
        // 创建测试建筑文件
        File buildingDir = tempDir.resolve("buildings").toFile();
        buildingDir.mkdirs();

        File buildingFile = new File(buildingDir, "test_building.json");
        try (FileWriter writer = new FileWriter(buildingFile)) {
            writer.write("{\n");
            writer.write("  \"id\": \"test_building\",\n");
            writer.write("  \"type\": \"residential\",\n");
            writer.write("  \"weight\": 10.0,\n");
            writer.write("  \"minFloors\": 3,\n");
            writer.write("  \"maxFloors\": 5,\n");
            writer.write("  \"parts\": [\n");
            writer.write("    {\n");
            writer.write("      \"partId\": \"test_part\",\n");
            writer.write("      \"offsetX\": 0,\n");
            writer.write("      \"offsetY\": 0,\n");
            writer.write("      \"offsetZ\": 0\n");
            writer.write("    }\n");
            writer.write("  ]\n");
            writer.write("}");
        }

        // 加载建筑
        int loaded = loader.loadBuildings(buildingDir, buildingRegistry, "test");

        // 验证
        assertEquals(1, loaded, "应该加载1个建筑");
        assertTrue(buildingRegistry.contains("test_building"), "注册表应该包含test_building");

        Building building = buildingRegistry.get("test_building");
        assertNotNull(building, "建筑不应为null");
        assertEquals("test_building", building.getId(), "建筑ID应该匹配");
        assertEquals("residential", building.getType(), "建筑类型应该为residential");
        assertEquals(3, building.getMinFloors(), "最小楼层应该为3");
        assertEquals(5, building.getMaxFloors(), "最大楼层应该为5");
    }

    /**
     * 测试加载格式错误的JSON文件
     */
    @Test
    void testLoadMalformedJson() throws IOException {
        // 创建格式错误的调色板文件
        File paletteDir = tempDir.resolve("palettes").toFile();
        paletteDir.mkdirs();

        File paletteFile = new File(paletteDir, "malformed.json");
        try (FileWriter writer = new FileWriter(paletteFile)) {
            writer.write("{ invalid json }");
        }

        // 加载调色板（应该跳过错误文件）
        int loaded = loader.loadPalettes(paletteDir, paletteRegistry, "test");

        // 验证：应该加载0个调色板，但不应该抛出异常
        assertEquals(0, loaded, "格式错误的文件应该被跳过");
    }

    /**
     * 测试加载不存在的目录
     */
    @Test
    void testLoadNonExistentDirectory() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent");

        // 加载不存在的目录
        int loaded = loader.loadPalettes(nonExistent, paletteRegistry, "test");

        // 验证：应该返回0，但不应该抛出异常
        assertEquals(0, loaded, "不存在的目录应该返回0");
    }

    /**
     * 测试加载空目录
     */
    @Test
    void testLoadEmptyDirectory() {
        File emptyDir = tempDir.resolve("empty").toFile();
        emptyDir.mkdirs();

        // 加载空目录
        int loaded = loader.loadPalettes(emptyDir, paletteRegistry, "test");

        // 验证
        assertEquals(0, loaded, "空目录应该返回0");
    }

    /**
     * 测试加载多个资源文件
     */
    @Test
    void testLoadMultipleResources() throws IOException {
        // 创建多个调色板文件
        File paletteDir = tempDir.resolve("palettes").toFile();
        paletteDir.mkdirs();

        for (int i = 1; i <= 3; i++) {
            File paletteFile = new File(paletteDir, "palette_" + i + ".json");
            try (FileWriter writer = new FileWriter(paletteFile)) {
                writer.write("{\n");
                writer.write("  \"id\": \"palette_" + i + "\",\n");
                writer.write("  \"blocks\": {\n");
                writer.write("    \"w\": \"STONE\"\n");
                writer.write("  }\n");
                writer.write("}");
            }
        }

        // 加载调色板
        int loaded = loader.loadPalettes(paletteDir, paletteRegistry, "test");

        // 验证
        assertEquals(3, loaded, "应该加载3个调色板");
        assertEquals(3, paletteRegistry.size(), "注册表应该包含3个调色板");
    }
}
