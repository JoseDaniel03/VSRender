package net.momirealms.craftengine.core.render.rig;

/** Pure visibility math matching BetterModel EntityUtil.isCustomNameVisible/isInPoint. */
public final class RenderNameTagVisibility {
    private static final double MAX_DISTANCE_SQ = 25.0;
    private static final double POINT_THRESHOLD = Math.toRadians(10.0);
    private static final double TAU = Math.PI * 2.0;

    private RenderNameTagVisibility() {}

    public static boolean customNameVisible(boolean sameWorld,
                                            double viewerX, double viewerY, double viewerZ,
                                            float viewerYawDegrees, float viewerPitchDegrees,
                                            double targetX, double targetY, double targetZ) {
        if (!sameWorld) return false;
        double dx = targetX - viewerX;
        double dy = targetY - viewerY;
        double dz = targetZ - viewerZ;
        if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQ) return false;
        double playerYaw = Math.toRadians(viewerYawDegrees);
        double playerPitch = -Math.toRadians(viewerPitchDegrees);
        double ry = Math.abs(Math.atan2(dy, Math.sqrt(dz * dz + dx * dx)) - playerPitch);
        double rz = Math.abs(Math.atan2(-dx, dz) - playerYaw);
        return inThreshold(ry) && inThreshold(rz);
    }

    private static boolean inThreshold(double radians) {
        return radians <= POINT_THRESHOLD || radians >= TAU - POINT_THRESHOLD;
    }
}
