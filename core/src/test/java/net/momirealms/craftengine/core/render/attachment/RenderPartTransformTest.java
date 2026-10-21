package net.momirealms.craftengine.core.render.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderPartTransformTest {
    @Test
    void preservesExplicitSpaceAndPose() {
        var transform = new RenderPartTransform(RenderTransformSpace.INSTANCE_LOCAL, 1, 2, 3, 10, 20, 30);
        assertEquals(RenderTransformSpace.INSTANCE_LOCAL, transform.space());
        assertEquals(1, transform.x());
        assertEquals(30, transform.rotationZ());
    }

    @Test
    void rejectsNonFinitePoseValues() {
        assertThrows(IllegalArgumentException.class, () -> new RenderPartTransform(RenderTransformSpace.WORLD, Double.NaN, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RenderPartTransform(RenderTransformSpace.WORLD, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0));
    }
}
