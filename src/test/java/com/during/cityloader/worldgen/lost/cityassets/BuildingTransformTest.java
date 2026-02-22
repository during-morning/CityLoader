package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.Transform;
import com.during.cityloader.worldgen.lost.regassets.BuildingRE;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Building Transform 选择测试")
class BuildingTransformTest {

    private static final Gson GSON = new Gson();

    @Test
    @DisplayName("应返回 PartRef 声明的 transform")
    void shouldReturnTransformFromPartRef() {
        BuildingRE buildingRE = GSON.fromJson("""
                {
                  "parts": [
                    {"part": "test:part_a", "factor": 1, "transform": 1}
                  ],
                  "parts2": [
                    {"part": "test:part_b", "factor": 1, "transform": "mirror_z"}
                  ]
                }
                """, BuildingRE.class);
        buildingRE.setRegistryName(new ResourceLocation("test", "transform_building"));

        Building building = new Building(buildingRE);
        ConditionContext context = new TestContext();

        Building.PartSelection part = building.getRandomPartRef(new Random(1L), context);
        Building.PartSelection part2 = building.getRandomPart2Ref(new Random(1L), context);

        assertNotNull(part);
        assertNotNull(part2);
        assertEquals("test:part_a", part.partName());
        assertEquals("test:part_b", part2.partName());
        assertEquals(Transform.ROTATE_90, part.transform());
        assertEquals(Transform.MIRROR_Z, part2.transform());
    }

    @Test
    @DisplayName("未声明 transform 时应回退 ROTATE_NONE")
    void shouldFallbackToRotateNoneWhenTransformMissing() {
        BuildingRE buildingRE = GSON.fromJson("""
                {
                  "parts": [
                    {"part": "test:part_default", "factor": 1}
                  ]
                }
                """, BuildingRE.class);
        buildingRE.setRegistryName(new ResourceLocation("test", "default_building"));

        Building building = new Building(buildingRE);
        Building.PartSelection part = building.getRandomPartRef(new Random(2L), new TestContext());

        assertNotNull(part);
        assertEquals(Transform.ROTATE_NONE, part.transform());
    }

    private static final class TestContext extends ConditionContext {

        private TestContext() {
            super(0, 0, 0, 1, "<none>", "<none>", "test:building", new ChunkCoord("world", 0, 0));
        }

        @Override
        public boolean isSphere() {
            return false;
        }

        @Override
        public ResourceLocation getBiome() {
            return new ResourceLocation("minecraft", "plains");
        }
    }
}
