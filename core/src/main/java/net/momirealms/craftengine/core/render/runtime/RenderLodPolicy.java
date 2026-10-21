package net.momirealms.craftengine.core.render.runtime;

/** Immutable distance-based visual update policy. */
public final class RenderLodPolicy {
    public static final RenderLodPolicy DEFAULT = new RenderLodPolicy(32f, 64f, 96f, 128f);

    private final float fullDistance;
    private final float halfDistance;
    private final float quarterDistance;
    private final float eighthDistance;

    public RenderLodPolicy(float fullDistance, float halfDistance, float quarterDistance, float eighthDistance) {
        requireFiniteNonNegative(fullDistance, "fullDistance");
        requireFiniteNonNegative(halfDistance, "halfDistance");
        requireFiniteNonNegative(quarterDistance, "quarterDistance");
        requireFiniteNonNegative(eighthDistance, "eighthDistance");
        if (fullDistance > halfDistance || halfDistance > quarterDistance || quarterDistance > eighthDistance) {
            throw new IllegalArgumentException("LOD distances must be ascending");
        }
        this.fullDistance = fullDistance;
        this.halfDistance = halfDistance;
        this.quarterDistance = quarterDistance;
        this.eighthDistance = eighthDistance;
    }

    public RenderLodLevel select(boolean hasViewers, float nearestViewerDistance) {
        if (!hasViewers) return RenderLodLevel.CULLED;
        if (!Float.isFinite(nearestViewerDistance) || nearestViewerDistance < 0f) {
            throw new IllegalArgumentException("nearestViewerDistance must be finite and >= 0");
        }
        if (nearestViewerDistance <= fullDistance) return RenderLodLevel.FULL;
        if (nearestViewerDistance <= halfDistance) return RenderLodLevel.HALF;
        if (nearestViewerDistance <= quarterDistance) return RenderLodLevel.QUARTER;
        if (nearestViewerDistance <= eighthDistance) return RenderLodLevel.EIGHTH;
        return RenderLodLevel.CULLED;
    }

    private static void requireFiniteNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0f) throw new IllegalArgumentException(name + " must be finite and >= 0");
    }
}
