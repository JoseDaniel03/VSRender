package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.core.curve.CompiledCurve;

public record CompiledSpread(
        double angleDegrees,
        CompiledCurve angleByShot,
        double sneakMultiplier,
        double sprintMultiplier
) {
    /** Compatibility constructor retained for definitions compiled before 0.51.1. */
    public CompiledSpread(double angleDegrees, CompiledCurve angleByShot) {
        this(angleDegrees, angleByShot, 1.0, 1.0);
    }

    public CompiledSpread(double angleDegrees) {
        this(angleDegrees, null, 1.0, 1.0);
    }

    public CompiledSpread {
        if (angleDegrees < 0.0 || angleDegrees > 89.0 || !Double.isFinite(angleDegrees)) {
            throw new IllegalArgumentException("angleDegrees must be finite and in [0, 89]");
        }
        if (!(sneakMultiplier >= 0.0) || !Double.isFinite(sneakMultiplier)) {
            throw new IllegalArgumentException("sneakMultiplier must be finite and >= 0");
        }
        if (!(sprintMultiplier >= 0.0) || !Double.isFinite(sprintMultiplier)) {
            throw new IllegalArgumentException("sprintMultiplier must be finite and >= 0");
        }
    }

    public double angleForShot(int shotIndex) {
        double angle = angleByShot == null ? angleDegrees : angleByShot.evaluate(Math.max(0, shotIndex));
        if (!Double.isFinite(angle)) return angleDegrees;
        return Math.max(0.0, Math.min(89.0, angle));
    }

    public double stanceMultiplier(boolean sneaking, boolean sprinting) {
        double result = 1.0;
        if (sneaking) result *= sneakMultiplier;
        if (sprinting) result *= sprintMultiplier;
        return Math.max(0.0, result);
    }
}
