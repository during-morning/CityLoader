package com.during.cityloader.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Part 变换测试")
class PartTransformTest {

    @Test
    @DisplayName("rotate90 应正确变换坐标")
    void rotate90ShouldTransformCoordinates() {
        Part part = createSamplePart();
        Part rotated = part.rotate90();

        assertEquals(2, rotated.getWidth());
        assertEquals(1, rotated.getHeight());
        assertEquals(3, rotated.getDepth());
        assertRows(rotated, "CF", "BE", "AD");
    }

    @Test
    @DisplayName("rotate180 应正确变换坐标")
    void rotate180ShouldTransformCoordinates() {
        Part part = createSamplePart();
        Part rotated = part.rotate180();

        assertEquals(3, rotated.getWidth());
        assertEquals(1, rotated.getHeight());
        assertEquals(2, rotated.getDepth());
        assertRows(rotated, "FED", "CBA");
    }

    @Test
    @DisplayName("rotate270 应正确变换坐标")
    void rotate270ShouldTransformCoordinates() {
        Part part = createSamplePart();
        Part rotated = part.rotate270();

        assertEquals(2, rotated.getWidth());
        assertEquals(1, rotated.getHeight());
        assertEquals(3, rotated.getDepth());
        assertRows(rotated, "DA", "EB", "FC");
    }

    @Test
    @DisplayName("mirrorX 与 mirrorZ 应分别沿对应轴翻转")
    void mirrorsShouldFlipAxes() {
        Part part = createSamplePart();

        Part mirrorX = part.mirrorX();
        assertRows(mirrorX, "CBA", "FED");

        Part mirrorZ = part.mirrorZ();
        assertRows(mirrorZ, "DEF", "ABC");
    }

    @Test
    @DisplayName("transform 应按先旋转再镜像执行")
    void transformShouldApplyRotationThenMirror() {
        Part part = createSamplePart();
        Part transformed = part.transform(1, false, true);

        assertEquals(2, transformed.getWidth());
        assertEquals(3, transformed.getDepth());
        assertRows(transformed, "AD", "BE", "CF");
    }

    @Test
    @DisplayName("Transform 枚举应支持 MIRROR_90_X")
    void transformEnumShouldSupportMirror90X() {
        Part part = createSamplePart();
        Part transformed = part.transform(Transform.MIRROR_90_X);

        assertEquals(2, transformed.getWidth());
        assertEquals(3, transformed.getDepth());
        assertRows(transformed, "AD", "BE", "CF");
    }

    @Test
    @DisplayName("Transform.getOpposite 应正确返回逆变换")
    void transformOppositeShouldBeCorrect() {
        assertEquals(Transform.ROTATE_270, Transform.ROTATE_90.getOpposite());
        assertEquals(Transform.ROTATE_90, Transform.ROTATE_270.getOpposite());
        assertEquals(Transform.ROTATE_180, Transform.ROTATE_180.getOpposite());
        assertEquals(Transform.MIRROR_X, Transform.MIRROR_X.getOpposite());
    }

    @Test
    @DisplayName("Transform.mapX/mapZ 应给出正确的坐标映射")
    void transformCoordinateMappingShouldBeCorrect() {
        int maxX = 3;
        int maxZ = 2;

        assertEquals(0, Transform.ROTATE_NONE.mapX(0, 1, maxX, maxZ));
        assertEquals(1, Transform.ROTATE_NONE.mapZ(0, 1, maxX, maxZ));

        assertEquals(1, Transform.ROTATE_90.mapX(0, 1, maxX, maxZ));
        assertEquals(3, Transform.ROTATE_90.mapZ(0, 1, maxX, maxZ));

        assertEquals(1, Transform.ROTATE_270.mapX(0, 1, maxX, maxZ));
        assertEquals(0, Transform.ROTATE_270.mapZ(0, 1, maxX, maxZ));

        assertEquals(3, Transform.MIRROR_X.mapX(0, 1, maxX, maxZ));
        assertEquals(1, Transform.MIRROR_X.mapZ(0, 1, maxX, maxZ));

        assertEquals(1, Transform.MIRROR_90_X.mapX(0, 1, maxX, maxZ));
        assertEquals(0, Transform.MIRROR_90_X.mapZ(0, 1, maxX, maxZ));
    }

    @Test
    @DisplayName("Transform.mapX/mapZ 应与 Part.transform 结果一致")
    void transformCoordinateMappingShouldMatchPartTransform() {
        Part source = createSamplePart();
        int maxX = source.getWidth() - 1;
        int maxZ = source.getDepth() - 1;

        for (Transform transform : Transform.values()) {
            Part transformed = source.transform(transform);
            for (int z = 0; z < source.getDepth(); z++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int mappedX = transform.mapX(x, z, maxX, maxZ);
                    int mappedZ = transform.mapZ(x, z, maxX, maxZ);
                    assertTrue(mappedX >= 0 && mappedX < transformed.getWidth(), transform + " mappedX out of bounds");
                    assertTrue(mappedZ >= 0 && mappedZ < transformed.getDepth(), transform + " mappedZ out of bounds");
                    assertEquals(source.getCharAt(x, 0, z), transformed.getCharAt(mappedX, 0, mappedZ),
                            transform + " mismatch at source(" + x + "," + z + ")");
                }
            }
        }
    }

    @Test
    @DisplayName("兼容方法 rotate/mirror 应保持原行为")
    void legacyMethodsShouldRemainCompatible() {
        Part part = createSamplePart();

        assertRows(part.rotate(), "CF", "BE", "AD");
        assertRows(part.mirror(), "CBA", "FED");
    }

    private static Part createSamplePart() {
        char[][][] structure = new char[1][2][3];
        structure[0][0] = new char[] { 'A', 'B', 'C' };
        structure[0][1] = new char[] { 'D', 'E', 'F' };
        return new Part("sample", List.of("test"), 3, 1, 2, structure);
    }

    private static void assertRows(Part part, String... rows) {
        assertEquals(rows.length, part.getDepth());
        for (int z = 0; z < rows.length; z++) {
            String expected = rows[z];
            StringBuilder actual = new StringBuilder();
            for (int x = 0; x < part.getWidth(); x++) {
                actual.append(part.getCharAt(x, 0, z));
            }
            assertEquals(expected, actual.toString(), "z=" + z);
        }
    }
}
