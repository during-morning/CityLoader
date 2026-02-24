package com.during.cityloader.worldgen.lost.regassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedBuilding;
import com.during.cityloader.worldgen.lost.regassets.data.PredefinedStreet;
import com.during.cityloader.worldgen.lost.regassets.data.PartRef;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资产解析单元测试
 * 验证能否正确解析建筑、调色板和多建筑JSON数据
 * 
 * @author During
 * @since 1.4.0
 */
@DisplayName("资产解析测试")
class AssetParsingTest {
    
    private Gson gson;
    
    @BeforeEach
    void setUp() {
        gson = new GsonBuilder().create();
    }
    
    // ==================== 调色板测试 ====================
    
    @Test
    @DisplayName("应该正确解析新格式调色板JSON（PaletteEntry数组）")
    void shouldParseNewFormatPaletteJson() {
        String json = """
            {
              "name": "test_palette",
              "palette": [
                {
                  "char": "A",
                  "block": "minecraft:stone"
                },
                {
                  "char": "B",
                  "variant": "asphalt"
                },
                {
                  "char": "C",
                  "block": "minecraft:grass_block",
                  "damaged": "minecraft:dirt"
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals("test_palette", palette.getName(), "调色板名称应匹配");
        assertNotNull(palette.getPaletteEntries(), "调色板条目列表不应为null");
        assertEquals(3, palette.getPaletteEntries().size(), "应有3个条目");
        
        assertEquals("A", palette.getPaletteEntries().get(0).getCharacter(), "第一个条目字符应为A");
        assertEquals("minecraft:stone", palette.getPaletteEntries().get(0).getBlock(), "第一个条目方块应匹配");
        
        assertEquals("B", palette.getPaletteEntries().get(1).getCharacter(), "第二个条目字符应为B");
        assertEquals("asphalt", palette.getPaletteEntries().get(1).getVariant(), "第二个条目变体应匹配");
        
        assertEquals("C", palette.getPaletteEntries().get(2).getCharacter(), "第三个条目字符应为C");
        assertEquals("minecraft:grass_block", palette.getPaletteEntries().get(2).getBlock(), "第三个条目方块应匹配");
        assertEquals("minecraft:dirt", palette.getPaletteEntries().get(2).getDamaged(), "第三个条目损坏方块应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析带随机方块选择的调色板JSON")
    void shouldParsePaletteWithBlockChoicesJson() {
        String json = """
            {
              "name": "random_palette",
              "palette": [
                {
                  "char": ":",
                  "blocks": [
                    {
                      "random": 31,
                      "block": "minecraft:iron_bars"
                    },
                    {
                      "random": 31,
                      "block": "minecraft:weathered_iron_bars"
                    },
                    {
                      "random": 1000,
                      "block": "minecraft:air"
                    }
                  ]
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals(1, palette.getPaletteEntries().size(), "应有1个条目");
        
        var entry = palette.getPaletteEntries().get(0);
        assertEquals(":", entry.getCharacter(), "字符应为:");
        assertNotNull(entry.getBlocks(), "方块选择列表不应为null");
        assertEquals(3, entry.getBlocks().size(), "应有3个方块选择");
        
        assertEquals(31, entry.getBlocks().get(0).getRandom(), "第一个选择权重应为31");
        assertEquals("minecraft:iron_bars", entry.getBlocks().get(0).getBlock(), "第一个选择方块应匹配");
        
        assertEquals(1000, entry.getBlocks().get(2).getRandom(), "第三个选择权重应为1000");
        assertEquals("minecraft:air", entry.getBlocks().get(2).getBlock(), "第三个选择方块应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析带NBT标签的调色板JSON")
    void shouldParsePaletteWithNbtTagJson() {
        String json = """
            {
              "name": "nbt_palette",
              "palette": [
                {
                  "char": ";",
                  "block": "minecraft:furnace",
                  "damaged": "minecraft:iron_bars",
                  "tag": {
                    "Items": [
                      {
                        "Slot": 0,
                        "id": "minecraft:coal",
                        "Count": 10
                      }
                    ]
                  }
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals(1, palette.getPaletteEntries().size(), "应有1个条目");
        
        var entry = palette.getPaletteEntries().get(0);
        assertEquals(";", entry.getCharacter(), "字符应为;");
        assertEquals("minecraft:furnace", entry.getBlock(), "方块应为furnace");
        assertNotNull(entry.getTag(), "NBT标签不应为null");
        assertTrue(entry.getTag().containsKey("Items"), "NBT标签应包含Items");
    }
    
    @Test
    @DisplayName("应该正确解析带所有字段的调色板条目JSON")
    void shouldParsePaletteEntryWithAllFieldsJson() {
        String json = """
            {
              "name": "complete_palette",
              "palette": [
                {
                  "char": "S",
                  "block": "minecraft:spawner",
                  "variant": "spawner_variant",
                  "frompalette": "lostcities:common",
                  "damaged": "minecraft:iron_bars",
                  "mob": "minecraft:zombie",
                  "loot": "minecraft:chests/simple_dungeon",
                  "torch": true,
                  "tag": {
                    "SpawnData": {
                      "entity": {
                        "id": "minecraft:zombie"
                      }
                    }
                  }
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals(1, palette.getPaletteEntries().size(), "应有1个条目");
        
        var entry = palette.getPaletteEntries().get(0);
        assertEquals("S", entry.getCharacter(), "字符应为S");
        assertEquals("minecraft:spawner", entry.getBlock(), "方块应匹配");
        assertEquals("spawner_variant", entry.getVariant(), "变体应匹配");
        assertEquals("lostcities:common", entry.getFromPalette(), "来源调色板应匹配");
        assertEquals("minecraft:iron_bars", entry.getDamaged(), "损坏方块应匹配");
        assertEquals("minecraft:zombie", entry.getMob(), "生物类型应匹配");
        assertEquals("minecraft:chests/simple_dungeon", entry.getLoot(), "战利品表应匹配");
        assertEquals(true, entry.getTorch(), "火把标志应为true");
        assertNotNull(entry.getTag(), "NBT标签不应为null");
    }
    
    @Test
    @DisplayName("应该通过getEntry方法快速查找条目")
    void shouldFindEntryByCharacter() {
        String json = """
            {
              "name": "lookup_palette",
              "palette": [
                {
                  "char": "A",
                  "block": "minecraft:stone"
                },
                {
                  "char": "B",
                  "block": "minecraft:dirt"
                },
                {
                  "char": "C",
                  "block": "minecraft:grass_block"
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        var entryA = palette.getEntry("A");
        assertNotNull(entryA, "应找到字符A的条目");
        assertEquals("minecraft:stone", entryA.getBlock(), "字符A应映射到stone");
        
        var entryB = palette.getEntry("B");
        assertNotNull(entryB, "应找到字符B的条目");
        assertEquals("minecraft:dirt", entryB.getBlock(), "字符B应映射到dirt");
        
        var entryX = palette.getEntry("X");
        assertNull(entryX, "不存在的字符应返回null");
    }
    
    @Test
    @DisplayName("应该正确解析简单调色板JSON（向后兼容测试）")
    void shouldParseSimplePaletteJson() {
        String json = """
            {
              "name": "test_palette",
              "palette": [
                {
                  "char": "A",
                  "block": "minecraft:stone"
                },
                {
                  "char": "B",
                  "block": "minecraft:dirt"
                },
                {
                  "char": "C",
                  "block": "minecraft:grass_block"
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals("test_palette", palette.getName(), "调色板名称应匹配");
        
        // 测试向后兼容的getPalette方法
        Map<String, String> legacyMap = palette.getPalette();
        assertNotNull(legacyMap, "调色板映射不应为null");
        assertEquals(3, legacyMap.size(), "应有3个映射");
        assertEquals("minecraft:stone", legacyMap.get("A"), "字符A应映射到stone");
        assertEquals("minecraft:dirt", legacyMap.get("B"), "字符B应映射到dirt");
        assertEquals("minecraft:grass_block", legacyMap.get("C"), "字符C应映射到grass_block");
    }
    
    @Test
    @DisplayName("应该正确解析空调色板JSON")
    void shouldParseEmptyPaletteJson() {
        String json = """
            {
              "name": "empty_palette",
              "palette": []
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals("empty_palette", palette.getName(), "调色板名称应匹配");
        assertNotNull(palette.getPaletteEntries(), "调色板条目列表不应为null");
        assertTrue(palette.getPaletteEntries().isEmpty(), "调色板条目列表应为空");
    }
    
    @Test
    @DisplayName("应该正确解析复杂调色板JSON（带方块状态）")
    void shouldParseComplexPaletteJson() {
        String json = """
            {
              "name": "complex_palette",
              "palette": [
                {
                  "char": "S",
                  "block": "minecraft:stone_stairs[facing=north,half=bottom]"
                },
                {
                  "char": "D",
                  "block": "minecraft:oak_door[facing=east,half=lower,hinge=left]"
                },
                {
                  "char": "W",
                  "block": "minecraft:oak_fence[east=true,west=true]"
                }
              ]
            }
            """;
        
        PaletteRE palette = gson.fromJson(json, PaletteRE.class);
        
        assertNotNull(palette, "调色板不应为null");
        assertEquals(3, palette.getPaletteEntries().size(), "应有3个条目");
        assertTrue(palette.getEntry("S").getBlock().contains("stairs"), "应包含楼梯方块");
        assertTrue(palette.getEntry("D").getBlock().contains("door"), "应包含门方块");
        assertTrue(palette.getEntry("W").getBlock().contains("fence"), "应包含栅栏方块");
    }
    
    @Test
    @DisplayName("应该处理调色板注册名称")
    void shouldHandlePaletteRegistryName() {
        PaletteRE palette = new PaletteRE();
        palette.setName("test");
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_palette");
        palette.setRegistryName(location);
        
        assertEquals(location, palette.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== 建筑测试 ====================
    
    @Test
    @DisplayName("应该正确解析简单建筑JSON")
    void shouldParseSimpleBuildingJson() {
        String json = """
            {
              "minfloors": 3,
              "maxfloors": 8,
              "mincellars": 0,
              "maxcellars": 2,
              "refpalette": "lostcities:default",
              "parts": [
                {
                  "part": "lostcities:floor_basic"
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(3, building.getMinFloors(), "最小楼层应为3");
        assertEquals(8, building.getMaxFloors(), "最大楼层应为8");
        assertEquals(0, building.getMinCellars(), "最小地下室应为0");
        assertEquals(2, building.getMaxCellars(), "最大地下室应为2");
        assertEquals("lostcities:default", building.getRefPalette(), "引用调色板应匹配");
        assertNotNull(building.getParts(), "部件列表不应为null");
        assertEquals(1, building.getParts().size(), "应有1个部件");
    }
    
    @Test
    @DisplayName("应该正确解析带条件的建筑JSON")
    void shouldParseBuildingWithConditionsJson() {
        String json = """
            {
              "minfloors": 5,
              "maxfloors": 10,
              "parts": [
                {
                  "part": "lostcities:floor_basic",
                  "factor": 10
                },
                {
                  "part": "lostcities:floor_fancy",
                  "factor": 5,
                  "conditions": [
                    {
                      "condition": "lostcities:top"
                    }
                  ]
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(5, building.getMinFloors(), "最小楼层应为5");
        assertEquals(10, building.getMaxFloors(), "最大楼层应为10");
        assertEquals(2, building.getParts().size(), "应有2个部件");
        
        PartRef firstPart = building.getParts().get(0);
        assertEquals("lostcities:floor_basic", firstPart.getPart(), "第一个部件名称应匹配");
        
        PartRef secondPart = building.getParts().get(1);
        assertEquals("lostcities:floor_fancy", secondPart.getPart(), "第二个部件名称应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析带parts2的建筑JSON")
    void shouldParseBuildingWithParts2Json() {
        String json = """
            {
              "minfloors": 4,
              "maxfloors": 6,
              "parts": [
                {
                  "part": "lostcities:floor_1"
                }
              ],
              "parts2": [
                {
                  "part": "lostcities:floor_2"
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(1, building.getParts().size(), "parts应有1个部件");
        assertEquals(1, building.getParts2().size(), "parts2应有1个部件");
        assertEquals("lostcities:floor_1", building.getParts().get(0).getPart(), "parts部件名称应匹配");
        assertEquals("lostcities:floor_2", building.getParts2().get(0).getPart(), "parts2部件名称应匹配");
    }

    @Test
    @DisplayName("应该兼容解析PartRef扁平条件字段")
    void shouldParsePartRefInlineConditionFields() {
        String json = """
            {
              "parts": [
                {
                  "part": "lostcities:top_only",
                  "top": true
                },
                {
                  "part": "lostcities:not_top",
                  "top": false,
                  "belowPart": ["lostcities:base"]
                }
              ]
            }
            """;

        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        assertNotNull(building);
        assertEquals(2, building.getParts().size());

        PartRef topPart = building.getParts().get(0);
        assertNotNull(topPart.getCondition(), "top 条件应被解析为 ConditionTest");
        assertEquals(Boolean.TRUE, topPart.getCondition().getTop());

        PartRef nonTopPart = building.getParts().get(1);
        assertNotNull(nonTopPart.getCondition(), "扁平条件字段应生成 ConditionTest");
        assertEquals(Boolean.FALSE, nonTopPart.getCondition().getTop());
        assertNotNull(nonTopPart.getCondition().getBelowPart());
        assertTrue(nonTopPart.getCondition().getBelowPart().contains("lostcities:base"));
    }

    @Test
    @DisplayName("应该兼容解析 PartRef transform 数字与字符串")
    void shouldParsePartRefTransformVariants() {
        String json = """
            {
              "parts": [
                {
                  "part": "lostcities:rotated",
                  "transform": 1
                },
                {
                  "part": "lostcities:mirrored",
                  "transform": "mirror_z"
                },
                {
                  "part": "lostcities:legacy_xform",
                  "xform": "ROTATE_270"
                }
              ]
            }
            """;

        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        assertNotNull(building);
        assertEquals(3, building.getParts().size());

        PartRef rotated = building.getParts().get(0);
        PartRef mirrored = building.getParts().get(1);
        PartRef legacyXform = building.getParts().get(2);
        assertEquals(1, rotated.getTransformCode());
        assertEquals(5, mirrored.getTransformCode());
        assertEquals(3, legacyXform.getTransformCode());
    }
    
    @Test
    @DisplayName("应该使用默认值解析最小建筑JSON")
    void shouldParseMinimalBuildingJsonWithDefaults() {
        String json = """
            {
              "parts": []
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(1, building.getMinFloors(), "默认最小楼层应为1");
        assertEquals(10, building.getMaxFloors(), "默认最大楼层应为10");
        assertEquals(0, building.getMinCellars(), "默认最小地下室应为0");
        assertEquals(0, building.getMaxCellars(), "默认最大地下室应为0");
        assertNotNull(building.getParts(), "部件列表不应为null");
    }
    
    @Test
    @DisplayName("应该处理建筑注册名称")
    void shouldHandleBuildingRegistryName() {
        BuildingRE building = new BuildingRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_building");
        building.setRegistryName(location);
        
        assertEquals(location, building.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== 多建筑测试 ====================
    
    @Test
    @DisplayName("应该正确解析2x2多建筑JSON")
    void shouldParse2x2MultiBuildingJson() {
        String json = """
            {
              "dimx": 2,
              "dimz": 2,
              "buildings": [
                ["building_0_0", "building_0_1"],
                ["building_1_0", "building_1_1"]
              ]
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(2, multiBuilding.getDimX(), "X维度应为2");
        assertEquals(2, multiBuilding.getDimZ(), "Z维度应为2");
        assertNotNull(multiBuilding.getBuildings(), "建筑列表不应为null");
        assertEquals(2, multiBuilding.getBuildings().size(), "应有2行");
        assertEquals(2, multiBuilding.getBuildings().get(0).size(), "第一行应有2个建筑");
        assertEquals(2, multiBuilding.getBuildings().get(1).size(), "第二行应有2个建筑");
        assertEquals("building_0_0", multiBuilding.getBuildings().get(0).get(0), "位置[0][0]建筑应匹配");
        assertEquals("building_1_1", multiBuilding.getBuildings().get(1).get(1), "位置[1][1]建筑应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析3x3多建筑JSON")
    void shouldParse3x3MultiBuildingJson() {
        String json = """
            {
              "dimx": 3,
              "dimz": 3,
              "buildings": [
                ["a", "b", "c"],
                ["d", "e", "f"],
                ["g", "h", "i"]
              ]
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(3, multiBuilding.getDimX(), "X维度应为3");
        assertEquals(3, multiBuilding.getDimZ(), "Z维度应为3");
        assertEquals(3, multiBuilding.getBuildings().size(), "应有3行");
        assertEquals("a", multiBuilding.getBuildings().get(0).get(0), "位置[0][0]应为a");
        assertEquals("e", multiBuilding.getBuildings().get(1).get(1), "位置[1][1]应为e");
        assertEquals("i", multiBuilding.getBuildings().get(2).get(2), "位置[2][2]应为i");
    }
    
    @Test
    @DisplayName("应该正确解析非对称多建筑JSON")
    void shouldParseAsymmetricMultiBuildingJson() {
        String json = """
            {
              "dimx": 3,
              "dimz": 2,
              "buildings": [
                ["building_a", "building_b", "building_c"],
                ["building_d", "building_e", "building_f"]
              ]
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(3, multiBuilding.getDimX(), "X维度应为3");
        assertEquals(2, multiBuilding.getDimZ(), "Z维度应为2");
        assertEquals(2, multiBuilding.getBuildings().size(), "应有2行");
        assertEquals(3, multiBuilding.getBuildings().get(0).size(), "每行应有3个建筑");
    }
    
    @Test
    @DisplayName("应该使用默认值解析最小多建筑JSON")
    void shouldParseMinimalMultiBuildingJsonWithDefaults() {
        String json = """
            {
              "buildings": []
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(2, multiBuilding.getDimX(), "默认X维度应为2");
        assertEquals(2, multiBuilding.getDimZ(), "默认Z维度应为2");
        assertNotNull(multiBuilding.getBuildings(), "建筑列表不应为null");
    }
    
    @Test
    @DisplayName("应该正确解析带命名空间的多建筑JSON")
    void shouldParseMultiBuildingWithNamespacesJson() {
        String json = """
            {
              "dimx": 2,
              "dimz": 2,
              "buildings": [
                ["cityloader:building_a", "lostcities:building_b"],
                ["custom:building_c", "minecraft:building_d"]
              ]
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals("cityloader:building_a", multiBuilding.getBuildings().get(0).get(0), "应保留命名空间");
        assertEquals("lostcities:building_b", multiBuilding.getBuildings().get(0).get(1), "应保留命名空间");
        assertEquals("custom:building_c", multiBuilding.getBuildings().get(1).get(0), "应保留命名空间");
        assertEquals("minecraft:building_d", multiBuilding.getBuildings().get(1).get(1), "应保留命名空间");
    }
    
    @Test
    @DisplayName("应该处理多建筑注册名称")
    void shouldHandleMultiBuildingRegistryName() {
        MultiBuildingRE multiBuilding = new MultiBuildingRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_multibuilding");
        multiBuilding.setRegistryName(location);
        
        assertEquals(location, multiBuilding.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== 边缘情况测试 ====================
    
    @Test
    @DisplayName("应该处理null JSON")
    void shouldHandleNullJson() {
        String nullJson = null;
        BuildingRE building = gson.fromJson(nullJson, BuildingRE.class);
        
        assertNull(building, "null JSON应返回null");
    }
    
    @Test
    @DisplayName("应该处理空JSON字符串")
    void shouldHandleEmptyJsonString() {
        String json = "";
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNull(building, "空JSON字符串应返回null");
    }
    
    @Test
    @DisplayName("应该处理格式错误的JSON")
    void shouldHandleMalformedJson() {
        String json = "{invalid json}";
        
        assertThrows(Exception.class, () -> {
            gson.fromJson(json, BuildingRE.class);
        }, "格式错误的JSON应抛出异常");
    }
    
    @Test
    @DisplayName("应该处理缺少必需字段的JSON")
    void shouldHandleJsonWithMissingFields() {
        String json = """
            {
              "dimx": 2
            }
            """;
        
        // 应该使用默认值而不是抛出异常
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(2, multiBuilding.getDimX(), "dimX应匹配");
        assertEquals(2, multiBuilding.getDimZ(), "dimZ应使用默认值");
    }
    
    @Test
    @DisplayName("应该处理额外字段的JSON")
    void shouldHandleJsonWithExtraFields() {
        String json = """
            {
              "dimx": 2,
              "dimz": 2,
              "buildings": [],
              "extraField": "should be ignored",
              "anotherExtra": 123
            }
            """;
        
        // Gson应该忽略额外字段
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(2, multiBuilding.getDimX(), "dimX应匹配");
        assertEquals(2, multiBuilding.getDimZ(), "dimZ应匹配");
    }
    
    // ==================== 实际数据测试 ====================
    
    @Test
    @DisplayName("应该正确解析实际的zombie_cabin建筑JSON")
    void shouldParseRealZombieCabinBuildingJson() {
        String json = """
            {
              "filler": "#",
              "rubble": "}",
              "minfloors": 1,
              "maxfloors": 1,
              "parts": [
                {
                  "part": "keerdm_zombie_essentials:zombie_cabin_0_0"
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(1, building.getMinFloors(), "最小楼层应为1");
        assertEquals(1, building.getMaxFloors(), "最大楼层应为1");
        assertEquals(1, building.getParts().size(), "应有1个部件");
        assertEquals("keerdm_zombie_essentials:zombie_cabin_0_0", 
                     building.getParts().get(0).getPart(), 
                     "部件名称应匹配");
        assertEquals("#", building.getFiller(), "填充方块应为#");
        assertEquals("}", building.getRubble(), "废墟方块应为}");
    }
    
    @Test
    @DisplayName("应该正确解析带所有新字段的建筑JSON")
    void shouldParseBuildingWithAllNewFieldsJson() {
        String json = """
            {
              "minfloors": 3,
              "maxfloors": 8,
              "filler": "B",
              "rubble": "R",
              "refpalette": "lostcities:default",
              "allowDoors": false,
              "allowFillers": false,
              "overrideFloors": true,
              "preferslonely": 0.5,
              "parts": [
                {
                  "part": "lostcities:floor_basic"
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals(3, building.getMinFloors(), "最小楼层应为3");
        assertEquals(8, building.getMaxFloors(), "最大楼层应为8");
        assertEquals("B", building.getFiller(), "填充方块应为B");
        assertEquals("R", building.getRubble(), "废墟方块应为R");
        assertEquals("lostcities:default", building.getRefPalette(), "引用调色板应匹配");
        assertEquals(false, building.getAllowDoors(), "allowDoors应为false");
        assertEquals(false, building.getAllowFillers(), "allowFillers应为false");
        assertEquals(true, building.getOverrideFloors(), "overrideFloors应为true");
        assertEquals(0.5f, building.getPrefersLonely(), 0.001f, "prefersLonely应为0.5");
    }
    
    @Test
    @DisplayName("应该使用新字段的默认值")
    void shouldUseDefaultValuesForNewFields() {
        String json = """
            {
              "minfloors": 5,
              "maxfloors": 10,
              "parts": []
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals("#", building.getFiller(), "默认填充方块应为#");
        assertNull(building.getRubble(), "默认废墟方块应为null");
        assertNull(building.getRefPalette(), "默认引用调色板应为null");
        assertNull(building.getLocalPalette(), "默认本地调色板应为null");
        assertEquals(true, building.getAllowDoors(), "默认allowDoors应为true");
        assertEquals(true, building.getAllowFillers(), "默认allowFillers应为true");
        assertEquals(false, building.getOverrideFloors(), "默认overrideFloors应为false");
        assertEquals(0.0f, building.getPrefersLonely(), 0.001f, "默认prefersLonely应为0.0");
    }
    
    @Test
    @DisplayName("应该正确解析带内联调色板的建筑JSON")
    void shouldParseBuildingWithInlinePaletteJson() {
        String json = """
            {
              "minfloors": 3,
              "maxfloors": 5,
              "palette": {
                "name": "inline_palette",
                "palette": [
                  {
                    "char": "A",
                    "block": "minecraft:stone"
                  },
                  {
                    "char": "B",
                    "block": "minecraft:dirt"
                  }
                ]
              },
              "parts": [
                {
                  "part": "lostcities:floor_basic"
                }
              ]
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertNotNull(building.getLocalPalette(), "本地调色板不应为null");
        assertEquals("inline_palette", building.getLocalPalette().getName(), "调色板名称应匹配");
        assertEquals(2, building.getLocalPalette().getPaletteEntries().size(), "调色板应有2个条目");
        assertEquals("minecraft:stone", building.getLocalPalette().getEntry("A").getBlock(), "字符A应映射到stone");
    }
    
    @Test
    @DisplayName("应该正确解析同时有refpalette和palette的建筑JSON")
    void shouldParseBuildingWithBothPaletteTypesJson() {
        String json = """
            {
              "minfloors": 2,
              "maxfloors": 4,
              "refpalette": "lostcities:default",
              "palette": {
                "name": "override_palette",
                "palette": [
                  {
                    "char": "X",
                    "block": "minecraft:glass"
                  }
                ]
              },
              "parts": []
            }
            """;
        
        BuildingRE building = gson.fromJson(json, BuildingRE.class);
        
        assertNotNull(building, "建筑不应为null");
        assertEquals("lostcities:default", building.getRefPalette(), "引用调色板应匹配");
        assertNotNull(building.getLocalPalette(), "本地调色板不应为null");
        assertEquals("override_palette", building.getLocalPalette().getName(), "本地调色板名称应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析实际的zombie_cabin多建筑JSON")
    void shouldParseRealZombieCabinMultiBuildingJson() {
        String json = """
            {
              "dimx": 2,
              "dimz": 2,
              "buildings": [
                [
                  "keerdm_zombie_essentials:zombie_cabin_0_0",
                  "keerdm_zombie_essentials:zombie_cabin_0_1"
                ],
                [
                  "keerdm_zombie_essentials:zombie_cabin_1_0",
                  "keerdm_zombie_essentials:zombie_cabin_1_1"
                ]
              ]
            }
            """;
        
        MultiBuildingRE multiBuilding = gson.fromJson(json, MultiBuildingRE.class);
        
        assertNotNull(multiBuilding, "多建筑不应为null");
        assertEquals(2, multiBuilding.getDimX(), "X维度应为2");
        assertEquals(2, multiBuilding.getDimZ(), "Z维度应为2");
        assertEquals(2, multiBuilding.getBuildings().size(), "应有2行");
        assertEquals("keerdm_zombie_essentials:zombie_cabin_0_0", 
                     multiBuilding.getBuildings().get(0).get(0), 
                     "位置[0][0]建筑应匹配");
        assertEquals("keerdm_zombie_essentials:zombie_cabin_1_1", 
                     multiBuilding.getBuildings().get(1).get(1), 
                     "位置[1][1]建筑应匹配");
    }
    
    // ==================== BuildingPartRE 测试 ====================
    
    @Test
    @DisplayName("应该正确解析简单建筑部件JSON")
    void shouldParseSimpleBuildingPartJson() {
        String json = """
            {
              "width": 16,
              "height": 16,
              "depth": 1,
              "palette": "lostcities:default",
              "slices": [
                "................",
                "................",
                "................"
              ]
            }
            """;
        
        BuildingPartRE part = gson.fromJson(json, BuildingPartRE.class);
        
        assertNotNull(part, "建筑部件不应为null");
        assertEquals(16, part.getWidth(), "宽度应为16");
        assertEquals(16, part.getHeight(), "高度应为16");
        assertEquals(1, part.getDepth(), "深度应为1");
        assertEquals("lostcities:default", part.getPalette(), "调色板应匹配");
        assertNotNull(part.getSlices(), "切片列表不应为null");
        assertEquals(3, part.getSlices().size(), "应有3个切片");
    }
    
    @Test
    @DisplayName("应该正确解析多层建筑部件JSON")
    void shouldParseMultiLayerBuildingPartJson() {
        String json = """
            {
              "width": 16,
              "height": 16,
              "depth": 3,
              "palette": "lostcities:glass",
              "slices": [
                "AAAAAAAAAAAAAAAA",
                "BBBBBBBBBBBBBBBB",
                "CCCCCCCCCCCCCCCC"
              ]
            }
            """;
        
        BuildingPartRE part = gson.fromJson(json, BuildingPartRE.class);
        
        assertNotNull(part, "建筑部件不应为null");
        assertEquals(3, part.getDepth(), "深度应为3");
        assertEquals(3, part.getSlices().size(), "切片数量应匹配深度");
        assertEquals("AAAAAAAAAAAAAAAA", part.getSlices().get(0), "第一层切片应匹配");
        assertEquals("CCCCCCCCCCCCCCCC", part.getSlices().get(2), "第三层切片应匹配");
    }
    
    @Test
    @DisplayName("应该使用默认值解析最小建筑部件JSON")
    void shouldParseMinimalBuildingPartJsonWithDefaults() {
        String json = """
            {
              "slices": []
            }
            """;
        
        BuildingPartRE part = gson.fromJson(json, BuildingPartRE.class);
        
        assertNotNull(part, "建筑部件不应为null");
        assertEquals(16, part.getWidth(), "默认宽度应为16");
        assertEquals(16, part.getHeight(), "默认高度应为16");
        assertEquals(1, part.getDepth(), "默认深度应为1");
        assertNotNull(part.getSlices(), "切片列表不应为null");
    }
    
    @Test
    @DisplayName("应该正确解析复杂建筑部件JSON（带特殊字符）")
    void shouldParseComplexBuildingPartJson() {
        String json = """
            {
              "width": 16,
              "height": 16,
              "depth": 2,
              "palette": "lostcities:rails",
              "slices": [
                "################",
                "####WWWWWW######"
              ]
            }
            """;
        
        BuildingPartRE part = gson.fromJson(json, BuildingPartRE.class);
        
        assertNotNull(part, "建筑部件不应为null");
        assertEquals(2, part.getDepth(), "深度应为2");
        assertTrue(part.getSlices().get(0).contains("#"), "第一层应包含#字符");
        assertTrue(part.getSlices().get(1).contains("W"), "第二层应包含W字符");
    }
    
    @Test
    @DisplayName("应该处理建筑部件注册名称")
    void shouldHandleBuildingPartRegistryName() {
        BuildingPartRE part = new BuildingPartRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_part");
        part.setRegistryName(location);
        
        assertEquals(location, part.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== ConditionRE 测试 ====================
    
    @Test
    @DisplayName("应该正确解析简单条件JSON")
    void shouldParseSimpleConditionJson() {
        String json = """
            {
              "values": [
                {
                  "factor": 1.0,
                  "value": "test_value"
                }
              ]
            }
            """;
        
        ConditionRE condition = gson.fromJson(json, ConditionRE.class);
        
        assertNotNull(condition, "条件不应为null");
        assertNotNull(condition.getValues(), "值列表不应为null");
        assertEquals(1, condition.getValues().size(), "应有1个值");
        assertEquals(1.0f, condition.getValues().get(0).getFactor(), 0.001f, "因子应为1.0");
        assertEquals("test_value", condition.getValues().get(0).getValue(), "值应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析带多个值的条件JSON")
    void shouldParseConditionWithMultipleValuesJson() {
        String json = """
            {
              "values": [
                {
                  "factor": 10.0,
                  "value": "value_a"
                },
                {
                  "factor": 5.0,
                  "value": "value_b"
                },
                {
                  "factor": 1.0,
                  "value": "value_c"
                }
              ]
            }
            """;
        
        ConditionRE condition = gson.fromJson(json, ConditionRE.class);
        
        assertNotNull(condition, "条件不应为null");
        assertEquals(3, condition.getValues().size(), "应有3个值");
        assertEquals(10.0f, condition.getValues().get(0).getFactor(), 0.001f, "第一个因子应为10.0");
        assertEquals(5.0f, condition.getValues().get(1).getFactor(), 0.001f, "第二个因子应为5.0");
        assertEquals(1.0f, condition.getValues().get(2).getFactor(), 0.001f, "第三个因子应为1.0");
    }
    
    @Test
    @DisplayName("应该正确解析带条件测试的条件JSON")
    void shouldParseConditionWithTestsJson() {
        String json = """
            {
              "values": [
                {
                  "factor": 1.0,
                  "value": "top_floor",
                  "top": true
                },
                {
                  "factor": 1.0,
                  "value": "ground_floor",
                  "ground": true
                },
                {
                  "factor": 1.0,
                  "value": "cellar_floor",
                  "cellar": true
                }
              ]
            }
            """;
        
        ConditionRE condition = gson.fromJson(json, ConditionRE.class);
        
        assertNotNull(condition, "条件不应为null");
        assertEquals(3, condition.getValues().size(), "应有3个值");
        assertEquals("top_floor", condition.getValues().get(0).getValue(), "第一个值应为top_floor");
        assertEquals("ground_floor", condition.getValues().get(1).getValue(), "第二个值应为ground_floor");
        assertEquals("cellar_floor", condition.getValues().get(2).getValue(), "第三个值应为cellar_floor");
    }
    
    @Test
    @DisplayName("应该使用默认值解析空条件JSON")
    void shouldParseEmptyConditionJsonWithDefaults() {
        String json = """
            {
              "values": []
            }
            """;
        
        ConditionRE condition = gson.fromJson(json, ConditionRE.class);
        
        assertNotNull(condition, "条件不应为null");
        assertNotNull(condition.getValues(), "值列表不应为null");
        assertTrue(condition.getValues().isEmpty(), "值列表应为空");
    }
    
    @Test
    @DisplayName("应该处理条件注册名称")
    void shouldHandleConditionRegistryName() {
        ConditionRE condition = new ConditionRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_condition");
        condition.setRegistryName(location);
        
        assertEquals(location, condition.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== StyleRE 测试 ====================
    
    @Test
    @DisplayName("应该正确解析简单样式JSON")
    void shouldParseSimpleStyleJson() {
        String json = """
            {
              "palettes": [
                "lostcities:default",
                "lostcities:glass"
              ],
              "weights": [
                10.0,
                5.0
              ]
            }
            """;
        
        StyleRE style = gson.fromJson(json, StyleRE.class);
        
        assertNotNull(style, "样式不应为null");
        assertNotNull(style.getPalettes(), "调色板列表不应为null");
        assertNotNull(style.getWeights(), "权重列表不应为null");
        assertEquals(2, style.getPalettes().size(), "应有2个调色板");
        assertEquals(2, style.getWeights().size(), "应有2个权重");
        assertEquals("lostcities:default", style.getPalettes().get(0), "第一个调色板应匹配");
        assertEquals(10.0f, style.getWeights().get(0), 0.001f, "第一个权重应为10.0");
    }

    @Test
    @DisplayName("应该正确解析 LostCities randompalettes 样式JSON")
    void shouldParseRandomPalettesStyleJson() {
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

        StyleRE style = gson.fromJson(json, StyleRE.class);

        assertNotNull(style, "样式不应为null");
        assertNotNull(style.getRandomPaletteChoices(), "randomPaletteChoices 不应为null");
        assertEquals(2, style.getRandomPaletteChoices().size(), "应有2个随机组");
        assertEquals(1, style.getRandomPaletteChoices().get(0).size(), "第一组应有1个条目");
        assertEquals(2, style.getRandomPaletteChoices().get(1).size(), "第二组应有2个条目");
        assertEquals("lostcities:stone", style.getRandomPaletteChoices().get(1).get(0).palette(), "调色板应匹配");
        assertEquals(2.0f, style.getRandomPaletteChoices().get(1).get(0).factor(), 0.001f, "权重应匹配");
    }

    @Test
    @DisplayName("应该将旧 palettes/weights 兼容转换为 randompalettes")
    void shouldConvertLegacyStyleFieldsToRandomPalettes() {
        String json = """
            {
              "palettes": ["lostcities:p1", "lostcities:p2"],
              "weights": [3.0, 7.0]
            }
            """;

        StyleRE style = gson.fromJson(json, StyleRE.class);

        assertNotNull(style, "样式不应为null");
        assertEquals(2, style.getRandomPaletteChoices().size(), "应转换成2个随机组");
        assertEquals("lostcities:p1", style.getRandomPaletteChoices().get(0).get(0).palette(), "第一个调色板应匹配");
        assertEquals(3.0f, style.getRandomPaletteChoices().get(0).get(0).factor(), 0.001f, "第一个权重应匹配");
        assertEquals("lostcities:p2", style.getRandomPaletteChoices().get(1).get(0).palette(), "第二个调色板应匹配");
        assertEquals(7.0f, style.getRandomPaletteChoices().get(1).get(0).factor(), 0.001f, "第二个权重应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析单一调色板样式JSON")
    void shouldParseSinglePaletteStyleJson() {
        String json = """
            {
              "palettes": [
                "lostcities:rails"
              ],
              "weights": [
                1.0
              ]
            }
            """;
        
        StyleRE style = gson.fromJson(json, StyleRE.class);
        
        assertNotNull(style, "样式不应为null");
        assertEquals(1, style.getPalettes().size(), "应有1个调色板");
        assertEquals(1, style.getWeights().size(), "应有1个权重");
        assertEquals("lostcities:rails", style.getPalettes().get(0), "调色板应匹配");
        assertEquals(1.0f, style.getWeights().get(0), 0.001f, "权重应为1.0");
    }
    
    @Test
    @DisplayName("应该正确解析多调色板样式JSON")
    void shouldParseMultiplePaletteStyleJson() {
        String json = """
            {
              "palettes": [
                "lostcities:default",
                "lostcities:glass_full",
                "lostcities:glass_thin",
                "cityloader:custom"
              ],
              "weights": [
                20.0,
                10.0,
                5.0,
                1.0
              ]
            }
            """;
        
        StyleRE style = gson.fromJson(json, StyleRE.class);
        
        assertNotNull(style, "样式不应为null");
        assertEquals(4, style.getPalettes().size(), "应有4个调色板");
        assertEquals(4, style.getWeights().size(), "应有4个权重");
        assertEquals(20.0f, style.getWeights().get(0), 0.001f, "第一个权重应为20.0");
        assertEquals(1.0f, style.getWeights().get(3), 0.001f, "第四个权重应为1.0");
    }
    
    @Test
    @DisplayName("应该使用默认值解析空样式JSON")
    void shouldParseEmptyStyleJsonWithDefaults() {
        String json = """
            {
              "palettes": [],
              "weights": []
            }
            """;
        
        StyleRE style = gson.fromJson(json, StyleRE.class);
        
        assertNotNull(style, "样式不应为null");
        assertNotNull(style.getPalettes(), "调色板列表不应为null");
        assertNotNull(style.getWeights(), "权重列表不应为null");
        assertTrue(style.getPalettes().isEmpty(), "调色板列表应为空");
        assertTrue(style.getWeights().isEmpty(), "权重列表应为空");
    }
    
    @Test
    @DisplayName("应该处理样式注册名称")
    void shouldHandleStyleRegistryName() {
        StyleRE style = new StyleRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_style");
        style.setRegistryName(location);
        
        assertEquals(location, style.getRegistryName(), "注册名称应匹配");
    }
    
    // ==================== VariantRE 测试 ====================
    
    @Test
    @DisplayName("应该正确解析预定义城市街道扩展字段")
    void shouldParsePredefinedStreetExtendedFields() {
        String json = """
            {
              "dimension": "world",
              "chunkx": 100,
              "chunkz": -40,
              "streets": [
                {
                  "rel_chunk_x": 2,
                  "rel_chunk_z": -3,
                  "connections": "nwe",
                  "type": "bend",
                  "south": true
                },
                {
                  "x": -1,
                  "z": 0,
                  "streetpart": "end",
                  "n": true,
                  "e": false
                }
              ]
            }
            """;

        PredefinedCityRE city = gson.fromJson(json, PredefinedCityRE.class);

        assertNotNull(city, "预定义城市不应为null");
        assertNotNull(city.getPredefinedStreets(), "街道列表不应为null");
        assertEquals(2, city.getPredefinedStreets().size(), "应有2条预定义街道");

        PredefinedStreet first = city.getPredefinedStreets().get(0);
        assertEquals(2, first.relChunkX(), "第一条 relChunkX 应匹配");
        assertEquals(-3, first.relChunkZ(), "第一条 relChunkZ 应匹配");
        assertEquals("nwe", first.getConnections(), "第一条 connections 应匹配");
        assertEquals("bend", first.getType(), "第一条 type 应匹配");
        assertEquals(Boolean.TRUE, first.getSouth(), "第一条 south 应为 true");

        PredefinedStreet second = city.getPredefinedStreets().get(1);
        assertEquals(-1, second.getRelChunkX(), "第二条 x 别名应映射为 relChunkX");
        assertEquals(0, second.getRelChunkZ(), "第二条 z 别名应映射为 relChunkZ");
        assertEquals("end", second.getType(), "第二条 streetpart 别名应映射为 type");
        assertEquals(Boolean.TRUE, second.getNorth(), "第二条 n 别名应映射为 north");
        assertEquals(Boolean.FALSE, second.getEast(), "第二条 e 别名应映射为 east");
    }

    @Test
    @DisplayName("应该正确解析预定义城市建筑偏移字段别名")
    void shouldParsePredefinedBuildingOffsetAliases() {
        String json = """
            {
              "dimension": "world",
              "chunkx": 32,
              "chunkz": -8,
              "buildings": [
                {
                  "building": "cabin",
                  "chunkx": 2,
                  "chunkz": -3,
                  "multi": true
                },
                {
                  "building": "warehouse",
                  "rel_chunk_x": -1,
                  "rel_chunk_z": 4,
                  "prevent_ruins": true
                }
              ]
            }
            """;

        PredefinedCityRE city = gson.fromJson(json, PredefinedCityRE.class);
        assertNotNull(city, "预定义城市不应为null");
        assertNotNull(city.getPredefinedBuildings(), "预定义建筑列表不应为null");
        assertEquals(2, city.getPredefinedBuildings().size(), "应解析出2个预定义建筑");

        PredefinedBuilding first = city.getPredefinedBuildings().get(0);
        assertEquals("cabin", first.getBuilding(), "第一条 building 应匹配");
        assertEquals(2, first.relChunkX(), "第一条 chunkx 别名应映射为 relChunkX");
        assertEquals(-3, first.relChunkZ(), "第一条 chunkz 别名应映射为 relChunkZ");
        assertTrue(first.multi(), "第一条 multi 应为 true");

        PredefinedBuilding second = city.getPredefinedBuildings().get(1);
        assertEquals("warehouse", second.getBuilding(), "第二条 building 应匹配");
        assertEquals(-1, second.relChunkX(), "第二条 rel_chunk_x 应匹配");
        assertEquals(4, second.relChunkZ(), "第二条 rel_chunk_z 应匹配");
        assertTrue(second.preventRuins(), "第二条 prevent_ruins 应为 true");
    }

    @Test
    @DisplayName("应该正确解析简单变体JSON")
    void shouldParseSimpleVariantJson() {
        String json = """
            {
              "name": "test_variant",
              "weight": 1.0
            }
            """;
        
        VariantRE variant = gson.fromJson(json, VariantRE.class);
        
        assertNotNull(variant, "变体不应为null");
        assertEquals("test_variant", variant.getName(), "名称应匹配");
        assertEquals(1.0f, variant.getWeight(), 0.001f, "权重应为1.0");
    }

    @Test
    @DisplayName("应该正确解析 LostCities blocks 变体JSON")
    void shouldParseBlocksVariantJson() {
        String json = """
            {
              "blocks": [
                {"random": 30, "block": "minecraft:stone"},
                {"random": 98, "block": "minecraft:mossy_stone_bricks"}
              ]
            }
            """;

        VariantRE variant = gson.fromJson(json, VariantRE.class);

        assertNotNull(variant, "变体不应为null");
        assertNotNull(variant.getBlocks(), "blocks 列表不应为null");
        assertEquals(2, variant.getBlocks().size(), "应有2个方块条目");
        assertEquals(30, variant.getBlocks().get(0).random(), "第一个条目权重应匹配");
        assertEquals("minecraft:stone", variant.getBlocks().get(0).block(), "第一个条目方块应匹配");
        assertEquals(98, variant.getBlocks().get(1).random(), "第二个条目权重应匹配");
    }
    
    @Test
    @DisplayName("应该正确解析带高权重的变体JSON")
    void shouldParseHighWeightVariantJson() {
        String json = """
            {
              "name": "common_variant",
              "weight": 100.0
            }
            """;
        
        VariantRE variant = gson.fromJson(json, VariantRE.class);
        
        assertNotNull(variant, "变体不应为null");
        assertEquals("common_variant", variant.getName(), "名称应匹配");
        assertEquals(100.0f, variant.getWeight(), 0.001f, "权重应为100.0");
    }
    
    @Test
    @DisplayName("应该正确解析带低权重的变体JSON")
    void shouldParseLowWeightVariantJson() {
        String json = """
            {
              "name": "rare_variant",
              "weight": 0.1
            }
            """;
        
        VariantRE variant = gson.fromJson(json, VariantRE.class);
        
        assertNotNull(variant, "变体不应为null");
        assertEquals("rare_variant", variant.getName(), "名称应匹配");
        assertEquals(0.1f, variant.getWeight(), 0.001f, "权重应为0.1");
    }
    
    @Test
    @DisplayName("应该使用默认值解析最小变体JSON")
    void shouldParseMinimalVariantJsonWithDefaults() {
        String json = """
            {
              "name": "default_variant"
            }
            """;
        
        VariantRE variant = gson.fromJson(json, VariantRE.class);
        
        assertNotNull(variant, "变体不应为null");
        assertEquals("default_variant", variant.getName(), "名称应匹配");
        assertEquals(1.0f, variant.getWeight(), 0.001f, "默认权重应为1.0");
    }
    
    @Test
    @DisplayName("应该处理变体注册名称")
    void shouldHandleVariantRegistryName() {
        VariantRE variant = new VariantRE();
        
        ResourceLocation location = new ResourceLocation("cityloader", "test_variant");
        variant.setRegistryName(location);
        
        assertEquals(location, variant.getRegistryName(), "注册名称应匹配");
    }
}
