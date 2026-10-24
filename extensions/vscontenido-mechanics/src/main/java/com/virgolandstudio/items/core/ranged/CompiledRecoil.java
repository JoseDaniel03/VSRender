package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.core.curve.CompiledCurve;

public record CompiledRecoil(
        double pitchKickDegrees,
        double pitchJitterDegrees,
        double yawKickDegrees,
        double yawJitterDegrees,
        CompiledCurve pitchByShot,
        CompiledCurve yawByShot,
        double maxPitchKickDegrees,
        double maxYawKickDegrees
) {
    /** Compatibility constructor retained for callers compiled before 0.51.1. */
    public CompiledRecoil(
            double pitchKickDegrees,
            double yawJitterDegrees,
            CompiledCurve pitchByShot,
            CompiledCurve yawByShot
    ) {
        this(pitchKickDegrees, 0.0, 0.0, yawJitterDegrees, pitchByShot, yawByShot, 45.0, 45.0);
    }

    public CompiledRecoil(double pitchKickDegrees, double yawJitterDegrees) {
        this(pitchKickDegrees, 0.0, 0.0, yawJitterDegrees, null, null, 45.0, 45.0);
    }

    public CompiledRecoil {
        validateNonNegativeFinite("pitchKickDegrees", pitchKickDegrees);
        validateNonNegativeFinite("pitchJitterDegrees", pitchJitterDegrees);
        if (!Double.isFinite(yawKickDegrees)) {
            throw new IllegalArgumentException("yawKickDegrees must be finite");
        }
        validateNonNegativeFinite("yawJitterDegrees", yawJitterDegrees);
        validateNonNegativeFinite("maxPitchKickDegrees", maxPitchKickDegrees);
        validateNonNegativeFinite("maxYawKickDegrees", maxYawKickDegrees);
    }

    public double pitchForShot(int shotIndex) {
        double value = pitchByShot == null ? pitchKickDegrees : pitchByShot.evaluate(Math.max(0, shotIndex));
        if (!Double.isFinite(value)) value = pitchKickDegrees;
        return clamp(value, 0.0, maxPitchKickDegrees);
    }

    public double yawForShot(int shotIndex) {
        double value = yawByShot == null ? yawKickDegrees : yawByShot.evaluate(Math.max(0, shotIndex));
        if (!Double.isFinite(value)) value = yawKickDegrees;
        return clamp(value, -maxYawKickDegrees, maxYawKickDegrees);
    }

    /** Legacy accessor preserved for old platform classes. */
    public double yawPatternForShot(int shotIndex) {
        return yawForShot(shotIndex);
    }

    public double clampPitch(double value) {
        return clamp(value, 0.0, maxPitchKickDegrees);
    }

    public double clampYaw(double value) {
        return clamp(value, -maxYawKickDegrees, maxYawKickDegrees);
    }

    private static void validateNonNegativeFinite(String name, double value) {
        if (value < 0.0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
