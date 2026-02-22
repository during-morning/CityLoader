package com.during.cityloader.worldgen.lost.regassets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Scattered 资产解析测试")
class ScatteredParsingTest {

    private final Gson gson = new GsonBuilder().create();

    @Test
    @DisplayName("应解析新格式 scattered 字段")
    void shouldParseModernScatteredFields() {
        String json = """
                {
                  "buildings": ["cabin", "radiotower"],
                  "multibuilding": "oilrig",
                  "terrainheight": "highest",
                  "terrainfix": "repeatslice",
                  "heightoffset": -3
                }
                """;

        ScatteredRE re = gson.fromJson(json, ScatteredRE.class);

        assertNotNull(re);
        assertEquals(List.of("cabin", "radiotower"), re.getBuildings());
        assertEquals("oilrig", re.getMultiBuilding());
        assertEquals("highest", re.getTerrainHeight());
        assertEquals("repeatslice", re.getTerrainFix());
        assertEquals(-3, re.getHeightOffset());
    }

    @Test
    @DisplayName("应兼容旧格式 scattered 字段")
    void shouldParseLegacyScatteredFields() {
        String json = """
                {
                  "building": "cabin",
                  "chance": 0.25
                }
                """;

        ScatteredRE re = gson.fromJson(json, ScatteredRE.class);

        assertNotNull(re);
        assertEquals("cabin", re.getBuilding());
        assertEquals(0.25f, re.getChance());
    }
}
