package com.virgolandstudio.items.core.cinematic;

/** Immutable camera pose independent from any frontend/world implementation. */
public record CinematicPose(double x, double y, double z, float yaw, float pitch) {
    public CinematicPose lerp(CinematicPose target, double alpha) {
        double t = Math.max(0.0, Math.min(1.0, alpha));
        return new CinematicPose(
                x + (target.x - x) * t,
                y + (target.y - y) * t,
                z + (target.z - z) * t,
                lerpAngle(yaw, target.yaw, t),
                (float) (pitch + (target.pitch - pitch) * t)
        );
    }

    /** True when a MOVE would only produce an imperceptible correction. */
    public boolean nearlyEquals(CinematicPose other, double positionEpsilon, float angleEpsilon) {
        if (other == null) return false;
        double dx = other.x - x;
        double dy = other.y - y;
        double dz = other.z - z;
        if (dx * dx + dy * dy + dz * dz > positionEpsilon * positionEpsilon) return false;
        return Math.abs(shortestAngleDelta(yaw, other.yaw)) <= angleEpsilon
                && Math.abs(other.pitch - pitch) <= angleEpsilon;
    }

    static float lerpAngle(float from, float to, double alpha) {
        return (float) (from + shortestAngleDelta(from, to) * alpha);
    }

    static float shortestAngleDelta(float from, float to) {
        return ((to - from + 540.0f) % 360.0f) - 180.0f;
    }

    static float normalizeYaw(double yaw) {
        double normalized = (yaw + 180.0) % 360.0;
        if (normalized < 0.0) normalized += 360.0;
        return (float) (normalized - 180.0);
    }
}
