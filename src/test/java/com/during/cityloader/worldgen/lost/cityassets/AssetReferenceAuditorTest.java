package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.during.cityloader.worldgen.lost.regassets.StyleRE;
import com.during.cityloader.worldgen.lost.regassets.WorldStyleRE;
import com.google.gson.Gson;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("AssetReferenceAuditor 资产引用审计测试")
class AssetReferenceAuditorTest {
    private static final Gson GSON = new Gson();

    @AfterEach
    void tearDown() {
        AssetRegistries.reset();
    }

    @Test
    @DisplayName("应能检测 worldstyle 中缺失的部件引用")
    void shouldDetectMissingWorldStylePartReference() {
        World world = mock(World.class);

        WorldStyleRE worldStyleRE = new Gson().fromJson("""
                {
                  "parts": {
                    "highways": {
                      "open": ["test:missing_highway_open"]
                    }
                  },
                  "citystyles": [
                    {"factor": 1.0, "citystyle": "lostcities:citystyle_standard"}
                  ]
                }
                """, WorldStyleRE.class);
        AssetRegistries.WORLDSTYLES.register(new ResourceLocation("test", "audit_missing"), worldStyleRE);

        AssetReferenceAuditor.AuditReport report = AssetReferenceAuditor.audit(world);
        assertTrue(report.getMissingReferences() > 0, "应报告至少一条缺失引用");
    }

    @Test
    @DisplayName("style building 选择器应允许引用 part")
    void shouldAcceptStyleBuildingSelectorReferencingPart() {
        World world = mock(World.class);

        BuildingPartRE partRE = GSON.fromJson("""
                {
                  "xsize": 16,
                  "zsize": 16,
                  "slices": [
                    [
                      "                "
                    ]
                  ]
                }
                """, BuildingPartRE.class);
        AssetRegistries.PARTS.register(new ResourceLocation("test", "building_selector_part"), partRE);

        StyleRE styleRE = GSON.fromJson("""
                {
                  "selectors": {
                    "building": [
                      {"factor": 1.0, "value": "building_selector_part"}
                    ]
                  }
                }
                """, StyleRE.class);
        AssetRegistries.STYLES.register(new ResourceLocation("test", "style_selector_part"), styleRE);

        AssetReferenceAuditor.AuditReport report = AssetReferenceAuditor.audit(world);
        assertEquals(0, report.getMissingReferences(), "style building 选择器引用 part 不应被误报");
    }

    @Test
    @DisplayName("style building 选择器引用无效值仍应报错")
    void shouldReportMissingWhenStyleBuildingSelectorResolvesToNothing() {
        World world = mock(World.class);

        StyleRE styleRE = GSON.fromJson("""
                {
                  "selectors": {
                    "building": [
                      {"factor": 1.0, "value": "not_exists_anywhere"}
                    ]
                  }
                }
                """, StyleRE.class);
        AssetRegistries.STYLES.register(new ResourceLocation("test", "style_selector_missing"), styleRE);

        AssetReferenceAuditor.AuditReport report = AssetReferenceAuditor.audit(world);
        assertTrue(report.getMissingReferences() > 0, "style building 选择器应继续报告真实缺失");
    }
}
