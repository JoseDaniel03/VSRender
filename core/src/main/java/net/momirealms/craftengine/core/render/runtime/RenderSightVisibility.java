package net.momirealms.craftengine.core.render.runtime;

/** Pure view-cone/range culling math equivalent to BetterModel EntityUtil.canSee. */
public final class RenderSightVisibility {
    private static final double Y_RENDER_THRESHOLD = Math.toRadians(45.0);
    private static final double X_RENDER_THRESHOLD = Y_RENDER_THRESHOLD * 1.78;
    private static final double TAU = Math.PI * 2.0;

    private RenderSightVisibility() {}

    public static boolean canSee(boolean sameWorld,
                                 double viewerX, double viewerY, double viewerZ,
                                 float viewerYawDegrees, float viewerPitchDegrees,
                                 double targetX, double targetY, double targetZ,
                                 double minSight, double maxSight) {
        if (!sameWorld) return false;
        double dx = targetX - viewerX;
        double dy = targetY - viewerY;
        double dz = targetZ - viewerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > maxSight) return false;
        if (distance <= minSight) return true;
        double t = Math.PI - 2.0 * Math.abs(Math.atan(distance));
        double ty = t + Y_RENDER_THRESHOLD;
        double tz = t + X_RENDER_THRESHOLD;
        double playerYaw = Math.toRadians(viewerYawDegrees);
        double playerPitch = -Math.toRadians(viewerPitchDegrees);
        double ry = Math.abs(Math.atan2(dy, Math.sqrt(dz * dz + dx * dx)) - playerPitch);
        double rz = Math.abs(Math.atan2(-dx, dz) - playerYaw);
        return inThreshold(ry, ty) && inThreshold(rz, tz);
    }

    private static boolean inThreshold(double radians, double threshold) {
        return radians <= threshold || radians >= TAU - threshold;
    }
}
