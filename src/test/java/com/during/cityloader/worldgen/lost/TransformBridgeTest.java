package com.during.cityloader.worldgen.lost;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("worldgen.lost.Transform 桥接测试")
class TransformBridgeTest {

    @Test
    @DisplayName("fromCode 非法值应回退 ROTATE_NONE")
    void shouldFallbackToNoneForUnknownCode() {
        assertEquals(Transform.ROTATE_NONE, Transform.fromCode(-1));
        assertEquals(Transform.ROTATE_NONE, Transform.fromCode(999));
    }

    @Test
    @DisplayName("fromCode 与映射语义应正确")
    void shouldMatchCodeSemantics() {
        assertEquals(Transform.ROTATE_NONE, Transform.fromCode(0));
        assertEquals(Transform.ROTATE_180, Transform.fromCode(2));
        assertEquals(Transform.MIRROR_90_X, Transform.fromCode(6));
        assertEquals(1, Transform.ROTATE_90.mapX(0, 1, 3, 2));
        assertEquals(3, Transform.ROTATE_90.mapZ(0, 1, 3, 2));
    }
}
