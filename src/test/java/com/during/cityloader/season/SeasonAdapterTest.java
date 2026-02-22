package com.during.cityloader.season;

import com.during.cityloader.config.PluginConfig;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 季节适配器测试
 * 测试DummySeasonAdapter和季节系统
 * 
 * @author During
 * @since 1.4.0
 */
public class SeasonAdapterTest {

    @Mock
    private PluginConfig config;

    @Mock
    private World world;

    private Logger logger;
    private SeasonAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logger = Logger.getLogger("TestLogger");

        // 配置默认季节
        when(config.getDefaultSeason()).thenReturn(Season.SPRING);
    }

    /**
     * 测试DummySeasonAdapter使用默认季节
     */
    @Test
    void testDummyAdapterUsesDefaultSeason() {
        adapter = new DummySeasonAdapter(logger, config);

        // 验证返回默认季节
        Season season = adapter.getCurrentSeason(world);
        assertEquals(Season.SPRING, season,
                "DummyAdapter应该返回默认季节SPRING");

        // 验证不可用
        assertFalse(adapter.isAvailable(),
                "DummyAdapter的isAvailable应该返回false");
    }

    /**
     * 测试Season枚举的fromString方法
     */
    @Test
    void testSeasonFromString() {
        // 测试有效的季节名称
        assertEquals(Season.SPRING, Season.fromString("SPRING"));
        assertEquals(Season.SUMMER, Season.fromString("SUMMER"));
        assertEquals(Season.AUTUMN, Season.fromString("AUTUMN"));
        assertEquals(Season.WINTER, Season.fromString("WINTER"));

        // 测试小写
        assertEquals(Season.SPRING, Season.fromString("spring"));
        assertEquals(Season.SUMMER, Season.fromString("summer"));

        // 测试无效输入
        assertEquals(Season.SPRING, Season.fromString("invalid"));
        assertEquals(Season.SPRING, Season.fromString(null));
        assertEquals(Season.SPRING, Season.fromString(""));
    }

    /**
     * 测试Season枚举的显示名称
     */
    @Test
    void testSeasonDisplayNames() {
        assertEquals("春季", Season.SPRING.getDisplayName());
        assertEquals("夏季", Season.SUMMER.getDisplayName());
        assertEquals("秋季", Season.AUTUMN.getDisplayName());
        assertEquals("冬季", Season.WINTER.getDisplayName());
    }

    /**
     * 测试所有季节枚举值
     */
    @Test
    void testAllSeasonValues() {
        Season[] seasons = Season.values();

        assertEquals(4, seasons.length, "应该有4个季节");

        // 验证所有季节都存在
        boolean hasSpring = false;
        boolean hasSummer = false;
        boolean hasAutumn = false;
        boolean hasWinter = false;

        for (Season season : seasons) {
            if (season == Season.SPRING)
                hasSpring = true;
            if (season == Season.SUMMER)
                hasSummer = true;
            if (season == Season.AUTUMN)
                hasAutumn = true;
            if (season == Season.WINTER)
                hasWinter = true;
        }

        assertTrue(hasSpring, "应该包含SPRING");
        assertTrue(hasSummer, "应该包含SUMMER");
        assertTrue(hasAutumn, "应该包含AUTUMN");
        assertTrue(hasWinter, "应该包含WINTER");
    }

    /**
     * 测试配置中不同的默认季节
     */
    @Test
    void testDifferentDefaultSeasons() {
        // 测试SUMMER作为默认季节
        when(config.getDefaultSeason()).thenReturn(Season.SUMMER);
        adapter = new DummySeasonAdapter(logger, config);

        Season season = adapter.getCurrentSeason(world);
        assertEquals(Season.SUMMER, season, "应该返回配置的默认季节SUMMER");

        // 测试WINTER作为默认季节
        when(config.getDefaultSeason()).thenReturn(Season.WINTER);
        adapter = new DummySeasonAdapter(logger, config);

        season = adapter.getCurrentSeason(world);
        assertEquals(Season.WINTER, season, "应该返回配置的默认季节WINTER");
    }

    /**
     * 测试Season枚举的valueOf方法
     */
    @Test
    void testSeasonValueOf() {
        assertEquals(Season.SPRING, Season.valueOf("SPRING"));
        assertEquals(Season.SUMMER, Season.valueOf("SUMMER"));
        assertEquals(Season.AUTUMN, Season.valueOf("AUTUMN"));
        assertEquals(Season.WINTER, Season.valueOf("WINTER"));
    }
}
