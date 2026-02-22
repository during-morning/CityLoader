package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.StyleRE;
import com.during.cityloader.worldgen.lost.regassets.VariantRE;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Variant/Style 兼容测试")
class VariantStyleCompatibilityTest {

    private final Gson gson = new GsonBuilder().create();

    @Test
    @DisplayName("Variant 应支持 LostCities blocks 格式")
    void variantShouldSupportBlocksFormat() {
        String json = """
                {
                  "blocks": [
                    {"random": 30, "block": "minecraft:stone"},
                    {"random": 98, "block": "minecraft:mossy_stone_bricks"}
                  ]
                }
                """;

        VariantRE re = gson.fromJson(json, VariantRE.class);
        re.setRegistryName(new ResourceLocation("lostcities", "stone_variant"));

        Variant variant = new Variant(re);

        assertEquals("lostcities:stone_variant", variant.getName(), "ID 应匹配");
        assertEquals(2, variant.getBlocks().size(), "应有2个方块条目");
        assertEquals("minecraft:stone", variant.getBlocks().get(0).block(), "首个方块应匹配");
    }

    @Test
    @DisplayName("Variant 应兼容旧 name/weight 格式")
    void variantShouldSupportLegacyFormat() {
        String json = """
                {
                  "name": "minecraft:stone",
                  "weight": 5.0
                }
                """;

        VariantRE re = gson.fromJson(json, VariantRE.class);
        re.setRegistryName(new ResourceLocation("legacy", "legacy_variant"));

        Variant variant = new Variant(re);

        assertEquals("minecraft:stone", variant.getVariantName(), "旧名称应保留");
        assertEquals(5.0f, variant.getWeight(), 0.001f, "旧权重应保留");
        assertEquals(1, variant.getBlocks().size(), "兼容模式应映射为1个方块条目");
        assertEquals("minecraft:stone", variant.getBlocks().get(0).block(), "兼容映射方块应匹配");
    }

    @Test
    @DisplayName("Style 应支持 randompalettes 格式")
    void styleShouldSupportRandomPalettesFormat() {
        String json = """
                {
                  "randompalettes": [
                    [
                      {"factor": 1.0, "palette": "lostcities:common"}
                    ],
                    [
                      {"factor": 2.0, "palette": "lostcities:stone"},
                      {"factor": 1.0, "palette": "lostcities:brick"}
                    ]
                  ]
                }
                """;

        StyleRE re = gson.fromJson(json, StyleRE.class);
        re.setRegistryName(new ResourceLocation("lostcities", "standard"));

        Style style = new Style(re);

        assertEquals("lostcities:standard", style.getName(), "ID 应匹配");
        assertEquals(2, style.getRandomPaletteChoices().size(), "应有2个随机组");

        List<String> picked = style.pickRandomPalettes(new Random(12345L));
        assertEquals(2, picked.size(), "每组应选出一个调色板");
        assertEquals("lostcities:common", picked.get(0), "第一组应固定选 common");
        assertTrue(
                "lostcities:stone".equals(picked.get(1)) || "lostcities:brick".equals(picked.get(1)),
                "第二组应在 stone/brick 中选择"
        );
    }

    @Test
    @DisplayName("Style 应兼容旧 palettes/weights 格式")
    void styleShouldSupportLegacyPalettesFormat() {
        String json = """
                {
                  "palettes": ["lostcities:p1", "lostcities:p2"],
                  "weights": [3.0, 7.0]
                }
                """;

        StyleRE re = gson.fromJson(json, StyleRE.class);
        re.setRegistryName(new ResourceLocation("legacy", "legacy_style"));

        Style style = new Style(re);

        assertEquals(2, style.getRandomPaletteChoices().size(), "旧格式应转换为2个随机组");
        assertEquals("lostcities:p1", style.getRandomPaletteChoices().get(0).get(0).palette(), "第一个调色板应匹配");
        assertEquals(3.0f, style.getRandomPaletteChoices().get(0).get(0).factor(), 0.001f, "第一个权重应匹配");
        assertEquals("lostcities:p2", style.getRandomPaletteChoices().get(1).get(0).palette(), "第二个调色板应匹配");
        assertEquals(7.0f, style.getRandomPaletteChoices().get(1).get(0).factor(), 0.001f, "第二个权重应匹配");
    }
}
