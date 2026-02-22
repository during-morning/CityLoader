package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.ConditionTest;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConditionContext 条件语义测试")
class ConditionContextTest {

    private static final Gson GSON = new Gson();

    @Test
    @DisplayName("顶部判定应匹配最后一层")
    void shouldTreatLastFloorAsTop() {
        ConditionContext top = new TestContext(4, 4, 0, 5, "roof", "floor", "building");
        ConditionContext nonTop = new TestContext(3, 3, 0, 5, "floor", "floor", "building");

        assertTrue(top.isTopOfBuilding());
        assertFalse(nonTop.isTopOfBuilding());
    }

    @Test
    @DisplayName("belowPart 条件应匹配下方部件而非当前部件")
    void shouldMatchBelowPartAgainstBelowPartField() {
        ConditionTest test = GSON.fromJson("{\"belowPart\":[\"lostcities:base\"]}", ConditionTest.class);
        Predicate<ConditionContext> predicate = ConditionContext.parseTest(test);

        ConditionContext matched = new TestContext(1, 1, 0, 2, "lostcities:upper", "lostcities:base", "building");
        ConditionContext notMatched = new TestContext(1, 1, 0, 2, "lostcities:base", "lostcities:other", "building");

        assertTrue(predicate.test(matched));
        assertFalse(predicate.test(notMatched));
    }

    private static final class TestContext extends ConditionContext {

        private TestContext(int level, int floor, int floorsBelowGround, int floorsAboveGround,
                            String part, String belowPart, String building) {
            super(level, floor, floorsBelowGround, floorsAboveGround, part, belowPart, building,
                    new ChunkCoord("world", 0, 0));
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
