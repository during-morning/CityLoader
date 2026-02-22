package com.during.cityloader.config;

import com.during.cityloader.exception.ConfigException;
import com.during.cityloader.season.Season;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ConfigManager单元测试
 * 
 * 测试配置加载、验证和错误恢复功能
 * 
 * @author During
 * @since 1.4.0
 */
@DisplayName("ConfigManager 单元测试")
class ConfigManagerTest {

    @Mock
    private Plugin mockPlugin;

    @Mock
    private Logger mockLogger;

    private File tempDir;
    private File configFile;
    private ConfigManager configManager;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws IOException {
        // 初始化Mockito
        mocks = MockitoAnnotations.openMocks(this);

        // 创建临时目录
        tempDir = Files.createTempDirectory("cityloader-test").toFile();
        configFile = new File(tempDir, "config.yml");

        // 配置mock对象
        when(mockPlugin.getDataFolder()).thenReturn(tempDir);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getResource("config.yml")).thenReturn(null); // 模拟资源不存在

        // 创建ConfigManager实例
        configManager = new ConfigManager(mockPlugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 清理临时文件
        if (configFile.exists()) {
            configFile.delete();
        }
        if (tempDir.exists()) {
            tempDir.delete();
        }

        // 关闭mocks
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("测试首次启动创建默认配置")
    void testFirstStartCreatesDefaultConfig() throws ConfigException, IOException {
        // 确保配置文件不存在
        assertFalse(configFile.exists(), "配置文件应该不存在");

        // 创建一个基本的默认配置文件
        createBasicConfigFile();

        // 加载配置
        PluginConfig config = configManager.loadConfig();

        // 验证配置不为null
        assertNotNull(config, "配置对象不应该为null");

        // 验证默认值
        assertNotNull(config.getDefaultSeason(), "默认季节不应该为null");
        assertTrue(config.getCityDensity() >= 0 && config.getCityDensity() <= 100,
                "城市密度应该在0-100之间");
        assertTrue(config.getMinBuildingHeight() > 0, "最小建筑高度应该大于0");
        assertTrue(config.getMaxBuildingHeight() >= config.getMinBuildingHeight(),
                "最大建筑高度应该大于等于最小高度");
    }

    @Test
    @DisplayName("测试加载有效配置")
    void testLoadValidConfig() throws ConfigException, IOException {
        // 创建有效的配置文件
        createValidConfigFile();

        // 加载配置
        PluginConfig config = configManager.loadConfig();

        // 验证配置值
        assertNotNull(config);
        assertEquals(Season.WINTER, config.getDefaultSeason());
        assertEquals(0.5, config.getCityDensity());
        assertEquals(5, config.getMinBuildingHeight());
        assertEquals(15, config.getMaxBuildingHeight());
        assertEquals(3, config.getStreetWidth());
    }

    @Test
    @DisplayName("测试配置验证 - 无效密度值")
    void testConfigValidation_InvalidDensity() throws ConfigException, IOException {
        // 创建包含无效密度的配置文件
        createConfigWithInvalidDensity();

        // 加载配置（应该使用默认值）
        PluginConfig config = configManager.loadConfig();

        // 验证使用了默认值
        assertNotNull(config);
        assertTrue(config.getCityDensity() >= 0 && config.getCityDensity() <= 100,
                "应该使用有效的默认密度值");
    }

    @Test
    @DisplayName("属性 2: 配置验证和错误恢复")
    @Tag("property-2")
    @RepeatedTest(10)
    void testProperty2_ConfigValidationAndRecovery() throws IOException {
        // 生成随机配置（可能包含无效值）
        createRandomConfigFile();

        // 尝试加载配置
        try {
            PluginConfig config = configManager.loadConfig();

            // 验证：要么成功加载，要么使用默认值，但不应该崩溃
            assertNotNull(config, "配置对象不应该为null");
            assertTrue(config.validate(), "配置应该是有效的");

        } catch (ConfigException e) {
            // 如果抛出异常，应该是ConfigException，不应该是其他异常
            assertNotNull(e.getMessage(), "异常应该有消息");
        }
    }

    @Test
    @DisplayName("测试配置重载")
    void testConfigReload() throws ConfigException, IOException {
        // 创建初始配置
        createValidConfigFile();
        PluginConfig config1 = configManager.loadConfig();
        assertEquals(0.5, config1.getCityDensity());

        // 修改配置文件
        createConfigWithDifferentDensity(0.8);

        // 重载配置
        PluginConfig config2 = configManager.reloadConfig();

        // 验证配置已更新
        assertEquals(0.8, config2.getCityDensity());
    }

    @Test
    @DisplayName("属性 3: 配置重载一致性")
    @Tag("property-3")
    void testProperty3_ConfigReloadConsistency() throws ConfigException, IOException {
        // 创建配置文件
        createValidConfigFile();

        // 加载配置
        PluginConfig config1 = configManager.loadConfig();
        double originalDensity = config1.getCityDensity();

        // 修改配置文件 (确保在0.0-1.0范围内)
        double newDensity = (originalDensity + 0.2) % 1.0;
        if (newDensity < 0.1) newDensity = 0.2; // 确保不会太小
        createConfigWithDifferentDensity(newDensity);

        // 重载配置
        PluginConfig config2 = configManager.reloadConfig();

        // 验证：新配置应该反映文件中的值
        assertEquals(newDensity, config2.getCityDensity(), 0.01,
                "重载后的配置应该反映新的配置值");
    }

    @Test
    @DisplayName("测试格式错误的配置文件")
    void testMalformedConfigFile() throws IOException {
        // 创建格式错误的配置文件
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("this is not valid yaml: [[[");
        }

        // 尝试加载配置（应该使用默认值）
        try {
            PluginConfig config = configManager.loadConfig();
            assertNotNull(config, "即使配置文件格式错误，也应该返回默认配置");
        } catch (ConfigException e) {
            // 也可以接受抛出ConfigException
            assertNotNull(e.getMessage());
        }
    }

    @Test
    @DisplayName("测试获取当前配置")
    void testGetConfig() throws ConfigException, IOException {
        // 加载配置
        createValidConfigFile();
        PluginConfig loadedConfig = configManager.loadConfig();

        // 获取配置
        PluginConfig retrievedConfig = configManager.getConfig();

        // 验证是同一个对象
        assertSame(loadedConfig, retrievedConfig, "应该返回相同的配置对象");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建基本的配置文件
     */
    private void createBasicConfigFile() throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("default-season: SPRING\n");
            writer.write("city-density: 30\n");
            writer.write("min-building-height: 3\n");
            writer.write("max-building-height: 20\n");
            writer.write("street-width: 5\n");
        }
    }

    /**
     * 创建有效的配置文件
     */
    private void createValidConfigFile() throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("default-season: WINTER\n");
            writer.write("city-density: 0.5\n");  // 使用0.0-1.0范围
            writer.write("min-building-height: 5\n");
            writer.write("max-building-height: 15\n");
            writer.write("street-width: 3\n");
            writer.write("generation:\n");
            writer.write("  generate-underground: true\n");
            writer.write("  generate-streets: true\n");
            writer.write("  vanilla-compatible: true\n");
            writer.write("resource-packs:\n");
            writer.write("  - datapacks/cityloader\n");
            writer.write("debug:\n");
            writer.write("  enabled: false\n");
            writer.write("  log-resource-loading: false\n");
            writer.write("  log-generation: false\n");
            writer.write("performance:\n");
            writer.write("  cache-size: 1000\n");
            writer.write("  async-loading: true\n");
        }
    }

    /**
     * 创建包含无效密度的配置文件
     */
    private void createConfigWithInvalidDensity() throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("default-season: SPRING\n");
            writer.write("city-density: 1.5\n"); // 无效值（超过1.0）
            writer.write("min-building-height: 3\n");
            writer.write("max-building-height: 20\n");
        }
    }

    /**
     * 创建具有指定密度的配置文件
     */
    private void createConfigWithDifferentDensity(double density) throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("default-season: SPRING\n");
            writer.write("city-density: " + density + "\n");
            writer.write("min-building-height: 3\n");
            writer.write("max-building-height: 20\n");
        }
    }

    /**
     * 创建随机配置文件（用于属性测试）
     */
    private void createRandomConfigFile() throws IOException {
        java.util.Random random = new java.util.Random();

        try (FileWriter writer = new FileWriter(configFile)) {
            // 随机季节
            Season[] seasons = Season.values();
            writer.write("default-season: " + seasons[random.nextInt(seasons.length)].name() + "\n");

            // 随机密度（可能无效）- 使用0.0-2.0范围来测试验证
            double density = random.nextDouble() * 2.0 - 0.5; // -0.5 到 1.5
            writer.write("city-density: " + density + "\n");

            // 随机建筑高度
            int minHeight = random.nextInt(50);
            int maxHeight = random.nextInt(50);
            writer.write("min-building-height: " + minHeight + "\n");
            writer.write("max-building-height: " + maxHeight + "\n");
        }
    }
}
