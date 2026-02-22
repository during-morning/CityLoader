package com.during.cityloader.worldgen.lost.regassets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorldStyle/CityStyle 解析测试")
class WorldCityStyleParsingTest {

    private final Gson gson = new GsonBuilder().create();

    @Test
    @DisplayName("WorldStyleRE 应解析完整 worldstyle 字段")
    void shouldParseWorldStyleExtendedFields() {
        String json = """
                {
                  "outsidestyle": "outside",
                  "settings": {"railwayavoidance": "ignore", "citychance": 0.6},
                  "multisettings": {"areasize": 10, "minimum": 1, "maximum": 4, "attempts": 20},
                  "parts": {"bridge": "bridge_a", "railsflat": "rail_flat_a"},
                  "citystyles": [{"factor": 1.0, "citystyle": "citystyle_standard"}],
                  "citybiomemultipliers": [{"multiplier": 0.5, "biomes": {"if_any": ["minecraft:ocean"]}}]
                }
                """;

        WorldStyleRE re = gson.fromJson(json, WorldStyleRE.class);

        assertNotNull(re);
        assertEquals("outside", re.getOutsideStyle());
        assertNotNull(re.getSettings());
        assertEquals("ignore", re.getSettings().getRailwayAvoidance());
        assertEquals(0.6f, re.getSettings().getCityChance(), 0.0001f);
        assertNotNull(re.getMultiSettings());
        assertEquals(10, re.getMultiSettings().getAreaSize());
        assertNotNull(re.getParts());
        assertEquals("bridge_a", re.getParts().getBridge());
        assertEquals(1, re.getCityStyleSelectors().size());
        assertEquals(1, re.getCityBiomeMultipliers().size());
    }

    @Test
    @DisplayName("WorldStyleRE 应解析嵌套 highways/railways parts")
    void shouldParseNestedWorldPartGroups() {
        String json = """
                {
                  "parts": {
                    "highways": {
                      "bridge": ["demo:highway_bridge"],
                      "open": ["demo:highway_open"]
                    },
                    "railways": {
                      "railshorizontal": ["demo:rails_horizontal"],
                      "railsvertical": ["demo:rails_vertical"],
                      "railsflat": ["demo:rails_flat"]
                    }
                  }
                }
                """;

        WorldStyleRE re = gson.fromJson(json, WorldStyleRE.class);

        assertNotNull(re);
        assertNotNull(re.getParts());
        assertEquals("demo:highway_bridge", re.getParts().getBridge());
        assertEquals("demo:highway_open", re.getParts().getOpen());
        assertEquals("demo:rails_horizontal", re.getParts().getRailsHorizontal());
        assertEquals("demo:rails_vertical", re.getParts().getRailsVertical());
        assertEquals("demo:rails_flat", re.getParts().getRailsFlat());
    }

    @Test
    @DisplayName("CityStyleRE 应解析 selectors/inherit/stuff_tags")
    void shouldParseCityStyleExtendedFields() {
        String json = """
                {
                  "inherit": "citystyle_common",
                  "style": "standard",
                  "stuff_tags": ["rubble"],
                  "explosionchance": 0.25,
                  "streetblocks": {"street": "S", "streetbase": "b", "width": 8},
                  "railblocks": {"railmain": "~"},
                  "selectors": {
                    "buildings": [{"factor": 0.7, "value": "building_a"}],
                    "multibuildings": [{"factor": 0.3, "value": "multi_a"}]
                  }
                }
                """;

        CityStyleRE re = gson.fromJson(json, CityStyleRE.class);

        assertNotNull(re);
        assertEquals("citystyle_common", re.getInherit());
        assertEquals("standard", re.getStyle());
        assertEquals(0.25f, re.getExplosionChance(), 0.0001f);
        assertEquals(1, re.getStuffTags().size());
        assertEquals("rubble", re.getStuffTags().get(0));
        assertNotNull(re.getStreetBlocks());
        assertEquals("S", re.getStreetBlocks().getStreet());
        assertEquals(8, re.getStreetBlocks().getWidth());
        assertNotNull(re.getRailBlocks());
        assertEquals("~", re.getRailBlocks().getRailMain());
        assertNotNull(re.getSelectors());
        assertEquals(1, re.getSelectors().getBuildings().size());
        assertEquals("building_a", re.getSelectors().getBuildings().get(0).getValue());
    }
}
