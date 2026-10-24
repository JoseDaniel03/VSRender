package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.core.math.Vec3;

import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class SpreadSampler {
    private SpreadSampler() {}

    /** Samples uniformly inside a cone around forward. */
    public static Vec3 sampleCone(Vec3 forward, double maxAngleDegrees, DoubleSupplier random) {
        Objects.requireNonNull(forward, "forward");
        Objects.requireNonNull(random, "random");
        Vec3 f = forward.normalize();
        if (f.lengthSquared() == 0.0 || maxAngleDegrees <= 0.0) return f;

        double maxAngle = Math.toRadians(maxAngleDegrees);
        double u = clampUnit(random.getAsDouble());
        double v = clampUnit(random.getAsDouble());
        double cosTheta = 1.0 - u * (1.0 - Math.cos(maxAngle));
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double phi = Math.PI * 2.0 * v;

        Vec3 helper = Math.abs(f.y()) < 0.999 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 right = helper.cross(f).normalize();
        Vec3 up = f.cross(right).normalize();

        return f.multiply(cosTheta)
                .add(right.multiply(Math.cos(phi) * sinTheta))
                .add(up.multiply(Math.sin(phi) * sinTheta))
                .normalize();
    }

    private static double clampUnit(double value) {
        if (value <= 0.0) return 0.0;
        if (value >= 1.0) return Math.nextDown(1.0);
        return value;
    }
}
